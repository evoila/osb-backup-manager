package de.evoila.cf.service;

import de.evoila.cf.model.*;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.openstack.SwiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by yremmet on 10.07.17.
 */
public abstract class SwiftBackupService extends AbstractBackupService{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @PostConstruct
    private void postConstruct () {
        destinationTypes.add(DestinationType.Swift);
    }

    protected String upload(File backup, DatabaseCredential source, FileDestination destination, BackupJob job) throws IOException, OSException {
        long s_time = System.currentTimeMillis();

        String msg = String.format("Uploading Backup (%s)", job.getId());
        log.info(msg);
        job.appendLog(msg);

        SwiftClient client = new SwiftClient(destination.getAuthUrl(),
                                             destination.getUsername(),
                                             destination.getPassword(),
                                             destination.getDomain(),
                                             destination.getProjectName()
        );
        String backupName = String.format(backup.getName(), format.format(new Date()));
        String filePath = client.upload(destination.getContainerName(), backupName, backup);

        msg = String.format("Uploading the Backup (%s) from %s:%d/%s took %fs (File size %f)",
                            job.getId(),
                            source.getHostname(),
                            source.getPort(),
                            source.getContext(),
                            ((System.currentTimeMillis() - s_time) / 1000.0),
                            (backup.length() / 1048576.0)
        );
        log.info(msg);
        job.appendLog(msg);

        return backupName;
    }

    public File download (FileDestination source, BackupJob job) throws IOException, OSException {
        long s_time = System.currentTimeMillis();
        String msg = String.format("Downloading Backup (%s)", job.getId());
        log.info(msg);
        job.appendLog(msg);
        SwiftClient client = new SwiftClient(source.getAuthUrl(),
                                             source.getUsername(),
                                             source.getPassword(),
                                             source.getDomain(),
                                             source.getProjectName()
        );

        File backup = client.download(source.getContainerName(), source.getFilename());
        msg = String.format("Download (%s) of File %s/%s took %f s (File size %f)",
                               job.getId(),
                               source.getContainerName(),
                               source.getFilename(),
                               ((System.currentTimeMillis() - s_time) / 1000.0),
                               (backup.length() / 1048576.0)
        );
        log.info(msg);
        job.appendLog(msg);

        return backup;
    }
}
