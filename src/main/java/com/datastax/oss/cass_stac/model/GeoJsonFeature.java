package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.n52.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;

import java.nio.ByteBuffer;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonFeature {
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    private static String type = "Feature";
    private JsonNode geometryJson;
    private Geometry geometry;
    private Map<String, Object> properties;
    private final WKBWriter wkbWriter = new WKBWriter();

    public GeoJsonFeature() {
        objectMapper.registerModule(new JtsModule());
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("geometry")
    public Geometry getGeometry() {
        return geometry;
    }

    public JsonNode getGeometryJson() {
        return geometryJson;
    }

    public ByteBuffer getGeometryByteBuffer() {
        byte[] bytes = wkbWriter.write(geometry);
        return ByteBuffer.wrap(bytes);
    }

    public void setGeometry(JsonNode geometryJson) throws JsonProcessingException {
        this.geometryJson = geometryJson;
        if (null == geometryJson)
            this.geometry = null;
        else {
            this.geometry = objectMapper.treeToValue(geometryJson, Geometry.class);
        }
    }

    @JsonProperty("properties")
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
