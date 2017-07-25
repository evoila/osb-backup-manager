package de.evoila.cf.service;

import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.DatabaseCredential;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.service.exception.ProcessException;
import de.evoila.cf.service.extension.TarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by yremmet on 06.07.17.
 */

@Service
public class MongoDbBackupService extends SwiftBackupService implements TarFile {
  private final Logger log = LoggerFactory.getLogger(getClass());


  @Override
  public DatabaseType getSourceType () {
    return DatabaseType.MongoDB;
  }


  @Override
  public String backup (DatabaseCredential source, FileDestination destination, BackupJob job) throws IOException, InterruptedException, OSException, ProcessException {

    long s_time = System.currentTimeMillis();

    log.info(String.format("Starting backup process to %s:%d/%s",
                           source.getHostname(),
                           source.getPort(),
                           source.getContext()
    ));
    File backup = new File(String.format("%s_%s",source.getContext(), format.format(new Date())));

    while (!backup.mkdirs()) Thread.sleep(1000);
    ProcessBuilder process = new ProcessBuilder(this.getClass().getResource("/mongodump").getPath(),
                                         String.format("--host=%s:%d", source.getHostname(), source.getPort()),
                                         String.format("--username=%s", source.getUsername()),
                                         String.format("--password=%s", source.getPassword()),
                                         String.format("--authenticationDatabase=%s", source.getContext()),
                                         String.format("--db=%s", source.getContext()),
                                         String.format("--out=%s", backup.getAbsolutePath())
    ).redirectError(new File("./mongodb.err.log")).redirectOutput(new File("./mongodb.log"));
    runProcess(process);

    backup = tarGz(backup, true);
    String filePath = upload(backup,source,destination,job);
    backup.delete();
    return filePath;
  }

  @Override
  public void restore (DatabaseCredential destination, FileDestination source, BackupJob job) throws IOException, OSException, InterruptedException, ProcessException {

    log.info(String.format("Starting restore process to %s:%d/%s",
                           destination.getHostname(),
                           destination.getPort(),
                           destination.getContext()
    ));

    File backup = super.download(source, job);
    backup = unTarGz(backup, true).get(1);
    long s_time = System.currentTimeMillis();


    ProcessBuilder process = new ProcessBuilder(this.getClass().getResource("/mongorestore").getPath(),
                                         String.format("--host=%s:%d", destination.getHostname(), destination.getPort()),
                                         String.format("--username=%s", destination.getUsername()),
                                         String.format("--password=%s", destination.getPassword()),
                                         String.format("--authenticationDatabase=%s", destination.getContext()),
                                         String.format("--db=%s", destination.getContext()),
                                         String.format("%s", backup.getAbsolutePath()))
                          ;
    runProcess(process);
    log.info(String.format("Restore (%s) of File %s/%s took %f s (TO %f)",
                           job.getId(),
                           source.getContainerName(),
                           source.getFilename(),
                           ((System.currentTimeMillis() - s_time) / 1000.0),
                           (backup.length() / 1048576.0)
    ));
    backup.getParentFile().delete();
  }
}
