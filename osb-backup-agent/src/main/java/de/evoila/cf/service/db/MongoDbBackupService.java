package de.evoila.cf.service.db;


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
 * Created by yremmet on 06.07.17.
 */

@Service
public class MongoDbBackupService extends SwiftBackupService implements TarFile {
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Override
    public DatabaseType getSourceType() {
        return DatabaseType.MONGODB;
    }


    @Override
    public Map<String, String> backup(BackupPlan plan, FileDestination destination, BackupJob job) throws IOException, InterruptedException,
            OSException, ProcessException {

        long s_time = System.currentTimeMillis();

        EndpointCredential endpointCredential = plan.getSource();
        Map<String, String> backupFiles = new HashMap<>();
        for (String collection : plan.getItems()) {
            log.info(String.format("Starting backup process to %s:%d/%s",
                    endpointCredential.getHostname(),
                    endpointCredential.getPort(),
                    collection
            ));
            File backup = new File(String.format("%s_%s", collection, format.format(new Date())));

            while (!backup.mkdirs()) Thread.sleep(1000);
            ProcessBuilder process = new ProcessBuilder(this.getClass().getResource("/mongodump").getPath(),
                    String.format("--host=%s:%d", endpointCredential.getHostname(), endpointCredential.getPort()),
                    String.format("--username=%s", endpointCredential.getUsername()),
                    String.format("--password=%s", endpointCredential.getPassword()),
                    String.format("--authenticationDatabase=%s", collection),
                    String.format("--db=%s", collection),
                    String.format("--out=%s", backup.getAbsolutePath())
            ).redirectError(new File("./mongodb.err.log")).redirectOutput(new File("./mongodb.log"));
            runProcess(process, job);

            backup = tarGz(backup, true);
            String filePath = upload(backup, endpointCredential, destination, job);
            backup.delete();
            backupFiles.put(collection, filePath);
        }
        return backupFiles;
    }

    @Override
    public void restore(EndpointCredential destination, FileDestination source, BackupJob job) throws IOException, OSException,
            InterruptedException, ProcessException {

        for (Map.Entry<String, String> filename : source.getFilenames().entrySet()) {
            log.info(String.format("Starting restore process to %s:%d/%s",
                    destination.getHostname(),
                    destination.getPort(),
                    filename.getKey()
            ));

            File backup = super.download(source, filename.getValue(), job);
            backup = unTarGz(backup, true).get(1);
            long s_time = System.currentTimeMillis();


            ProcessBuilder process = new ProcessBuilder(this.getClass().getResource("/mongorestore").getPath(),
                    String.format("--host=%s:%d", destination.getHostname(), destination.getPort()),
                    String.format("--username=%s", destination.getUsername()),
                    String.format("--password=%s", destination.getPassword()),
                    String.format("--authenticationDatabase=%s", filename.getKey()),
                    String.format("--db=%s", filename.getKey()),
                    String.format("%s", backup.getAbsolutePath()));
            runProcess(process, job);
            log.info(String.format("Restore (%s) of File %s/%s took %f s (TO %f)",
                    job.getId(),
                    source.getContainerName(),
                    filename.getValue(),
                    ((System.currentTimeMillis() - s_time) / 1000.0),
                    (backup.length() / 1048576.0)
            ));
            backup.getParentFile().delete();
        }
    }
}
