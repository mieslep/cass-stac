package com.datastax.oss.cass_stac.service;

import com.datastax.oss.cass_stac.dao.GeoTimePartition;
import com.datastax.oss.cass_stac.dao.FeatureDao;
import com.datastax.oss.cass_stac.dto.FeatureDto;
import com.datastax.oss.cass_stac.entity.Feature;
import com.datastax.oss.cass_stac.entity.FeaturePrimaryKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeatureService {
	private final FeatureDao featureDao;
	
	public void add(FeatureDto dto) {
		final Feature feature = convertFeatureToDao(dto);
		featureDao.save(feature);
	}
	
	private Feature convertFeatureToDao(FeatureDto dto)  {
		final Feature feature = new Feature();
		
        final int geoResolution = 6;
        final GeoTimePartition.TimeResolution timeResolution = GeoTimePartition.TimeResolution.valueOf("MONTH");
        final GeoTimePartition partitioner = new GeoTimePartition(geoResolution, timeResolution);
     
        final Map<String, Object> properties = dto.getProperties();
        final Object dateTime = properties.containsKey("datetime") ? properties.get("datetime") : properties.get("start_datetime");
        final OffsetDateTime datetime = (OffsetDateTime) dateTime;
        
//        final GeometryDto geometryDto = dto.getGeometry();
//        ObjectMapper mapper = new ObjectMapper();
//        final JsonNode jsonNode = mapper.convertValue(geometryDto, JsonNode.class);
//        final Geometry geometry;
//		try {
//			geometry = jsonNode != null ? mapper.treeToValue(jsonNode, Geometry.class) : null;
//		} catch (JsonProcessingException | IllegalArgumentException e) {
//			throw new RuntimeException(e.getLocalizedMessage());
//		}
//
//		final List<Float[][]> coordinates = geometryDto.getCoordinates();
//
//        final Point centroid = geometry.getCentroid();
//
//        final String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, datetime);
        final String id = dto.getId();
        final String partitionId = id + "123456";
        final FeaturePrimaryKey pk = new FeaturePrimaryKey();
        pk.setId(id);
        pk.setPartition_id(partitionId);
    
//        feature.setId(pk);
        
//        CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));
//        feature.setCentroid(centroidVector);
//
//        feature.setGeometry(GeometryUtil.toByteBuffer(geometry));
        
//        feature.setCollection(dto.getCollection());
//        feature.setAdditional_attributes(dto.getAdditional_attributes());
//        feature.setProperties(properties.toString());

		return feature;
	}

}
