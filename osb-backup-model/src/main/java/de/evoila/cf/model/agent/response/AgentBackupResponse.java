package de.evoila.cf.model.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Johannes Hiemer.
 */
public class AgentBackupResponse extends AgentExecutionResponse {

    private String filenamePrefix;

    private String filename;

    private Filesize filesize;

    @JsonProperty("pre_backup_lock_log")
    private String preBackupLockLog;

    @JsonProperty("pre_backup_lock_errorlog")
    private String preBackupLockErrorLog;

    @JsonProperty("pre_backup_check_log")
    private String preBackCheckLog;

    @JsonProperty("pre_backup_check_errorlog")
    private String preBackCheckErrorLog;

    @JsonProperty("backup_log")
    private String backupLog;

    @JsonProperty("backup_errorlog")
    private String backupErrorLog;

    @JsonProperty("backup_cleanup_log")
    private String backupCleanupLog;

    @JsonProperty("backup_cleanup_errorlog")
    private String backupCleanupErrorLog;

    @JsonProperty("post_backup_unlock_log")
    private String postBackupUnlockLog;

    @JsonProperty("post_backup_unlock_errorlog")
    private String postBackupUnlockErrorLog;

    public class Filesize {

        private long size;

        private String unit;

        public Filesize() {}

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }

    public String getFilenamePrefix() {
        return filenamePrefix;
    }

    public void setFilenamePrefix(String filenamePrefix) {
        this.filenamePrefix = filenamePrefix;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Filesize getFilesize() {
        return filesize;
    }

    public void setFilesize(Filesize filesize) {
        this.filesize = filesize;
    }

    public String getPreBackupLockLog() {
        return preBackupLockLog;
    }

    public void setPreBackupLockLog(String preBackupLockLog) {
        this.preBackupLockLog = preBackupLockLog;
    }

    public String getPreBackupLockErrorLog() {
        return preBackupLockErrorLog;
    }

    public void setPreBackupLockErrorLog(String preBackupLockErrorLog) {
        this.preBackupLockErrorLog = preBackupLockErrorLog;
    }

    public String getPreBackCheckLog() {
        return preBackCheckLog;
    }

    public void setPreBackCheckLog(String preBackCheckLog) {
        this.preBackCheckLog = preBackCheckLog;
    }

    public String getPreBackCheckErrorLog() {
        return preBackCheckErrorLog;
    }

    public void setPreBackCheckErrorLog(String preBackCheckErrorLog) {
        this.preBackCheckErrorLog = preBackCheckErrorLog;
    }

    public String getBackupLog() {
        return backupLog;
    }

    public void setBackupLog(String backupLog) {
        this.backupLog = backupLog;
    }

    public String getBackupErrorLog() {
        return backupErrorLog;
    }

    public void setBackupErrorLog(String backupErrorLog) {
        this.backupErrorLog = backupErrorLog;
    }

    public String getBackupCleanupLog() {
        return backupCleanupLog;
    }

    public void setBackupCleanupLog(String backupCleanupLog) {
        this.backupCleanupLog = backupCleanupLog;
    }

    public String getBackupCleanupErrorLog() {
        return backupCleanupErrorLog;
    }

    public void setBackupCleanupErrorLog(String backupCleanupErrorLog) {
        this.backupCleanupErrorLog = backupCleanupErrorLog;
    }

    public String getPostBackupUnlockLog() {
        return postBackupUnlockLog;
    }

    public void setPostBackupUnlockLog(String postBackupUnlockLog) {
        this.postBackupUnlockLog = postBackupUnlockLog;
    }

    public String getPostBackupUnlockErrorLog() {
        return postBackupUnlockErrorLog;
    }

    public void setPostBackupUnlockErrorLog(String postBackupUnlockErrorLog) {
        this.postBackupUnlockErrorLog = postBackupUnlockErrorLog;
    }
}
