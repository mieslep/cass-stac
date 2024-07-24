package com.datastax.oss.cass_stac.util;

import com.datastax.oss.cass_stac.model.ItemModelRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoJsonParser {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(GeoJsonParser.class);

    public static ItemModelRequest parseGeoJsonContent(String geoJsonContent) throws IOException {
        logger.debug("Parsing GeoJSON content.");
        JsonNode rootNode = objectMapper.readTree(geoJsonContent);

        // Extract properties from content
        JsonNode contentNode = rootNode.get("content");
        if (contentNode != null) {
            JsonNode propertiesNode = contentNode.get("properties");
            if (propertiesNode != null) {
                ((ObjectNode) rootNode).set("properties", propertiesNode);
            } else {
                logger.warn("No 'properties' field found in 'content'.");
            }
        } else {
            logger.warn("No 'content' field found in GeoJSON.");
        }

        return objectMapper.treeToValue(rootNode, ItemModelRequest.class);
    }
}
