package com.datastax.oss.cass_stac.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import com.datastax.oss.cass_stac.dao.FeatureDao;
import com.datastax.oss.cass_stac.dao.GeoTimePartition;
import com.datastax.oss.cass_stac.dto.FeatureDto;
import com.datastax.oss.cass_stac.dto.GeometryDto;
import com.datastax.oss.cass_stac.entity.Feature;
import com.datastax.oss.cass_stac.entity.FeaturePrimaryKey;
import com.datastax.oss.cass_stac.util.GeometryUtil;
import com.datastax.oss.cass_stac.util.PropertyUtil;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FeatureService {
	private final FeatureDao featureDao;
	
	public void add(FeatureDto dto) {
		final Feature feature = convertFeatureToDao(dto);
		featureDao.save(feature);
	}
	
	public List<FeatureDto> getFeature(final String partitionid,
            final String itemid,
            final String label,
            final String dateTime) {
		
		final OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTime);
		
		//final long offsetEpohs = offsetDateTime.toInstant();
		//final Instant instantDateTime = Instant.ofEpochMilli(offsetEpohs);
		final Instant instantDateTime = offsetDateTime.toInstant();
		
		final List<Feature> features = featureDao.FindFeatureByIdLabelAndDate(partitionid, itemid, label, instantDateTime);
		if (features == null || features.isEmpty() || features.size() < 1) {
			throw new RuntimeException("No data found");
		}
		
		return features.stream().map(this::convertFeatureToDto).collect(Collectors.toList());
		
	}
	

        public FeatureDto getFeature(final String partitionid,
                                        final String itemid,
                                        final String label,
                                        final String dateTime,
                                        final Double latitude,
                                        final Double longitude) {
        	
        		final OffsetDateTime offsetDateTime = OffsetDateTime.parse(dateTime);
        		
        		final Instant instantDateTime = offsetDateTime.toInstant();
        		
                final FeaturePrimaryKey featurePrimaryKey = new FeaturePrimaryKey();
                
                final List<Double[]> coordinates = new ArrayList<Double[]>();
                Double[] latlangs = new Double[2];
                latlangs[0] = latitude;
                latlangs[1] = longitude;
                coordinates.add(latlangs);
                latlangs = new Double[2];
                latlangs[0] = -35.10115;
                latlangs[1] = 144.54592;
                coordinates.add(latlangs);
                final GeometryDto geometryDto = GeometryDto.builder()
                									.type("Polygon")
                									.coordinates(coordinates)
                									.build();
                final Geometry geometry;
                try {
                    geometry = GeometryUtil.createGeometryFromDto(geometryDto);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException(e.getLocalizedMessage());
                }

                final Point centroid = geometry.getCentroid();
                CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));
                
                featurePrimaryKey.setItem_id(itemid);
                featurePrimaryKey.setPartition_id(partitionid);
                featurePrimaryKey.setLabel(label);
                featurePrimaryKey.setDatetime(Instant.parse(dateTime));
                featurePrimaryKey.setCentroid(centroidVector);

                final Feature feature = featureDao.findById(featurePrimaryKey)
                        .orElseThrow(() -> new RuntimeException("No data found"));
                final FeatureDto featureDto = convertFeatureToDto(feature);
                return featureDto;
        }
        
        private FeatureDto convertFeatureToDto(final Feature feature) {
                return FeatureDto.builder()
                        .id(feature.getId().getItem_id())
                        .partition_id(feature.getId().getPartition_id())
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

                Map<String,Boolean> booleanMap = PropertyUtil.getBooleans(properties);
                Map<String,String> textMap = PropertyUtil.getTexts(properties);
                Map<String,Double> numberMap = PropertyUtil.getNumbers(properties);
                Map<String,OffsetDateTime> datetimeMap = PropertyUtil.getDateTimes(properties);

                feature.setIndexed_properties_boolean(booleanMap);
                feature.setIndexed_properties_double(numberMap);
                feature.setIndexed_properties_text(textMap);
//                feature.setIndexed_properties_timestamp(datetimeMap);

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
                final String label = dto.getLabel();
                pk.setItem_id(id);
                pk.setPartition_id(partitionId);
                pk.setLabel(label);
                pk.setDatetime(datetime);
                pk.setCentroid(centroidVector);

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

//        private Geometry createGeometryFromDto(GeometryDto geometryDto) {
//                List<Coordinate> coordinates = new ArrayList<>();
//                for (Double[] coordinate : geometryDto.getCoordinates()) {
//                        coordinates.add(new Coordinate(coordinate[0], coordinate[1]));
//                }
//                GeometryFactory geometryFactory = new GeometryFactory();
//                return geometryFactory.createPolygon(coordinates.toArray(new Coordinate[0]));
//        }

}
