package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.bedatadriven.jackson.datatype.jts.JtsModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeoJsonFeatureTest {
    private ObjectMapper objectMapper;
    private GeometryFactory geometryFactory;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JtsModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        geometryFactory = new GeometryFactory();
    }

    @Test
    void testSerialization() throws Exception {
        GeoJsonFeature feature = new GeoJsonFeature();
        feature.setGeometry(geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
                new Coordinate(0, 0)
        }));

        Map<String, Object> properties = new HashMap<>();
        properties.put("name", "Test Area");
        properties.put("elevation", 1000);
        feature.setProperties(properties);

        String json = objectMapper.writeValueAsString(feature);

        String expectedJson = """
            {
              "properties": {
                "name": "Test Area",
                "elevation": 1000
              },
              "geometry": {
                "type": "Polygon"
              }
            }
            """;
        JSONAssert.assertEquals(expectedJson, json, false);
    }

    @Test
    void testDeserialization() throws Exception {
        String json = """
        {
          "type": "Feature",
          "geometry": {
            "type": "Polygon",
            "coordinates": [
              [
                [0.0, 0.0], [1.0, 0.0], [1.0, 1.0], [0.0, 1.0], [0.0, 0.0]
              ]
            ]
          },
          "properties": {
            "name": "Test Area",
            "elevation": 1000
          }
        }
        """;

        GeoJsonFeature feature = objectMapper.readValue(json, GeoJsonFeature.class);
        assertNotNull(feature.getGeometry(), "Geometry should not be null.");
        assertInstanceOf(Polygon.class, feature.getGeometry(), "Geometry type should be Polygon.");
        assertEquals(1000, feature.getProperties().get("elevation"), "Elevation should match the JSON value.");
        assertEquals("Test Area", feature.getProperties().get("name"), "Name property should match the JSON value.");
    }
}
