package de.evoila.cf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.evoila.cf.model.enums.DestinationType;
import de.evoila.cf.model.enums.JobStatus;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by yremmet on 06.07.17.
 */
public class BackupJob {

    public static final String BACKUP_JOB = "Backup Job";

    public static final String RESTORE_JOB = "Restore Job";

    private String id;

    private Date startDate;

    private String instanceId;

    private JobStatus status;

    private String jobType;

    private FileDestination destination;

    private List<String> logs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instance) {
        this.instanceId = instance;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public List<String> getLogs() {
        if (logs == null) {
            logs = new LinkedList();
        }
        return logs;
    }

    public void setLogs(List<String> logs) {
        this.logs = logs;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public FileDestination getDestination() {
        return destination;
    }

    public void setFileDestination(FileDestination fileDestination) {
        this.destination = fileDestination;
    }

    public synchronized void appendLog(String msg) {
        this.getLogs().add(msg);
    }

}
