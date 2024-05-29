package com.datastax.oss.cass_stac.model;

import com.datastax.oss.cass_stac.config.ConfigManager;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Item extends GeoJsonFeature {
    @JsonProperty("id")
    private String id;

    @JsonProperty("collection")
    private String collection;

    @JsonProperty("properties")
    private JsonNode propertiesJson;
    private Map<String, Object> properties;  // Lazy-loaded properties map, converted using custom deserializer
    private final Set<String> propertiesDateFields;

    @JsonAnySetter
    private Map<String, JsonNode> additionalAttributes = new HashMap<>();

    public Item() {
        super();
        ConfigManager configManager = ConfigManager.getInstance();
        propertiesDateFields = new HashSet<>(configManager.getPropertyAsList("dao.item.property.DateFields","datetime,start_datetime,end_datetime,created,updated"));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public void setPropertiesJson(JsonNode propertiesJson) throws JsonProcessingException {
        this.propertiesJson = propertiesJson;
        this.properties = null;
    }

    public Map<String, Object> getProperties() {
        if (properties == null && propertiesJson != null) {
            properties = deserializeProperties(propertiesJson);
        }
        return properties;
    }

    public String getPropertiesAsString() throws JsonProcessingException {
        return objectMapper.writeValueAsString(propertiesJson);
    }

    public void setAdditionalAttributes(String key, JsonNode value) {
        additionalAttributes.put(key, value);
    }

    public void setAdditionalAttributes(String additionalAttributes) throws JsonProcessingException {
        this.additionalAttributes = new HashMap<>();
        Map<String, JsonNode> properties = objectMapper.readValue(additionalAttributes, new TypeReference<Map<String, JsonNode>>() {});
        this.additionalAttributes.putAll(properties);
    }

    public Map<String, JsonNode> getAdditionalAttributes() {
        return additionalAttributes;
    }

    public String getAdditionalAttributesAsString() throws JsonProcessingException {
        return objectMapper.writeValueAsString(additionalAttributes);
    }

    public JsonNode toJson() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", this.id);
        node.put("collection", this.collection);
        node.set("geometry", this.getGeometryJson());
        node.set("properties", this.propertiesJson);
        additionalAttributes.forEach(node::set);
        return node;
    }

    private Map<String, Object> deserializeProperties(JsonNode propertiesJson) {
        Map<String, Object> properties = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = propertiesJson.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (propertiesDateFields.contains(fieldName) && !fieldValue.isNull()) {
                properties.put(fieldName, OffsetDateTime.parse(fieldValue.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            } else {
                properties.put(fieldName, convertNode(fieldValue));
            }
        }
        return properties;
    }

    private Object convertNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        } else if (node.isNumber()) {
            return node.numberValue();
        } else if (node.isBoolean()) {
            return node.booleanValue();
        } else if (node.isTextual()) {
            return node.textValue();
        } else {
            // Return the JsonNode itself for non-basic types or as a fallback
            return node;
        }
    }

}
