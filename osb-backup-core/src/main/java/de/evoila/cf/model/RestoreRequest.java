package de.evoila.cf.model;

/**
 * Created by yremmet on 28.06.17.
 */
public class RestoreRequest {
  private FileDestination source;
  private EndpointCredential destination;

  public FileDestination getSource () {
    return source;
  }

  public void setSource (FileDestination source) {
    this.source = source;
  }

  public void setDestination (EndpointCredential destination) {
    this.destination = destination;
  }


  public EndpointCredential getDestination () {
    return destination;
  }
}
