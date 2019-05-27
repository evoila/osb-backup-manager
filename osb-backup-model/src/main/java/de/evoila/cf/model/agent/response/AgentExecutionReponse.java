package de.evoila.cf.model.agent.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.evoila.cf.model.enums.JobStatus;

import java.util.Date;

/**
 * @author Johannes Hiemer.
 */
public class AgentExecutionReponse {

    protected JobStatus status;

    protected String message;

    @JsonProperty("error_message")
    protected String errorMessage;

    @JsonProperty("start_time")
    protected Date startTime;

    @JsonProperty("end_time")
    protected Date endTime;

    @JsonProperty("execution_time_ms")
    protected long executionTime;

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long executionTime) {
        this.executionTime = executionTime;
    }
}
