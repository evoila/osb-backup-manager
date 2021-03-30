/**
 *
 */
package de.evoila.cf.backup.clients;

import de.evoila.cf.backup.controller.exception.BackupException;
import de.evoila.cf.backup.repository.FileDestinationRepository;
import de.evoila.cf.model.api.file.S3FileDestination;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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
public class S3Client implements FileClient {

    private static final Logger log = LoggerFactory.getLogger(S3Client.class);

    private static final String s3ValidationFileName = "s3_validation_testfile";
    private static final String s3ValidationFileExtension = "txt";

    private MinioClient client;

    private FileDestinationRepository destinationRepository;

    public S3Client(String endpoint, String region, String authKey, String authSecret,
                    FileDestinationRepository destinationRepository) {

        this.destinationRepository = destinationRepository;

        if(region.isEmpty()) {
            client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(authKey, authSecret)
                    .build();
        } else {
            client = MinioClient.builder()
                    .endpoint(endpoint)
                    .region(region)
                    .credentials(authKey, authSecret)
                    .build();
        }
    }

    /**
     * Checks whether the created Client is able to write data to the specified endpoint & bucket
     * @param destination
     * @throws URISyntaxException
     */
    public void validate(S3FileDestination destination) throws URISyntaxException, IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, ErrorResponseException, XmlParserException, InternalException, BackupException {
        String bucket = destination.getBucket();
        String url = generateUrl(bucket, s3ValidationFileName, s3ValidationFileExtension).toString();

        log.info("Starting validation for " + url.substring(0, url.lastIndexOf("/")));

        if(destinationRepository.findByNameAndServiceInstanceId(destination.getName(), destination.getServiceInstance().getId()) != null) {
            throw new BackupException("Endpoint with name " + destination.getName() + " already exists");
        }

        URL resource = getClass().getClassLoader().getResource(FileClient.concatIdentifier(s3ValidationFileName, s3ValidationFileExtension));
        File file = new File(resource.toURI());

        upload(file, bucket, s3ValidationFileName, s3ValidationFileExtension);

        delete(bucket, file.getName());
    }

    public String upload(File file, String bucket, String identifier, String extension) throws IOException, ServerException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException {
        Assert.notNull(bucket, "Bucket may not be undefined");

        client.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucket)
                        .object(file.getName())
                        .filename(file.getAbsolutePath())
                        .build());

        URL url = generateUrl(bucket, identifier, extension);

        log.info("Uploaded file to: " + url.toString());

        return url.toString();
    }

    public URL generateUrl(String bucket, String identifier, String extension) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {
        String filename = FileClient.concatIdentifier(identifier, extension);

        //This is sadly the only way to get any url from the minio client
        String url = client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(filename)
                .build());

        return new URL(url.split("\\?")[0]);
    }

    public File download(String bucket, String identifier, String extension, String path) throws IOException, ServerException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException {
        Assert.notNull(client, "S3 Connection may not be null");
        Assert.notNull(bucket, "Bucket may not be undefined");

        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))
            throw new IllegalArgumentException("Bucket does not exists -> " + bucket);

        String filename = FileClient.concatIdentifier(identifier, extension);

        InputStream inputStream = client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(filename)
                .build());

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

    public void delete(String bucket, String identifier, String extension) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {

        String filename = FileClient.concatIdentifier(identifier, extension);

        delete(bucket, filename);
    }

    public void delete(String bucket, String filename) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException {

        client.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(filename)
                .build());

        log.info("File deleted: " + bucket + "/" + filename);
    }

}
