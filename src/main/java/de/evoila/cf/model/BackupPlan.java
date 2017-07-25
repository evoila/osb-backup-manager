package de.evoila.cf.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import de.evoila.cf.model.enums.RetentionStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yremmet on 18.07.17.
 */
public class BackupPlan{
    private String id;
    private DatabaseCredential source;
    private String frequency;
    private RetentionStyle retentionstyle;
    private int retentionPeriod;
    private FileDestination destination;
    private List<String> jobIds;

    public String getId () {
        return id;
    }

    public String getFrequency () {
        return frequency;
    }

    public void setFrequency (String frequency) {
        this.frequency = frequency;
    }

    public RetentionStyle getRetentionstyle () {
        return retentionstyle;
    }

    public void setRetentionstyle (String retentionstyle) {
        this.retentionstyle = RetentionStyle.valueOf(retentionstyle);
    }
    public void setRetentionstyle (RetentionStyle retentionstyle) {
        this.retentionstyle = retentionstyle;
    }

    public int getRetentionPeriod () {
        return retentionPeriod;
    }

    public void setRetentionPeriod (int retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public DatabaseCredential getSource () {
        return source;
    }

    public void setSource (DatabaseCredential source) {
        this.source = source;
    }

    public FileDestination getDestination () {
        return destination;
    }

    public void setDestination (FileDestination destination) {
        this.destination = destination;
    }

    @JsonIgnore
    public List<String> getJobIds () {
        if(jobIds == null)
            jobIds = new ArrayList<>();
        return jobIds;
    }

    public void setJobIds (List<String> jobIds) {
        this.jobIds = jobIds;
    }

    public void setId (String id) {
        this.id = id;
    }

    public void update (BackupPlan plan) {
        if(plan.frequency != null) {
            this.frequency = plan.frequency;
        }
        if(plan.retentionstyle != null){
            this.retentionstyle = plan.retentionstyle;
        }
        if(plan.destination != null){
            this.destination = plan.destination;
        }
        if(plan.getRetentionPeriod() > 0) {
            this.retentionPeriod = plan.retentionPeriod;
        }
    }
}
