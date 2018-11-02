package de.evoila.cf.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yannic Remmet, Johannes Hiemer.
 */
public enum DestinationType {

    @JsonProperty("SWIFT")
    SWIFT("SWIFT"),

    @JsonProperty("S3")
    S3("S3");

    private String value;

    private DestinationType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
