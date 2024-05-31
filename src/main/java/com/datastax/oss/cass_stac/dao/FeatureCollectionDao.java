package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.cass_stac.model.FeatureCollection;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;

public class FeatureCollectionDao extends ADao<FeatureCollection> {

    private static final Logger logger = LoggerFactory.getLogger(FeatureCollectionDao.class);

    public FeatureCollectionDao(CqlSession session, GeoTimePartition partitioner) throws ConfigException {
        super(session, partitioner);
    }

    @Override
    public void initialize() throws ConfigException {
        String createTableQuery = """
                CREATE TABLE IF NOT EXISTS feature_collection (
                    item_id TEXT,
                    partition_id TEXT,
                    properties TEXT,
                    additional_attributes TEXT,
                    PRIMARY KEY ((item_id))
                )""";
        session.execute(createTableQuery);
    }

    private static final String insertStmt = """
        INSERT INTO feature_collection
          (item_id,
           partition_id,
           properties,
           additional_attributes)
         VALUES (?, ?, ?, ?)
        """;

    @Override
    public void save(FeatureCollection featureCollection) throws DaoException {
        try {
            String itemId = featureCollection.getItemId();
            ItemDao itemDao = DaoFactory.getInstance().getDao(DaoFactory.DaoType.ITEM);
            Pair<String, OffsetDateTime> ids = itemDao.getIds(itemId);
            if (null == ids) {
                throw new DaoException("Item id " + itemId + " not found by itemDao");
            }

            FeatureDao featureDao = DaoFactory.getInstance().getDao(DaoFactory.DaoType.FEATURE);
            String partitionId = ids.getLeft();
            OffsetDateTime datetime = ids.getRight();
            featureDao.save(partitionId, itemId, datetime, featureCollection.getFeatures());

            PreparedStatement pstmt = session.prepare(insertStmt);
            BoundStatement bstmt = pstmt.bind(
                    itemId,
                    partitionId,
                    featureCollection.getPropertiesAsString(),
                    featureCollection.getAdditionalAttributesAsString());

            session.execute(bstmt);

        } catch (Exception e) {
            throw new DaoException("An operation failed", e);
        }
    }

    @Override
    public FeatureCollection get(String partitionId, Object id) throws DaoException {
        throw new DaoException("Not implemented");
    }

}
