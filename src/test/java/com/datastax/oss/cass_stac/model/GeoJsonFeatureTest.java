package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.databind.JsonNode;
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
    void testMinimalSerialization() throws Exception {
        GeoJsonFeature feature = new GeoJsonFeature();
        Polygon polygon = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(1, 0),
                new Coordinate(1, 1),
                new Coordinate(0, 1),
                new Coordinate(0, 0)
        });
        JsonNode geometryJson = objectMapper.valueToTree(polygon);
        feature.setGeometry(geometryJson);

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
    void testSerialization() throws Exception {
        String json = """
                {
                  "type": "Feature",
                  "id": "20201211_223832_CS2",
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                      [
                        [
                          172.91173669923782,
                          1.343885195161
                        ],
                        [
                          172.95469614953714,
                          1.3438851951615003
                        ],
                        [
                          172.95469614953714,
                          1.3690476620161975
                        ],
                        [
                          172.91173669923782,
                          1.3690476620161975
                        ],
                        [
                          172.91173669923782,
                          1.3438851951610000
                        ]
                      ]
                    ]
                  },
                  "properties": {
                    "datetime": "2020-12-11T22:38:32.125000Z"
                  }
                }
                """;

        GeoJsonFeature geoFeature = objectMapper.readValue(json, GeoJsonFeature.class);
        assertNotNull(geoFeature);
        assertEquals("20201211_223832_CS2", geoFeature.getId());
        assertInstanceOf(Polygon.class, geoFeature.getGeometry());
        assertEquals("2020-12-11T22:38:32.125000Z", geoFeature.getProperties().get("datetime").toString());

        JsonNode geoFeatureJsonNode = geoFeature.toJson();
        JsonNode expectedJsonNode = objectMapper.readTree(json);
        assertEquals(expectedJsonNode, geoFeatureJsonNode, "The serialized JSON should match the expected JSON.");
    }
}
