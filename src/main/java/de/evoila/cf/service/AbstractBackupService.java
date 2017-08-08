package de.evoila.cf.service;

import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.service.extension.ProcessRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yremmet on 06.07.17.
 */
public abstract class AbstractBackupService implements BackupService, ProcessRunner {
  private final Logger log = LoggerFactory.getLogger(getClass());
  protected static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
  protected List<DestinationType> destinationTypes = new ArrayList<DestinationType>();
  private BackupServiceManager serviceManager;

  public List<DestinationType> getDestinationTypes () {
    return this.destinationTypes;
  }

  public BackupServiceManager getServiceManager () {
    return serviceManager;
  }

  @Autowired
  public void setServiceManager (BackupServiceManager serviceManager) {
    this.serviceManager = serviceManager;
    this.serviceManager.addBackupServiceManager(this);
  }

  protected String getBinary (String path) throws BackupException {
    URL toolUrl = this.getClass().getResource(path);
    if(toolUrl == null)
      throw new BackupException("Could not load "+ path);
    return toolUrl.getPath();
  }

}
