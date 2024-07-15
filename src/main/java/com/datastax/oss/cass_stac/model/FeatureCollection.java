package com.datastax.oss.cass_stac.model;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.config.ConfigManager;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeatureCollection extends PropertyObject {

    @JsonProperty("features")
    private List<GeoJsonFeature> features;

    // @JsonIgnore
    @JsonProperty("item_id")
    private String itemId;

    private static final String ITEM_ID_KEY_NAME = "dao.featureCollection.property.itemIdName";
    private static final String ITEM_ID_NAME = ConfigManager.getInstance().getProperty(ITEM_ID_KEY_NAME, "item_id");

    // Getters and Setters
    public String getItemId() throws ConfigException {
        if (null == this.itemId) {
            this.itemId = (String) getProperties().get(ITEM_ID_NAME);
            if (null == this.itemId) {
                throw new ConfigException("Item identifier expected on " + ITEM_ID_KEY_NAME + "=" + ITEM_ID_NAME + " is not found on object's properties map.");
            }
        }
        return itemId;
    }

    public List<GeoJsonFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<GeoJsonFeature> features) {
        this.features = features;
    }
}
