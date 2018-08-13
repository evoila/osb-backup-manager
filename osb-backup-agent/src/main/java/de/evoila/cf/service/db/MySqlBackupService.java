package de.evoila.cf.service.db;


import de.evoila.cf.controller.exception.BackupException;
import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.BackupPlan;
import de.evoila.cf.model.EndpointCredential;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.service.SwiftBackupService;
import de.evoila.cf.service.exception.ProcessException;
import de.evoila.cf.service.extension.TarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by yremmet on 27.06.17.
 */
@Service
public class MySqlBackupService extends SwiftBackupService implements TarFile {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public DatabaseType getSourceType() {
        return DatabaseType.MYSQL;
    }


    public Map<String, String> backup(BackupPlan plan, FileDestination destination, BackupJob job) throws IOException,
            InterruptedException, OSException, ProcessException, BackupException {

        long s_time = System.currentTimeMillis();

        EndpointCredential endpointCredential = plan.getSource();
        Map<String, String> backupFiles = new HashMap<>();
        for (String database : plan.getItems()) {
            String msg = String.format("Starting backup (%s)", job.getId());
            log.info(msg);
            job.appendLog(msg);

            File backup = new File(String.format("%s/%s_%s.sql",
                    database,
                    database,
                    format.format(new Date())
            ));
            backup.getParentFile().mkdirs();
            String tool = getBinary("/mysqldump");
            ProcessBuilder processBuilder = new ProcessBuilder(tool,
                    "-u" + endpointCredential.getUsername(),
                    "-p" + endpointCredential.getPassword(),
                    "-h" + endpointCredential.getHostname(),
                    "-P" + Integer.toString(endpointCredential.getPort()),
                    database
            ).redirectOutput(backup);
            runProcess(processBuilder, job);

            backup = tarGz(backup, false);

            msg = String.format("Backup (%s) from %s:%d/%s took %fs (File size %f)",
                    job.getId(),
                    endpointCredential.getHostname(),
                    endpointCredential.getPort(),
                    database,
                    ((System.currentTimeMillis() - s_time) / 1000.0),
                    (backup.length() / 1048576.0)
            );
            log.info(msg);
            job.appendLog(msg);

            String filePath = upload(backup, endpointCredential, destination, job);
            backup.delete();
            backupFiles.put(database, filePath);
        }
        return backupFiles;
    }


    public void restore(EndpointCredential destination, FileDestination source, BackupJob job) throws IOException, OSException,
            InterruptedException, ProcessException, BackupException {

        for (Map.Entry<String, String> filename : source.getFilenames().entrySet()) {
            log.info(String.format("Starting restore (%s) process to %s:%d/%s",
                    job.getId(),
                    destination.getHostname(),
                    destination.getPort(),
                    filename.getKey()
            ));

            File backup = super.download(source, filename.getValue(), job);
            backup = unTarGz(backup, true).get(0);


            long s_time = System.currentTimeMillis();
            ProcessBuilder processBuilder = new ProcessBuilder(getBinary("/mysql"),
                    "-u" + destination.getUsername(),
                    "-p" + destination.getPassword(),
                    "-h" + destination.getHostname(),
                    "-P" + Integer.toString(destination.getPort()),
                    filename.getKey()

            ).redirectInput(backup);
            runProcess(processBuilder, job);
            log.info(String.format("Restore (%s) of File %s/%s took %f s (TO %f)",
                    job.getId(),
                    source.getContainerName(),
                    filename.getValue(),
                    ((System.currentTimeMillis() - s_time) / 1000.0),
                    (backup.length() / 1048576.0)
            ));
            backup.delete();

        }
    }
}
