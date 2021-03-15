package de.evoila.cf.backup.clients;

import io.minio.errors.*;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public interface FileClient {

    static String concatIdentifier(String identifier, String extension) {
        Assert.notNull(identifier, "Identifier may not be null");
        Assert.notNull(extension, "Extension may not be undefined");

        return identifier + "." + extension;
    }

    String upload(File file, String bucket, String identifier, String extension) throws IOException, ServerException, InsufficientDataException, InternalException, InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, XmlParserException, ErrorResponseException;

    URL generateUrl(String bucket, String identifier, String extension) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException;

    File download(String bucket, String identifier, String extension, String path)
            throws IOException, IllegalArgumentException, ServerException, InsufficientDataException, InvalidKeyException, InvalidResponseException, NoSuchAlgorithmException, InternalException, XmlParserException, ErrorResponseException;

    void delete(String bucket, String identifier, String extension) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, ErrorResponseException;
}
