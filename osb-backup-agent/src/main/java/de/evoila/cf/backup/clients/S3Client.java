/**
 *
 */
package de.evoila.cf.backup.clients;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AbstractAmazonS3;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.net.URL;

/**
 * @author Johannes Hiemer.
 * Create a public bucket:
 * {
 * "Version": "2008-10-17",
 * "Id": "Policy1410846366931",
 * "Statement": [
 * {
 * "Sid": "Stmt1410846362554",
 * "Effect": "Allow",
 * "Principal": {
 * "AWS": "*"
 * },
 * "Action": [
 * "s3:DeleteObject",
 * "s3:GetObject",
 * "s3:PutObject"
 * ],
 * "Resource": "arn:aws:s3:::whibs/*"
 * }
 * ]
 * }
 */
public class S3Client extends AbstractAmazonS3 implements FileClient {

    private static final Logger log = LoggerFactory.getLogger(S3Client.class);

    private AmazonS3 client;

    public S3Client(String region, String awsId, String awsKey) {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(awsId, awsKey);

        this.client = AmazonS3ClientBuilder.standard()
                .withRegion(Regions.fromName(region))
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    @Override
    public String upload(File file, String bucket, String identifier, String extension) {
        Assert.notNull(bucket, "Bucket may not be undefined");

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.addUserMetadata("identifier", identifier);
        objectMetadata.addUserMetadata("extension", extension);

        String filename = FileClient.concatIdentifier(identifier, extension);

        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, filename, file);
        putObjectRequest.setMetadata(objectMetadata);
        PutObjectResult putObjectResult = client.putObject(putObjectRequest);

        URL url = client.getUrl(bucket, filename);

        log.info("Uploaded file to: " + url.toString());

        return url.toString();
    }

    @Override
    public URL generateUrl(String bucket, String identifier, String extension) {
        String filename = FileClient.concatIdentifier(identifier, extension);

        return client.getUrl(bucket, filename);
    }

    @Override
    public File download(String bucket, String identifier, String extension, String path)
            throws IOException, IllegalArgumentException {
        Assert.notNull(client, "S3 Connection may not be null");
        Assert.notNull(bucket, "Bucket may not be undefined");

        if (!client.doesBucketExistV2(bucket))
            throw new IllegalArgumentException("Bucket does not exists -> " + bucket);

        String filename = FileClient.concatIdentifier(identifier, extension);

        if (!client.doesObjectExist(bucket, filename))
            throw new IllegalArgumentException("File does not exists -> " + bucket + " -> " + filename);

        S3Object s3Object = client.getObject(new GetObjectRequest(bucket, filename));
        InputStream inputStream = new BufferedInputStream(s3Object.getObjectContent());

        File downloadedFile = new File(path + File.separator + filename);

        log.info("Opening file save to: " + downloadedFile.getAbsolutePath());
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedFile));

        int read;
        while ((read = inputStream.read()) != -1)
            outputStream.write(read);

        outputStream.flush();
        outputStream.close();
        inputStream.close();

        log.info("Saved file to: " + downloadedFile.getAbsolutePath());

        return downloadedFile;
    }

    @Override
    public void delete(String bucket, String identifier, String extension) {

        String filename = FileClient.concatIdentifier(identifier, extension);

        client.deleteObject(bucket, filename);

        log.info("File deleted: " + bucket + "/" + filename);
    }

    public void delete(String bucket, String filename) {
        client.deleteObject(bucket, filename);

        log.info("File deleted: " + bucket + "/" + filename);
    }

}
