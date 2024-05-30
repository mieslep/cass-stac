package com.datastax.oss.cass_stac.model;

import com.datastax.oss.cass_stac.config.ConfigManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.locationtech.jts.geom.Geometry;

import java.util.HashSet;
import java.util.Objects;

public class Item extends GeoJsonFeature {

    @JsonProperty("collection")
    private String collection;

    public Item() {
        super();
        constructorInit();
    }

    public Item(String id, String collection, Geometry geometry, String propertiesString, String additionalAttributes) throws JsonProcessingException {
        super(geometry, propertiesString, additionalAttributes);
        constructorInit();
        setId(id);
        setCollection(collection);
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    private void constructorInit() {
        ConfigManager configManager = ConfigManager.getInstance();
        this.propertiesDateFields = new HashSet<>(configManager.getPropertyAsList("dao.item.property.DateFields", "datetime,start_datetime,end_datetime,created,updated"));
    }

    protected ObjectNode toObjectNode() {
        ObjectNode node = super.toObjectNode();
        node.put("collection", this.collection);
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        if (this == o) return true;
        if (getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(collection, item.collection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), collection);
    }

}
