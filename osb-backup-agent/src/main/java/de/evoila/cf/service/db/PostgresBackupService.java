package de.evoila.cf.service.db;

import de.evoila.cf.model.BackupJob;
import de.evoila.cf.model.EndpointCredential;
import de.evoila.cf.model.FileDestination;
import de.evoila.cf.model.enums.DatabaseType;
import de.evoila.cf.openstack.OSException;
import de.evoila.cf.service.SwiftBackupService;
import de.evoila.cf.service.exception.ProcessException;
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
public class PostgresBackupService extends SwiftBackupService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public DatabaseType getSourceType() {
        return DatabaseType.POSTGRESQL;
    }

    @Override
    public Map<String, String> backup(EndpointCredential source, FileDestination dest, BackupJob job) throws IOException, InterruptedException,
            OSException, ProcessException {
        long s_time = System.currentTimeMillis();

        Map<String, String> backupFiles = new HashMap<>();
        for (String database : source.getItems()) {


            log.info(String.format("Starting restore (%s)", job.getId()));

            File backup = new File(String.format("%s_%s.pg", database, format.format(new Date())));

            ProcessBuilder process = new ProcessBuilder(this.getClass().getResource("/pg_dump").getPath(),
                                                             "-Fc", // enable custom export format for big databases
                                                             String.format("--host=%s", source.getHostname()),
                                                             String.format("--port=%d", source.getPort()),
                                                             String.format("--username=%s", source.getUsername()),
                                                             String.format("%s", database)
            ).redirectOutput(backup);
            process.environment().put("PGPASSWORD", source.getPassword());
            runProcess(process, job);

            log.info(String.format("Backup (%s) from %s:%d/%s took %fs (File size %f)",
                                   job.getId(),
                                   source.getHostname(),
                                   source.getPort(),
                                   database,
                                   ((System.currentTimeMillis() - s_time) / 1000.0),
                                   (backup.length() / 1048576.0)
            ));
            s_time = System.currentTimeMillis();

            String filePath = upload(backup, source, dest, job);
            backup.delete();
            backupFiles.put(database, filePath);
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

            long s_time = System.currentTimeMillis();
            ProcessBuilder process = new ProcessBuilder(this.getClass().getResource("/pg_restore").getPath(),
                    String.format("--host=%s", destination.getHostname()),
                    String.format("--port=%d", destination.getPort()),
                    String.format("--username=%s", destination.getUsername()),
                    String.format("--dbname=%s", filename.getKey()),
                    "--schema=public",
                    String.format("%s", backup.getAbsolutePath()
                    )
            ).redirectError(new File("pg.err.log")).redirectOutput(new File("pg.test.log"));
            process.environment().put("PGPASSWORD", destination.getPassword());
            runProcess(process, job);

            log.info(String.format("Restore (%s) of File %s/%s took %f s (TO %f)",
                    job.getId(),
                    source.getContainerName(),
                    filename.getKey(),
                    ((System.currentTimeMillis() - s_time) / 1000.0),
                    (backup.length() / 1048576.0)
            ));
            backup.getParentFile().delete();
        }
    }
}
