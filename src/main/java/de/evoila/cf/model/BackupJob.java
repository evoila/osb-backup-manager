package de.evoila.cf.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.evoila.cf.model.enums.DestinationType;

import java.util.Date;

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
  private String backupPath;
  private BackupDestination destination;

  public String getId () {
    return id;
  }

  public void setId (String id) {
    this.id = id;
  }

  public Date getStartDate () {
    return startDate;
  }

  public String getInstanceId () {
    return instanceId;
  }

  public void setInstanceId (String instance) {
    this.instanceId = instance;
  }

  public JobStatus getStatus () {
    return status;
  }

  public void setStatus (JobStatus status) {
    this.status = status;
  }

  public String getJobType () {
    return jobType;
  }

  public void setJobType (String jobType) {
    this.jobType = jobType;
  }

  public void setStartDate (Date startDate) {
    this.startDate = startDate;
  }


  @JsonInclude(JsonInclude.Include.NON_NULL)
  public BackupDestination getDestination () {
    return destination;
  }

  public void setBackupFile (FileDestination destination) {
    this.destination = new BackupDestination(destination);
  }
  public void setDestination (BackupDestination destination) {
    this.destination = destination;
  }




  public static class BackupDestination {
    private String type;
    private String project;
    private String container;
    private String filename;
    public BackupDestination(){
      super();
    }

    public void setType (String type) {
      this.type = type;
    }

    public void setType (DestinationType type) {
      this.type = type.toString();
    }


    public void setProject (String project) {
      this.project = project;
    }

    public void setContainer (String container) {
      this.container = container;
    }

    public void setFilename (String filename) {
      this.filename = filename;
    }

    private BackupDestination (FileDestination destination) {
      setType(destination.getType());
      setProject(destination.getProjectName());
      setContainer(destination.getContainerName());
      setFilename(destination.getFilename());
    }

    public String getType () {
      return type;
    }

    public String getProject () {
      return project;
    }

    public String getContainer () {
      return container;
    }

    public String getFilename () {
      return filename;
    }
  }
}
