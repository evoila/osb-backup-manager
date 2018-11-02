package de.evoila.cf.model;

/**
 * @author Yannic Remmet, Johannes Hiemer
 */
public class BackupRequest {

    private BackupPlan plan;

    public BackupPlan getPlan() {
        return plan;
    }

    public void setPlan(BackupPlan plan) {
        this.plan = plan;
    }
}
