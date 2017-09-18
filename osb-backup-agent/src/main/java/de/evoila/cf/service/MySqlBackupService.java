package de.evoila.cf.service;


import de.evoila.cf.controller.exception.BackupException;
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
 * Created by yremmet on 27.06.17.
 */
@Service
public class MySqlBackupService extends SwiftBackupService implements TarFile {
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    public DatabaseType getSourceType () {
        return DatabaseType.MySQL;
    }


    public String backup (DatabaseCredential source, FileDestination destination, BackupJob job) throws IOException, InterruptedException, OSException, ProcessException,BackupException {

        long s_time = System.currentTimeMillis();

        String msg = String.format("Starting backup (%s)", job.getId());
        log.info(msg);
        job.appendLog(msg);

        File backup = new File(String.format("%s/%s_%s.sql",
                                             source.getContext(),
                                             source.getContext(),
                                             format.format(new Date())
        ));
        backup.getParentFile().mkdirs();
        String tool = getBinary("/mysqldump");
        ProcessBuilder processBuilder = new ProcessBuilder(tool,
                                                           "-u" + source.getUsername(),
                                                           "-p" + source.getPassword(),
                                                           "-h" + source.getHostname(),
                                                           "-P" + Integer.toString(source.getPort()),
                                                           source.getContext()
        ).redirectOutput(backup);
        runProcess(processBuilder, job);

        backup = tarGz(backup, false);

        msg = String.format("Backup (%s) from %s:%d/%s took %fs (File size %f)",
                            job.getId(),
                            source.getHostname(),
                            source.getPort(),
                            source.getContext(),
                            ((System.currentTimeMillis() - s_time) / 1000.0),
                            (backup.length() / 1048576.0)
        );
        log.info(msg);
        job.appendLog(msg);

        String filePath = upload(backup, source,destination, job);
        backup.delete();
        return filePath;
    }




    public void restore (DatabaseCredential destination, FileDestination source, BackupJob job) throws IOException, OSException, InterruptedException, ProcessException, BackupException {


        log.info(String.format("Starting restore (%s) process to %s:%d/%s",
                               job.getId(),
                               destination.getHostname(),
                               destination.getPort(),
                               destination.getContext()
        ));

        File backup = super.download(source, job);
        backup = unTarGz(backup, true).get(0);


        long s_time = System.currentTimeMillis();
        ProcessBuilder processBuilder = new ProcessBuilder(getBinary("/mysql"),
                                                           "-u" + destination.getUsername(),
                                                           "-p" + destination.getPassword(),
                                                           "-h" + destination.getHostname(),
                                                           "-P" + Integer.toString(destination.getPort()),
                                                           destination.getContext()

        ).redirectInput(backup);
        runProcess(processBuilder, job);
        log.info(String.format("Restore (%s) of File %s/%s took %f s (TO %f)",
                               job.getId(),
                               source.getContainerName(),
                               source.getFilename(),
                               ((System.currentTimeMillis() - s_time) / 1000.0),
                               (backup.length() / 1048576.0)
        ));
        backup.delete();

    }
}
