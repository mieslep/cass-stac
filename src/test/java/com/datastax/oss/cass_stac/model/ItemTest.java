package com.datastax.oss.cass_stac.model;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Polygon;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JtsModule());
    }

    @Test
    void testMinimalItemDeserialization() throws Exception {
        String json = """
                {
                  "stac_version": "1.0.0",
                  "stac_extensions": [],
                  "type": "Feature",
                  "id": "20201211_223832_CS2",
                  "bbox": [
                    172.91173669923782,
                    1.3438851951615003,
                    172.95469614953714,
                    1.3690476620161975
                  ],
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                      [
                        [
                          172.91173669923782,
                          1.3438851951615003
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
                          1.3438851951615003
                        ]
                      ]
                    ]
                  },
                  "properties": {
                    "datetime": "2020-12-11T22:38:32.125000Z"
                  },
                  "collection": "simple-collection",
                  "links": "[]",
                  "assets": "{}"
                }
                """;

        Item item = objectMapper.readValue(json, Item.class);
        assertNotNull(item);
        assertEquals("20201211_223832_CS2", item.getId());
        assertInstanceOf(Polygon.class, item.getGeometry());
        assertEquals("2020-12-11T22:38:32.125Z", item.getProperties().get("datetime").toString());
        assertEquals("simple-collection", item.getCollection());

        JsonNode itemJsonNode = item.toJson();
        JsonNode expectedJsonNode = objectMapper.readTree(json);
        assertEquals(expectedJsonNode, itemJsonNode, "The serialized JSON should match the expected JSON.");
    }

    @Test
    void testComplexItemDeserialization() throws Exception {
        String jsonBase = """
                {
                  "stac_version": "1.0.0",
                  "stac_extensions": [],
                  "type": "Feature",
                  "id": "20201211_223832_CS2",
                  "bbox": [
                    172.91173669923782,
                    1.3438851951615003,
                    172.95469614953714,
                    1.3690476620161975
                  ],
                  "geometry": {
                    "type": "Polygon",
                    "coordinates": [
                      [
                        [
                          172.91173669923782,
                          1.3438851951615003
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
                          1.3438851951615003
                        ]
                      ]
                    ]
                  },
                  "properties": {
                    "title": "Core Item",
                    "description": "A sample STAC Item that includes examples of all common metadata",
                    "start_datetime": "2020-12-11T22:38:32.125Z",
                    "end_datetime": "2020-12-11T22:38:32.327Z",
                    "created": "2020-12-12T01:48:13.725Z",
                    "updated": "2020-12-12T01:48:13.725Z",
                    "platform": "cool_sat1",
                    "instruments": [
                      "cool_sensor_v1"
                    ],
                    "constellation": "ion",
                    "mission": "collection 5624",
                    "gsd": 0.512
                  },
                  "collection": "simple-collection",""";
        String jsonLinks = """
                  [
                    {
                      "rel": "collection",
                      "href": "./collection.json",
                      "type": "application/json",
                      "title": "Simple Example Collection"
                    },
                    {
                      "rel": "root",
                      "href": "./collection.json",
                      "type": "application/json",
                      "title": "Simple Example Collection"
                    },
                    {
                      "rel": "parent",
                      "href": "./collection.json",
                      "type": "application/json",
                      "title": "Simple Example Collection"
                    },
                    {
                      "rel": "alternate",
                      "type": "text/html",
                      "href": "http://remotedata.io/catalog/20201211_223832_CS2/index.html",
                      "title": "HTML version of this STAC Item"
                    }
                  ],""";
        String jsonAssets = """
                  {
                    "analytic": {
                      "href": "https://storage.googleapis.com/open-cogs/stac-examples/20201211_223832_CS2_analytic.tif",
                      "type": "image/tiff; application=geotiff; profile=cloud-optimized",
                      "title": "4-Band Analytic",
                      "roles": [
                        "data"
                      ]
                    },
                    "thumbnail": {
                      "href": "https://storage.googleapis.com/open-cogs/stac-examples/20201211_223832_CS2.jpg",
                      "title": "Thumbnail",
                      "type": "image/png",
                      "roles": [
                        "thumbnail"
                      ]
                    },
                    "visual": {
                      "href": "https://storage.googleapis.com/open-cogs/stac-examples/20201211_223832_CS2.tif",
                      "type": "image/tiff; application=geotiff; profile=cloud-optimized",
                      "title": "3-Band Visual",
                      "roles": [
                        "visual"
                      ]
                    },
                    "udm": {
                      "href": "https://storage.googleapis.com/open-cogs/stac-examples/20201211_223832_CS2_analytic_udm.tif",
                      "title": "Unusable Data Mask",
                      "type": "image/tiff; application=geotiff;"
                    },
                    "json-metadata": {
                      "href": "http://remotedata.io/catalog/20201211_223832_CS2/extended-metadata.json",
                      "title": "Extended Metadata",
                      "type": "application/json",
                      "roles": [
                        "metadata"
                      ]
                    },
                    "ephemeris": {
                      "href": "http://cool-sat.com/catalog/20201211_223832_CS2/20201211_223832_CS2.EPH",
                      "title": "Satellite Ephemeris Metadata"
                    }
                  }
                }
                """;

        String json = jsonBase + "\"links\": " + jsonLinks + "\"assets\": " + jsonAssets;

        Item item = objectMapper.readValue(json, Item.class);
        assertNotNull(item);
        assertEquals("20201211_223832_CS2", item.getId());
        assertInstanceOf(Polygon.class, item.getGeometry());
        assertNull(item.getProperties().get("datetime"));
        assertEquals(OffsetDateTime.parse("2020-12-11T22:38:32.125Z"), item.getProperties().get("start_datetime"));
        assertEquals(OffsetDateTime.parse("2020-12-11T22:38:32.327Z"), item.getProperties().get("end_datetime"));
        assertEquals("simple-collection", item.getCollection());

        JsonNode itemJsonNode = item.toJson();
        JsonNode expectedJsonNode = objectMapper.readTree(json);
        assertEquals(expectedJsonNode, itemJsonNode, "The serialized JSON should match the expected JSON.");
    }
}
