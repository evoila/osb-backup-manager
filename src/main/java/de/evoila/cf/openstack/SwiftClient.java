package de.evoila.cf.openstack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.evoila.cf.model.BackupJob;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.*;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Created by yremmet on 04.07.17.
 */
public class SwiftClient {
  Logger logger = LoggerFactory.getLogger(getClass());
  private String token;
  private String url;

  public SwiftClient (String authUrl, String username, String password, String domain, String project) throws IOException, OSException {
    logger.info("Authentication for Swift Access USER=" + username + " URL=" + authUrl);
    HttpResponse response = Request.Post(authUrl + "/auth/tokens")
                                .addHeader("Content-Type", "application/json")
                                .bodyString(this.generateAuthObject(domain, project, username, password),
                                            ContentType.APPLICATION_JSON
                                )
                                .execute().returnResponse();

    if (response.getStatusLine().getStatusCode() != 201) {
      logger.error("could not authenticate Swift USER=" + username + " URL=" + authUrl + " "+ response.getStatusLine().getReasonPhrase());
      throw new OSException("could not authenticate Swift USER=" + username + " URL=" + authUrl + " "+ response.getStatusLine().getReasonPhrase());
    }
    getToken(response);
    getSwiftUrl(response);
    if (this.url == null) {
      logger.error("could not find Swift URL AUTH-URL=" + authUrl + " "+ response.getStatusLine().getReasonPhrase());
      throw new OSException("could not find Swift URL AUTH-URL=" + authUrl + " "+ response.getStatusLine().getReasonPhrase());
    }


  }

    /*
    public List<de.evoila.cf.openstack.Container> getContainers() throws IOException {
        Content content = Request.Get(this.url+url)
                .addHeader("X-Auth-Token", token)
                .addHeader("Accept", "application/json")
                .execute().returnContent();
        ObjectMapper mapper = new ObjectMapper();
        mapper.readValue(content.asStream(), Configuration.class);
        ret
    }*/

  public String upload (String containerName, String objectName, File file) throws IOException {
    String path = this.url + "/" + containerName + "/" + objectName;
    Content content = Request.Put(path)
                          .addHeader("X-Auth-Token", token)
                          .addHeader("Accept", "application/json")
                          .bodyFile(file, ContentType.DEFAULT_BINARY)
                          .execute().returnContent();
    return path;
  }

  public File download (String containerName, String objectName) throws IOException {
    Content content = Request.Get(this.url + "/" + containerName + "/" + objectName)
                          .addHeader("X-Auth-Token", token)
                          .addHeader("Accept", "application/json")
                          .execute().returnContent();
    File file = new File("/tmp/" + containerName);
    file.mkdirs();
    file = new File("/tmp/" + containerName + "/" + objectName);

    FileOutputStream fileOutputStream = new FileOutputStream(file);
    fileOutputStream.write(content.asBytes());
    fileOutputStream.flush();
    fileOutputStream.close();
    return file;
  }


  public void delete (String containerName, String objectname) throws IOException, OSException {
    String fileUrl= String.format("%s/%s/%s", this.url, containerName, objectname);
    logger .info(String.format("Deleting Swift File %s", fileUrl));
    HttpResponse response = Request.Delete(fileUrl)
                                  .addHeader("X-Auth-Token", token)
                                  .addHeader("Accept", "application/json")
                                  .execute().returnResponse();
    if(response.getStatusLine().getStatusCode() != 204){
      logger.error("Could not delete " + fileUrl);
      throw new OSException("Could not delete " + fileUrl);
    }

  }

  private void getSwiftUrl (HttpResponse response) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(response.getEntity().getContent());
    JsonNode catalog = actualObj.findValue("catalog");

    for (JsonNode catalogItem : catalog) {
      if (catalogItem.get("name").asText().equals("swift")) {
        this.url = catalogItem.get("endpoints").get(0).get("url").asText();
        break;
      }
    }
  }

  private void getToken (HttpResponse response) throws OSException {
    Header[] headers = response.getHeaders("X-Subject-Token");
    if (headers.length != 1) {
      throw new OSException("Could not aqurie Authentication Token");
    }
    token = headers[0].getValue();
  }


  private String generateAuthObject (String domainName, String projectName, String username, String password) {
    return String.format(
        "{\"auth\": {\"scope\": {\"project\": {\"name\": \"%s\",\"domain\": {\"name\": \"%s\"}}},\"identity\": {\"methods\": [\"password\"],\"password\": {\"user\": {\"name\": \"%s\",\"password\": \"%s\",\"domain\": {\"name\": \"%s\"}}}}}}",
        projectName,
        domainName,
        username,
        password,
        domainName
    );

  }

}

