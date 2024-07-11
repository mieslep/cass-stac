package com.datastax.oss.cass_stac.dao;

import com.datastax.oss.cass_stac.config.ConfigException;
import com.datastax.oss.cass_stac.config.ConfigManager;
import com.datastax.oss.cass_stac.dao.partitioning.GeoTimePartition;
import com.datastax.oss.cass_stac.dao.util.GeometryUtil;
import com.datastax.oss.cass_stac.dao.util.PropertyUtil;
import com.datastax.oss.cass_stac.model.GeoJsonFeature;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FeatureDao extends ADao<GeoJsonFeature> {

    private static final Logger logger = LoggerFactory.getLogger(FeatureDao.class);

    private static final Map<String, String> PROPERTY_INDEX_MAP = PropertyUtil.getPropertyMap("feature.property.IndexList");
    private static final String LABEL_PROPERTY_NAME = ConfigManager.getInstance().getProperty("dao.feature.label.property_name");
    private static final String LABEL_PROPERTY_MISSING_VALUE = ConfigManager.getInstance().getProperty("dao.feature.label.missingValue", "unclassified");
    private static final Integer BATCH_SIZE = ConfigManager.getInstance().getIntProperty("dao.feature.batchSize", 50);

    public FeatureDao(CqlSession session, GeoTimePartition partitioner) throws ConfigException {
        super(session, partitioner);
    }

    @Override
    public void initialize() throws ConfigException {
        String createTableQuery = """
                CREATE TABLE IF NOT EXISTS feature (
                    partition_id TEXT,
                    item_id TEXT,
                    label TEXT,
                    datetime TIMESTAMP,
                    centroid VECTOR<float,2>,
                    geometry BLOB,
                    indexed_properties_text MAP<text,text>,
                    indexed_properties_double MAP<text,double>,
                    indexed_properties_boolean MAP<text,boolean>,
                    indexed_properties_timestamp MAP<text,timestamp>,
                    properties TEXT,
                    additional_attributes TEXT,
                    PRIMARY KEY ((partition_id, item_id), label, datetime, centroid)
                )""";
        session.execute(createTableQuery);
    }

    private static final String insertQuery = """
        INSERT INTO feature (
            partition_id,
            item_id,
            label,
            centroid,
            datetime,
            geometry,
            indexed_properties_text,
            indexed_properties_double,
            indexed_properties_boolean,
            indexed_properties_timestamp,
            properties,
            additional_attributes)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    public void save(String partition_id, String item_id, OffsetDateTime datetime, List<GeoJsonFeature> geoJsonFeatures) throws DaoException {
        if (geoJsonFeatures == null || geoJsonFeatures.isEmpty()) {
            logger.warn("No features provided to save: partition="+partition_id+", item="+item_id+", datetime="+datetime.toString());
            return;
        }

        // Do this as a single-partition unlogged batch, but limiting batch size to the configured parameter
        int count = 0;

        try {
            PreparedStatement ps = session.prepare(insertQuery);

            BatchStatementBuilder batchBuilder = BatchStatement.builder(DefaultBatchType.UNLOGGED);

            for (GeoJsonFeature feature : geoJsonFeatures) {
                String label = (null == LABEL_PROPERTY_NAME) ? LABEL_PROPERTY_MISSING_VALUE : feature.getProperty(LABEL_PROPERTY_NAME, LABEL_PROPERTY_MISSING_VALUE);
                CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) feature.getGeometry().getCentroid().getY(), (float) feature.getGeometry().getCentroid().getX()));

                PropertyUtil propertyUtil = new PropertyUtil(PROPERTY_INDEX_MAP, feature);

                BoundStatement boundStmt = ps.bind(
                        partition_id,
                        item_id,
                        label,
                        centroidVector,
                        datetime,
                        GeometryUtil.toByteBuffer(feature.getGeometry()),
                        propertyUtil.getIndexedTextProps(),
                        propertyUtil.getIndexedNumberProps(),
                        propertyUtil.getIndexedBooleanProps(),
                        propertyUtil.getIndexedTimestampProps(),
                        feature.getPropertiesAsString(),
                        feature.getAdditionalAttributesAsString()
                );

                batchBuilder.addStatement(boundStmt);
                count++;
                // flush and reset the batch if we reach batch size
                if (count == BATCH_SIZE) {
                    session.execute(batchBuilder.build());
                    batchBuilder = BatchStatement.builder(DefaultBatchType.UNLOGGED);
                    count = 0;
                }
            }

            // flush any outstanding records
            if (count > 0) {
                session.execute(batchBuilder.build());
            }

        } catch (Exception e) {
            throw new DaoException("Failed to save GeoJson features", e);
        }
    }

    @Override
    public void save(GeoJsonFeature geoJsonFeature) throws DaoException {
        throw new DaoException("Not implemented");
    }

    @Override
    public GeoJsonFeature get(String partitionId, Object id) throws DaoException {
        throw new DaoException("Not implemented");
    }

}
