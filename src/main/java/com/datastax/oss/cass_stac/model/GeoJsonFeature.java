package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.n52.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBWriter;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoJsonFeature {
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String type = "Feature";
    private JsonNode geometryJson;
    private Geometry geometry;
    private Map<String, Object> properties;

    public GeoJsonFeature() {
        objectMapper.registerModule(new JtsModule());
    }

    public GeoJsonFeature(Geometry geometry, Map<String, Object> properties) {
        this();
        this.geometry = geometry;
        if (null != properties)
            this.properties = properties;
    }

    public GeoJsonFeature(Geometry geometry) {
        this(geometry, null);
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("geometry")
    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(JsonNode geometryJson) throws JsonProcessingException {
        this.geometryJson = geometryJson;
        if (null == geometryJson)
            this.geometry = null;
        else {
            this.geometry = objectMapper.treeToValue(geometryJson, Geometry.class);
        }
    }

    public JsonNode getGeometryJson() {
        return geometryJson;
    }

    @JsonProperty("properties")
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public ByteBuffer getGeometryByteBuffer() {
        byte[] bytes = (new WKBWriter()).write(geometry);
        return ByteBuffer.wrap(bytes);
    }

    public static Geometry fromGeometryByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        try {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            return (new WKBReader()).read(bytes);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse geometry from ByteBuffer", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GeoJsonFeature that = (GeoJsonFeature) o;
        return Objects.equals(geometry, that.geometry) &&
                Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geometry, properties);
    }

}
