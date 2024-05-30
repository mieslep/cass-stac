package com.datastax.oss.cass_stac.model;

import com.datastax.oss.cass_stac.config.ConfigManager;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.n52.jackson.datatype.jts.JtsModule;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

public class GeoJsonFeature extends PropertyObject {

    @JsonProperty("type")
    private static final String TYPE = "Feature";

    static {
        objectMapper.registerModule(new JtsModule(ConfigManager.getInstance().getIntProperty("geojson.coordinateDecimalPlaces", 16)));
    }

    @JsonProperty("id")
    private String id;
    @JsonProperty("geometry")
    private Geometry geometry;

    public GeoJsonFeature() {
        super();
    }

    public GeoJsonFeature(Geometry geometry, String propertiesString, String additionalAttributes) throws JsonProcessingException {
        super(propertiesString, additionalAttributes);
        this.geometry = geometry;
    }

    public GeoJsonFeature(Geometry geometry, Map<String, Object> properties) {
        super();
        this.geometry = geometry;
        setProperties(properties);
    }

    public GeoJsonFeature(ByteBuffer geometryByteBuffer, Map<String, Object> properties) {
        this(fromGeometryByteBuffer(geometryByteBuffer), properties);
    }

    public GeoJsonFeature(Geometry geometry) {
        this(geometry, null);
    }

    public GeoJsonFeature(ByteBuffer geometryByteBuffer) {
        this(fromGeometryByteBuffer(geometryByteBuffer));
    }

    public static Geometry fromGeometryByteBuffer(ByteBuffer byteBuffer) {
        if (byteBuffer == null) {
            return null;
        }
        try {
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.duplicate().get(bytes);
            return new WKBReader().read(bytes);
        } catch (ParseException e) {
            throw new RuntimeException("Failed to parse geometry from ByteBuffer", e);
        }
    }

    public String getType() {
        return TYPE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    protected void setGeometry(JsonNode geometryJson) throws JsonProcessingException {
        this.geometry = geometryJson == null ? null : objectMapper.treeToValue(geometryJson, Geometry.class);
    }

    public ByteBuffer getGeometryByteBuffer() {
        if (geometry == null) {
            return ByteBuffer.allocate(0);
        }
        byte[] bytes = new WKBWriter().write(geometry);
        return ByteBuffer.wrap(bytes);
    }

    @Override
    protected ObjectNode toObjectNode() {
        ObjectNode node = super.toObjectNode();
        node.put("id", id);
        node.set("geometry", objectMapper.valueToTree(geometry));
        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!super.equals(o)) return false;
        if (getClass() != o.getClass()) return false;
        GeoJsonFeature that = (GeoJsonFeature) o;
        return Objects.equals(id, that.id) && Objects.equals(geometry, that.geometry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, geometry);
    }
}
