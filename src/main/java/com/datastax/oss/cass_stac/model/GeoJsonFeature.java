package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonFeature {
    private static String type = "Feature";
    private Geometry geometry;
    private Map<String, Object> properties;

    // Default constructor, getters and setters
    public GeoJsonFeature() {
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("geometry")
    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    @JsonProperty("properties")
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
