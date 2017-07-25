package de.evoila.cf.model;

/**
 * Created by yremmet on 05.07.17.
 */
public interface SwiftConfig {
  public String getFilename ();
  public String getAuthUrl ();
  public String getUsername ();
  public String getPassword ();
  public String getDomain ();
  public String getProjectName ();
  public String getContainerName ();
}
