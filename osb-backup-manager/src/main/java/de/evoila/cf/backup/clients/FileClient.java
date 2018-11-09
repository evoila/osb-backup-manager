package de.evoila.cf.backup.clients;

import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public interface FileClient {

    static String concatIdentifier(String identifier, String extension) {
        Assert.notNull(identifier, "Identifier may not be null");
        Assert.notNull(extension, "Extension may not be undefined");

        return identifier + "." + extension;
    }

    String upload(File file, String bucket, String identifier, String extension) throws MalformedURLException;

    URL generateUrl(String bucket, String identifier, String extension) throws MalformedURLException;

    File download(String bucket, String identifier, String extension, String path)
            throws IOException, IllegalArgumentException;

    void delete(String bucket, String identifier, String extension);
}
