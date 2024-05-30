package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class PropertyObjectTest {

    private PropertyObject propertyObject;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws JsonProcessingException {
        String jsonProperties = "{\"name\":\"testName\", \"date\":\"2020-01-01T12:00:00+00:00\"}";
        String jsonAttributes = "{\"attr1\":\"value1\"}";
        propertyObject = new PropertyObject(jsonProperties, jsonAttributes);
        propertyObject.propertiesDateFields.add("date");
    }

    @Test
    @DisplayName("Test properties are set correctly from JSON string")
    void testPropertiesFromJsonString() throws JsonProcessingException {
        assertEquals("testName", propertyObject.getProperties().get("name"));
        assertEquals(OffsetDateTime.parse("2020-01-01T12:00:00+00:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                propertyObject.getProperties().get("date"));
    }

    @Test
    @DisplayName("Test additional attributes are set and retrieved correctly")
    void testPropertyGetter() throws JsonProcessingException {
        assertEquals("testName", propertyObject.getProperty("name"));
    }

    @Test
    @DisplayName("Test additional attributes are set and retrieved correctly")
    void testAdditionalAttributes() throws JsonProcessingException {
        assertEquals("value1", propertyObject.getAttribute("attr1"));
    }

    @Test
    @DisplayName("Test JSON serialization of properties")
    void testPropertiesSerialization() throws JsonProcessingException {
        String propertiesJson = propertyObject.getPropertiesAsString();
        JsonNode tree = objectMapper.readTree(propertiesJson);
        assertTrue(tree.has("name"));
        assertFalse(tree.has("unknown"));
    }

    @Test
    @DisplayName("Test setting and getting properties using Map")
    void testSetPropertiesUsingMap() {
        Map<String, Object> newProps = new HashMap<>();
        newProps.put("newName", "newValue");
        propertyObject.setProperties(newProps);
        assertEquals("newValue", propertyObject.getProperties().get("newName"));
        assertNotNull(propertyObject.getPropertiesJson());
    }

    @Test
    @DisplayName("Test equals method")
    void testEquals() throws JsonProcessingException {
        PropertyObject another = new PropertyObject("{\"name\":\"testName\", \"date\":\"2020-01-01T12:00:00+00:00\"}", "{\"attr1\":\"value1\"}");
        another.propertiesDateFields.add("date");
        assertEquals(propertyObject, another);
    }

    @Test
    @DisplayName("Test conversion of JSON nodes to Object")
    void testConvertNode() throws JsonProcessingException {
        JsonNode intNode = objectMapper.readTree("123");
        JsonNode boolNode = objectMapper.readTree("true");
        JsonNode stringNode = objectMapper.readTree("\"test\"");

        assertEquals(123, propertyObject.convertNode(intNode));
        assertEquals(true, propertyObject.convertNode(boolNode));
        assertEquals("test", propertyObject.convertNode(stringNode));
    }

}
