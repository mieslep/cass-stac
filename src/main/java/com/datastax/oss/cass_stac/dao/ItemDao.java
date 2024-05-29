package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.config.ConfigManager;
import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.cass_stac.model.Item;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

import java.util.*;

public class ItemDao extends ADao<Item> {
    private static final Logger logger = LoggerFactory.getLogger(ItemDao.class);

    private final Map<String, String> propertyIndexMap = new HashMap<>();

    public ItemDao(CqlSession session, GeoTimePartition partitioner) throws ConfigException {
        super(session, partitioner);
        initializeIndexMap();
    }

    private void initializeIndexMap() {
        ConfigManager configManager = ConfigManager.getInstance();
        List<String> textIndices = configManager.getPropertyAsList("dao.item.property.IndexList.text");
        List<String> numberIndices = configManager.getPropertyAsList("dao.item.property.IndexList.number");
        List<String> booleanIndices = configManager.getPropertyAsList("dao.item.property.IndexList.boolean");
        List<String> timestampIndices = configManager.getPropertyAsList("dao.item.property.IndexList.timestamp");

        if (null != textIndices)
            textIndices.forEach(prop -> propertyIndexMap.put(prop, "indexed_properties_text"));
        if (null != numberIndices)
            numberIndices.forEach(prop -> propertyIndexMap.put(prop, "indexed_properties_double"));
        if (null != booleanIndices)
            booleanIndices.forEach(prop -> propertyIndexMap.put(prop, "indexed_properties_boolean"));
        if (null != timestampIndices)
            timestampIndices.forEach(prop -> propertyIndexMap.put(prop, "indexed_properties_timestamp"));
    }

    @Override
    public void initialize() throws ConfigException {
        String createTableQuery = """
            CREATE TABLE IF NOT EXISTS item (
                partition_id TEXT,
                id TEXT,
                collection TEXT,
                datetime TIMESTAMP,
                geometry BLOB,
                centroid VECTOR<float,2>,
                indexed_properties_text MAP<text,text>,
                indexed_properties_double MAP<text,double>,
                indexed_properties_boolean MAP<text,boolean>,
                indexed_properties_timestamp MAP<text,timestamp>,
                properties TEXT,
                additional_attributes TEXT,
                PRIMARY KEY ((partition_id), id)
            )""";
        session.execute(createTableQuery);

        session.execute("""
        CREATE CUSTOM INDEX IF NOT EXISTS item_datetime ON item (datetime) USING 'StorageAttachedIndex';
        """);

        session.execute("""
        CREATE CUSTOM INDEX IF NOT EXISTS item_properties_timestamp_entries ON item (ENTRIES(indexed_properties_timestamp)) USING 'StorageAttachedIndex';
        """);
    }

    @Override
    public void save(Item item) throws DaoException {
        try {
            String insertStmt = """
                INSERT INTO item
                  (partition_id,
                   id,
                   collection,
                   datetime,
                   geometry,
                   centroid,
                   indexed_properties_text,
                   indexed_properties_double,
                   indexed_properties_boolean,
                   indexed_properties_timestamp,
                   properties,
                   additional_attributes)
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            PreparedStatement ps = session.prepare(insertStmt);
            Point centroid = item.getGeometry().getCentroid();
            CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));

            OffsetDateTime datetime = (OffsetDateTime) (item.getProperties().containsKey("datetime") ? item.getProperties().get("datetime") : item.getProperties().get("start_datetime"));

            String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, datetime);

            Map<String, String> indexedTextProps = new HashMap<>();
            Map<String, Number> indexedNumberProps = new HashMap<>();
            Map<String, OffsetDateTime> indexedTimestampProps = new HashMap<>();
            Map<String, Boolean> indexedBooleanProps = new HashMap<>();

            item.getProperties().forEach((key, value) -> {
                String indexColumn = propertyIndexMap.get(key);
                if (indexColumn != null) {
                    try {
                        switch (indexColumn) {
                            case "indexed_properties_text":
                                if (value instanceof String)
                                    indexedTextProps.put(key, (String) value);
                                break;
                            case "indexed_properties_double":
                                if (value instanceof Number)
                                    indexedNumberProps.put(key, (Number) value);
                                break;
                            case "indexed_properties_boolean":
                                if (value instanceof Boolean)
                                    indexedBooleanProps.put(key, (Boolean) value);
                                break;
                            case "indexed_properties_timestamp":
                                if (value instanceof OffsetDateTime)
                                    indexedTimestampProps.put(key, (OffsetDateTime) value);
                                break;
                        }
                    } catch (ClassCastException e) {
                        logger.error("Type mismatch for property " + key + ", expected type based on configuration.");
                        throw e;
                    }
                }
            });

            BoundStatement boundStmt = ps.bind(
                    partitionId,
                    item.getId(),
                    item.getCollection(),
                    datetime,
                    item.getGeometryByteBuffer(),
                    centroidVector,
                    indexedTextProps,
                    indexedNumberProps,
                    indexedBooleanProps,
                    indexedTimestampProps,
                    item.getPropertiesAsString(),
                    item.getAdditionalAttributesAsString()
            );

            session.execute(boundStmt);
        }
        catch (Exception e) {
            throw new DaoException("An operation failed", e);
        }
    }

    @Override
    public Item get(Object id) {
        // Implementation for finding an item
        return null;
    }
}
