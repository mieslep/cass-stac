package com.datastax.oss.cass_stac.service;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Coordinates;
import org.locationtech.jts.geom.*;

import com.datastax.oss.cass_stac.dao.GeoTimePartition;
import com.datastax.oss.cass_stac.dao.FeatureDao;
import com.datastax.oss.cass_stac.dto.FeatureDto;
import com.datastax.oss.cass_stac.dto.GeometryDto;
import com.datastax.oss.cass_stac.entity.Feature;
import com.datastax.oss.cass_stac.entity.FeaturePrimaryKey;
import com.datastax.oss.cass_stac.util.GeometryUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.datastax.oss.driver.api.core.data.CqlVector;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.Arrays;

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
        
        final GeometryDto geometryDto = dto.getGeometry();
        final Geometry geometry;
		try {
			geometry = createGeometryFromDto(geometryDto);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e.getLocalizedMessage());
		}

		final java.util.List coordinates = geometryDto.getCoordinates();

        final Point centroid = geometry.getCentroid();

        final String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, datetime);
        final String id = dto.getId();
        //final String partitionId = id + "123456";
        final FeaturePrimaryKey pk = new FeaturePrimaryKey();
        final String label = "abc";
        //final Point centroid = geometry.getCentroid();
        pk.setItem_id(id);
        pk.setPartition_id(partitionId);
        pk.setLabel(label);
        pk.setDatetime(datetime);
        //pk.setCentroid(centroid);
    
        feature.setId(pk);
        
        CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));
        feature.setCentroid(centroidVector);

        feature.setGeometry(GeometryUtil.toByteBuffer(geometry));
        
        //feature.setCollection(dto.getCollection());
        feature.setAdditional_attributes(dto.getAdditional_attributes());
        feature.setProperties(properties.toString());

		return feature;
	}

        private Geometry createGeometryFromDto(GeometryDto geometryDto) {
                List<Coordinate> coordinates = new ArrayList<>();
                for (Double[] coordinate : geometryDto.getCoordinates()) {
                        coordinates.add(new Coordinate(coordinate[0], coordinate[1]));
                }
                GeometryFactory geometryFactory = new GeometryFactory();
                return geometryFactory.createPolygon(coordinates.toArray(new Coordinate[0]));
        }

}
