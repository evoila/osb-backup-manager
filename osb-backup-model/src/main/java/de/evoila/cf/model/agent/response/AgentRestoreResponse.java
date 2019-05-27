package de.evoila.cf.model.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Johannes Hiemer.
 */
public class AgentRestoreResponse extends AgentExecutionReponse {

    @JsonProperty("pre_restore_lock_log")
    private String preRestoreLockLog;

    @JsonProperty("pre_restore_lock_errorlog")
    private String preRestoreLockErrorLog;

    @JsonProperty("restore_log")
    private String restoreLog;

    @JsonProperty("restore_errorlog")
    private String restoreErrorLog;

    @JsonProperty("restore_cleanup_log")
    private String restoreCleanupLog;

    @JsonProperty("restore_cleanup_errorlog")
    private String restoreCleanupErrorLog;

    @JsonProperty("post_restore_unlock_log")
    private String postRestoreUnlockLog;

    @JsonProperty("post_restore_unlock_errorlog")
    private String postRestoreUnlockErrorLog;

    public String getPreRestoreLockLog() {
        return preRestoreLockLog;
    }

    public void setPreRestoreLockLog(String preRestoreLockLog) {
        this.preRestoreLockLog = preRestoreLockLog;
    }

    public String getPreRestoreLockErrorLog() {
        return preRestoreLockErrorLog;
    }

    public void setPreRestoreLockErrorLog(String preRestoreLockErrorLog) {
        this.preRestoreLockErrorLog = preRestoreLockErrorLog;
    }

    public String getRestoreLog() {
        return restoreLog;
    }

    public void setRestoreLog(String restoreLog) {
        this.restoreLog = restoreLog;
    }

    public String getRestoreErrorLog() {
        return restoreErrorLog;
    }

    public void setRestoreErrorLog(String restoreErrorLog) {
        this.restoreErrorLog = restoreErrorLog;
    }

    public String getRestoreCleanupLog() {
        return restoreCleanupLog;
    }

    public void setRestoreCleanupLog(String restoreCleanupLog) {
        this.restoreCleanupLog = restoreCleanupLog;
    }

    public String getRestoreCleanupErrorLog() {
        return restoreCleanupErrorLog;
    }

    public void setRestoreCleanupErrorLog(String restoreCleanupErrorLog) {
        this.restoreCleanupErrorLog = restoreCleanupErrorLog;
    }

    public String getPostRestoreUnlockLog() {
        return postRestoreUnlockLog;
    }

    public void setPostRestoreUnlockLog(String postRestoreUnlockLog) {
        this.postRestoreUnlockLog = postRestoreUnlockLog;
    }

    public String getPostRestoreUnlockErrorLog() {
        return postRestoreUnlockErrorLog;
    }

    public void setPostRestoreUnlockErrorLog(String postRestoreUnlockErrorLog) {
        this.postRestoreUnlockErrorLog = postRestoreUnlockErrorLog;
    }
}
