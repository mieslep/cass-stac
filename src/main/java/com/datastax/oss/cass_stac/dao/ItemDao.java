package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.cass_stac.dao.util.GeometryUtil;
import com.datastax.oss.cass_stac.dao.util.PropertyUtil;
import com.datastax.oss.cass_stac.model.Item;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;

public class ItemDao extends ADao<Item> {
    private static final Logger logger = LoggerFactory.getLogger(ItemDao.class);

    private static final Map<String, String> propertyIndexMap = PropertyUtil.getPropertyMap("dao.item.property.IndexList");
    ;
    private static final String insertMainStmt = """
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
    private static final String insertIdsStmt = """
            INSERT INTO item_ids
              (id,
               partition_id,
               datetime)
             VALUES (?, ?, ?)
            """;
    private static final String getQuery = """
            SELECT collection,
                   geometry,
                   properties,
                   additional_attributes
              FROM item
             WHERE partition_id = ?
               AND id = ?
            """;
    private static final String getIdsQuery = """
            SELECT partition_id,
                   datetime
              FROM item_ids
             WHERE id = ?
            """;

    public ItemDao(CqlSession session, GeoTimePartition partitioner) throws ConfigException {
        super(session, partitioner);
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

        createTableQuery = """
                CREATE TABLE IF NOT EXISTS item_ids (
                    id TEXT,
                    partition_id TEXT,
                    datetime TIMESTAMP,
                    PRIMARY KEY ((id))
                )""";
        session.execute(createTableQuery);

    }

    @Override
    public void save(Item item) throws DaoException {
        try {

            PropertyUtil propertyUtil = new PropertyUtil(propertyIndexMap, item);
            Point centroid = item.getGeometry().getCentroid();
            CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));

            OffsetDateTime datetime = (OffsetDateTime) (item.getProperties().containsKey("datetime") ? item.getProperties().get("datetime") : item.getProperties().get("start_datetime"));
            String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, datetime);
            String id = item.getId();

            PreparedStatement ps = session.prepare(insertIdsStmt);
            BoundStatement boundStmt = ps.bind(
                    id,
                    partitionId,
                    datetime);
            session.execute(boundStmt);

            ps = session.prepare(insertMainStmt);
            boundStmt = ps.bind(
                    partitionId,
                    id,
                    item.getCollection(),
                    datetime,
                    GeometryUtil.toByteBuffer(item.getGeometry()),
                    centroidVector,
                    propertyUtil.getIndexedTextProps(),
                    propertyUtil.getIndexedNumberProps(),
                    propertyUtil.getIndexedBooleanProps(),
                    propertyUtil.getIndexedTimestampProps(),
                    item.getPropertiesAsString(),
                    item.getAdditionalAttributesAsString());
            session.execute(boundStmt);

        } catch (Exception e) {
            throw new DaoException("An operation failed", e);
        }
    }

    @Override
    public Item get(String partitionId, Object id) throws DaoException {
        try {
            PreparedStatement pstmt = session.prepare(getQuery);
            BoundStatement boundStmt = pstmt.bind(partitionId, id);

            ResultSet rs = session.execute(boundStmt);
            Row row = rs.one();

            if (row != null) {
                String collection = row.getString("collection");
                ByteBuffer geometryByteBuffer = row.getByteBuffer("geometry");
                Geometry geometry = GeometryUtil.fromGeometryByteBuffer(geometryByteBuffer);
                String propertiesString = row.getString("properties");
                String additionalAttributesString = row.getString("additional_attributes");

                return new Item((String) id, collection, geometry, propertiesString, additionalAttributesString);
            }
        } catch (Exception e) {
            throw new DaoException("Problem fetching item", e);
        }

        return null;
    }

    public Item get(String id) throws DaoException {
        try {
            Pair<String, OffsetDateTime> partitionDatetime = this.getPartitionAndDatetime(id);
            String partitionId = partitionDatetime.getLeft();

            PreparedStatement pstmt = session.prepare(getQuery);
            BoundStatement boundStmt = pstmt.bind(partitionId, id);

            ResultSet rs = session.execute(boundStmt);
            Row row = rs.one();

            if (row != null) {
                String collection = row.getString("collection");
                ByteBuffer geometryByteBuffer = row.getByteBuffer("geometry");
                Geometry geometry = GeometryUtil.fromGeometryByteBuffer(geometryByteBuffer);
                String propertiesString = row.getString("properties");
                String additionalAttributesString = row.getString("additional_attributes");

                return new Item((String) id, collection, geometry, propertiesString, additionalAttributesString);
            }
        } catch (Exception e) {
            throw new DaoException("Problem fetching item", e);
        }

        return null;
    }
    
    public Pair<String, OffsetDateTime> getPartitionAndDatetime(String id) throws DaoException {
        try {

            PreparedStatement pstmt = session.prepare(getIdsQuery);
            BoundStatement boundStmt = pstmt.bind(id);

            ResultSet rs = session.execute(boundStmt);
            Row row = rs.one();

            if (row != null) {
                String partition_id = row.getString("partition_id");
                OffsetDateTime datetime = row.get("datetime", OffsetDateTime.class);

                return Pair.of(partition_id, datetime);
            }
        } catch (Exception e) {
            throw new DaoException("Problem fetching item", e);
        }

        return null;
    }
}
