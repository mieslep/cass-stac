package com.datastax.oss.cass_stac.service;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datastax.oss.cass_stac.dao.FeatureCollectionDao;
import com.datastax.oss.cass_stac.entity.*;
import com.datastax.oss.cass_stac.model.FeatureModelRequest;
import com.datastax.oss.cass_stac.model.FeatureModelResponse;

import com.datastax.oss.cass_stac.util.PropertyUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import com.datastax.oss.cass_stac.dao.FeatureDao;
import com.datastax.oss.cass_stac.dao.GeoTimePartition;
import com.datastax.oss.cass_stac.dto.FeatureDto;
import com.datastax.oss.cass_stac.dto.GeometryDto;
import com.datastax.oss.cass_stac.util.GeometryUtil;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureService {
        private final FeatureDao featureDao;
        private final FeatureCollectionDao featureCollectionDao;

        private static final Map<String, String> propertyIndexMap = PropertyUtil.getPropertyMap("dao.feature.property.IndexList");

        public void add(FeatureDto dto) {
		final Feature feature = convertFeatureToDao(dto);
        final Feature ft = featureDao.save(feature);
        final FeatureCollection featureCollection = createFeatureCollection(ft);
        featureCollectionDao.save(featureCollection);
        }

        public FeatureModelResponse getFeatureById(final String item_id) {
                final FeatureCollection featureCollection = featureCollectionDao.findById(item_id)
                        .orElseThrow(() -> new RuntimeException(item_id + " is not found"));
                final String partitionId = featureCollection.getPartition_id();
                final FeaturePrimaryKey pk = new FeaturePrimaryKey();
                pk.setPartition_id(partitionId);
                pk.setItem_id(item_id);
                final Feature feature = featureDao.findFeatureByPartitionIdAndId(partitionId, item_id).stream().findFirst().get();
                final String label = feature.getId().getLabel();
                final ByteBuffer geometryByteBuffer = feature.getGeometry();
                final Geometry geometry = GeometryUtil.fromGeometryByteBuffer(geometryByteBuffer);

                final String propertiesString = feature.getProperties();
                final String additionalAttributesString = feature.getAdditional_attributes();

                try {
                        return new FeatureModelResponse((String) item_id, label, geometry.toString(), propertiesString, additionalAttributesString);
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                }
        }

        public List<FeatureDto> getFeature(final String partitionid, final String item_id) {
                final List<Feature> feature;
                if (item_id == null || item_id.isEmpty()) {
                        feature = featureDao.findFeatureByPartitionId(partitionid);
                } else {
                        feature = featureDao.findFeatureByPartitionIdAndId(partitionid, item_id);
                }
                if (feature == null || feature.isEmpty()){
                        throw new RuntimeException("No data found");
                }
                return feature.stream().map(this::convertFeatureToDto).collect(Collectors.toList());
        }

        public void save(final String json) {

                final Feature feature = converFeatureJsonToFeature(json);
                final Feature ft = featureDao.save(feature);
                final FeatureCollection featureCollection = createFeatureCollection(ft);
                featureCollectionDao.save(featureCollection);

        }

        private Feature converFeatureJsonToFeature(final String json) {
                final ObjectMapper objectMapper = new ObjectMapper();
                try {

                        final int geoResolution = 6;
                        final GeoTimePartition.TimeResolution timeResolution = GeoTimePartition.TimeResolution.valueOf("MONTH");
                        final GeoTimePartition partitioner = new GeoTimePartition(geoResolution, timeResolution);

                        final FeatureModelRequest featureModel = objectMapper.readValue(json, FeatureModelRequest.class);
                        final PropertyUtil propertyUtil = new PropertyUtil(propertyIndexMap, featureModel);
                        Point centroid = featureModel.getGeometry().getCentroid();
                        CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));

                        OffsetDateTime datetime = (OffsetDateTime) (featureModel.getProperties().containsKey("datetime") ? featureModel.getProperties().get("datetime") : featureModel.getProperties().get("start_datetime"));
                        String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, datetime);
                        String item_id = featureModel.getItem_id();

                        final FeaturePrimaryKey pk = new FeaturePrimaryKey();
                        pk.setItem_id(item_id);
                        pk.setPartition_id(partitionId);
                        pk.setLabel(featureModel.getLabel());
                        pk.setDatetime(datetime.toInstant());
                        pk.setCentroid(centroidVector);

                        final Feature feature = new Feature();

                        feature.setId(pk);
                        feature.setGeometry(GeometryUtil.toByteBuffer(featureModel.getGeometry()));
                        feature.setProperties(featureModel.getPropertiesAsString());
                        feature.setAdditional_attributes(featureModel.getAdditionalAttributesAsString());
                        feature.setIndexed_properties_boolean(propertyUtil.getIndexedBooleanProps());
                        feature.setIndexed_properties_double(propertyUtil.getIndexedNumberProps());
                        feature.setIndexed_properties_text(propertyUtil.getIndexedTextProps());
                        feature.setIndexed_properties_timestamp(propertyUtil.getIndexedTimestampProps());
                        return feature;
                } catch (JsonProcessingException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                }
        }

        public List<FeatureDto> getFeature(final String partitionid,
                                           final String itemid,
                                           final String label,
                                           final String dateTime) {
                final List<Feature> features;

                if ((label == null || label.isEmpty()) && (dateTime == null || dateTime.isEmpty())) {
                        features = featureDao.findFeatureById(partitionid, itemid);
                } else if (dateTime == null || dateTime.isEmpty()) {
                        features = featureDao.findFeatureByIdAndLabel(partitionid, itemid, label);
                } else {
                        final OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTime);
                        final Instant instantDateTime = offsetDateTime.toInstant();
                        features = featureDao.findFeatureByIdLabelAndDate(partitionid, itemid, label, instantDateTime);
                }
                if (features == null || features.isEmpty() || features.size() < 1) {
                        throw new RuntimeException("No data found");
                }

                return features.stream().map(this::convertFeatureToDto).collect(Collectors.toList());

        }

        public FeatureCollection getFeatureId(final String item_id) {
                final FeatureCollection featureCollection = featureCollectionDao.findById(item_id)
                        .orElseThrow(() -> new RuntimeException("No data found for selected item id"));
                return featureCollection;
        }

        private FeatureCollection createFeatureCollection(final Feature ft) {
                final String item_id = ft.getId().getItem_id();
                final String partition_id = ft.getId().getPartition_id();
                final String properties = ft.getProperties();
                final String additional_attributes = ft.getAdditional_attributes();
                final FeatureCollection featureCollection = new FeatureCollection();
                featureCollection.setItem_id(item_id);
                featureCollection.setPartition_id(partition_id);
                featureCollection.setProperties(properties);
                featureCollection.setAdditional_attributes(additional_attributes);
                return featureCollection;

        }

        private FeatureDto convertFeatureToDto(final Feature feature) {
                return FeatureDto.builder()
                        .id(feature.getId().getItem_id())
                        .partition_id(feature.getId().getPartition_id())
                        .label(feature.getId().getLabel())
                        .additional_attributes(feature.getAdditional_attributes())
                        .build();
        }

	private Feature convertFeatureToDao(FeatureDto dto)  {
		final Feature feature = new Feature();

                final int geoResolution = 6;
                final GeoTimePartition.TimeResolution timeResolution = GeoTimePartition.TimeResolution.valueOf("MONTH");
                final GeoTimePartition partitioner = new GeoTimePartition(geoResolution, timeResolution);
        
                final Map<String, Object> properties = dto.getProperties();
                if (properties == null || properties.size() < 1 || properties.isEmpty()) {
                    throw new RuntimeException("There are no properties set.");
                }
                final GeometryDto geometryDto = dto.getGeometry();
                if (geometryDto == null) {
                    throw new RuntimeException("There are no Geomentry set.");
                }
                final String dateTime = (String) (properties.containsKey("datetime") ? properties.get("datetime") : properties.get("start_datetime"));
                final Instant datetime = Instant.parse(dateTime);
                final OffsetDateTime offDatetime = OffsetDateTime.parse(dateTime) ;

                if (datetime == null) {
                    throw new RuntimeException("No date time is set");
                }

                final Geometry geometry;
                try {
                    geometry = GeometryUtil.createGeometryFromDto(geometryDto);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e.getLocalizedMessage());
                }

                final Point centroid = geometry.getCentroid();
                CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));

                String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, offDatetime);
                final String id = dto.getId();
        
                final FeaturePrimaryKey pk = new FeaturePrimaryKey();
                pk.setItem_id(id);
                pk.setPartition_id(partitionId);
                pk.setDatetime(datetime);
                pk.setCentroid(centroidVector);
                pk.setLabel(dto.getLabel());
                feature.setId(pk);

                feature.setGeometry(GeometryUtil.toByteBuffer(geometry));

                feature.setAdditional_attributes(dto.getAdditional_attributes());
                final String propertiesText;
                try {
                    propertiesText = new ObjectMapper().writeValueAsString(dto.getProperties());
                } catch (Exception ex) {
                    throw new RuntimeException(ex.getLocalizedMessage());
                }
                feature.setProperties(propertiesText);

                return feature;
	}
}
