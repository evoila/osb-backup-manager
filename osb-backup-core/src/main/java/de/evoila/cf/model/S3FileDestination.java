package de.evoila.cf.model;

public class S3FileDestination extends FileDestination {

    private String region;

    private String bucket;

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }
}
