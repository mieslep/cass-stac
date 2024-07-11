package com.datastax.oss.cass_stac.config;

public class DatabaseConfig {
    private int geoResolution;
    private String timeResolution;
    private int batchSize;
    private int readParallelism;
    private int writeParallelism;

    // Getters and setters
    public int getGeoResolution() {
        return geoResolution;
    }

    public void setGeoResolution(int geoResolution) {
        this.geoResolution = geoResolution;
    }

    public String getTimeResolution() {
        return timeResolution;
    }

    public void setTimeResolution(String timeResolution) {
        this.timeResolution = timeResolution;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getReadParallelism() {
        return readParallelism;
    }

    public void setReadParallelism(int readParallelism) {
        this.readParallelism = readParallelism;
    }

    public int getWriteParallelism() {
        return writeParallelism;
    }

    public void setWriteParallelism(int writeParallelism) {
        this.writeParallelism = writeParallelism;
    }

}
