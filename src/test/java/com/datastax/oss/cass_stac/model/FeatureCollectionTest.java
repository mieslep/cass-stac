package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import static org.junit.jupiter.api.Assertions.*;

class FeatureCollectionTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.bedatadriven.jackson.datatype.jts.JtsModule());
    }

    @Test
    void testFeatureCollectionDeserialization() throws Exception {
        String json = """
                {
                    "name": "mmsegmentation-loveda-b segmentations",
                    "type": "FeatureCollection",
                    "crs": {
                        "type": "name",
                        "properties": {
                            "name": "urn:ogc:def:crs:OGC:1.3:CRS84"
                        }
                    },
                    "properties": {
                        "item_id": "20230122_095631_SN29_QUICKVIEW_VISUAL_1_1_10_SATL-2KM-39N_554_2776",
                        "model": {
                            "id": "mmsegmentation-loveda-b",
                            "params": {
                                "rotation": 0
                            }
                        }
                    },
                    "features": []
                }
                """;

        FeatureCollection featureCollection = objectMapper.readValue(json, FeatureCollection.class);
        assertNotNull(featureCollection);
        assertEquals("20230122_095631_SN29_QUICKVIEW_VISUAL_1_1_10_SATL-2KM-39N_554_2776", featureCollection.getItemId());
        assertEquals("mmsegmentation-loveda-b segmentations", featureCollection.getAttribute("name"));
        assertEquals("FeatureCollection", featureCollection.getAttribute("type"));
        assertNotNull(featureCollection.getAttribute("crs"));
        assertNotNull(featureCollection.getProperties());
        assertEquals(0, featureCollection.getFeatures().size());
    }

    @Test
    void testFeatureCollectionWithFeaturesDeserialization() throws Exception {
        String json = """
                {
                    "type": "FeatureCollection",
                    "properties": {
                        "item_id": "20230122_095631_SN29_QUICKVIEW_VISUAL_1_1_10_SATL-2KM-39N_554_2776"
                        },
                    "features": [
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "Polygon",
                                "coordinates": [
                                    [
                                        [0.0, 0.0],
                                        [1.0, 0.0],
                                        [1.0, 1.0],
                                        [0.0, 1.0],
                                        [0.0, 0.0]
                                    ]
                                ]
                            },
                            "properties": {
                                "name": "Test Feature 1"
                            }
                        },
                        {
                            "type": "Feature",
                            "geometry": {
                                "type": "Polygon",
                                "coordinates": [
                                    [
                                        [2.0, 2.0],
                                        [3.0, 2.0],
                                        [3.0, 3.0],
                                        [2.0, 3.0],
                                        [2.0, 2.0]
                                    ]
                                ]
                            },
                            "properties": {
                                "name": "Test Feature 2"
                            }
                        }
                    ]
                }
                """;

        FeatureCollection featureCollection = objectMapper.readValue(json, FeatureCollection.class);
        assertNotNull(featureCollection);
        assertEquals("20230122_095631_SN29_QUICKVIEW_VISUAL_1_1_10_SATL-2KM-39N_554_2776", featureCollection.getItemId());
        assertNull(featureCollection.getAttribute("name"));
        assertEquals("FeatureCollection", featureCollection.getAttribute("type"));
        assertNull(featureCollection.getAttribute("crs"));
        assertNotNull(featureCollection.getProperties());
        assertEquals(2, featureCollection.getFeatures().size());

        // Check the first feature
        GeoJsonFeature feature1 = featureCollection.getFeatures().get(0);
        assertNotNull(feature1);
        assertInstanceOf(Polygon.class, feature1.getGeometry());
        assertEquals("Test Feature 1", feature1.getProperty("name"));

        // Check the second feature
        GeoJsonFeature feature2 = featureCollection.getFeatures().get(1);
        assertNotNull(feature2);
        assertInstanceOf(Polygon.class, feature2.getGeometry());
        assertEquals("Test Feature 2", feature2.getProperty("name"));
    }
}
