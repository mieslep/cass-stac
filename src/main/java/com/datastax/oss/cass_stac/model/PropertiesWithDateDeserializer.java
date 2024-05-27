package com.datastax.oss.cass_stac.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PropertiesWithDateDeserializer extends JsonDeserializer<Map<String, Object>> {

    private static final Set<String> PROPERTIES_DATE_FIELDS = new HashSet<>();
    static {
        PROPERTIES_DATE_FIELDS.add("datetime");
        PROPERTIES_DATE_FIELDS.add("start_datetime");
        PROPERTIES_DATE_FIELDS.add("end_datetime");
        PROPERTIES_DATE_FIELDS.add("created");
        PROPERTIES_DATE_FIELDS.add("updated");
    }

    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        Map<String, Object> properties = new HashMap<>();
        JsonNode node = p.getCodec().readTree(p);
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode fieldValue = field.getValue();

            if (PROPERTIES_DATE_FIELDS.contains(fieldName)) {
                if (fieldValue.isNull()) {
                    properties.put(fieldName, null);
                } else {
                    properties.put(fieldName, OffsetDateTime.parse(fieldValue.asText(), DateTimeFormatter.ISO_OFFSET_DATE_TIME));
                }
            } else {
                properties.put(fieldName, fieldValue);
            }
        }

        return properties;
    }
}
