package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item extends GeoJsonFeature {
    @JsonProperty("stac_version")
    private String stacVersion;

    @JsonProperty("id")
    private String id;

    @JsonProperty("bbox")
    private List<Float> bbox;

    @JsonProperty("collection")
    private String collection;

    @JsonProperty("properties")
    @JsonDeserialize(using = PropertiesWithDateDeserializer.class)
    private Map<String, Object> properties;

    // Leave some of these properties as "raw objects" for the time being
    @JsonProperty("stac_extensions")
    private JsonNode stacExtensions;

    @JsonProperty("assets")
    private JsonNode assets;

    @JsonProperty("links")
    private JsonNode links;

    public Item() {
        super();
    }

    // Getters and Setters
    public String getStacVersion() {
        return stacVersion;
    }

    public void setStacVersion(String stacVersion) {
        this.stacVersion = stacVersion;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Float> getBbox() {
        return bbox;
    }

    public void setBbox(List<Float> bbox) {
        this.bbox = bbox;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public JsonNode getStacExtensions() {
        return stacExtensions;
    }

    public void setStacExtensions(JsonNode stacExtensions) {
        this.stacExtensions = stacExtensions;
    }

    public JsonNode getAssets() {
        return assets;
    }

    public void setAssets(JsonNode assets) {
        this.assets = assets;
    }

    public JsonNode getLinks() {
        return links;
    }

    public void setLinks(JsonNode links) {
        this.links = links;
    }
}
