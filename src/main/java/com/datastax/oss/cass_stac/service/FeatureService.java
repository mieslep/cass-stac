package com.datastax.oss.cass_stac.service;

import com.datastax.oss.cass_stac.dto.ItemDto;
import com.datastax.oss.cass_stac.entity.Item;
import com.datastax.oss.cass_stac.entity.ItemPrimaryKey;
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

import java.time.Instant;
import java.util.ArrayList;
import com.datastax.oss.driver.api.core.data.CqlVector;
import org.springframework.web.bind.annotation.RequestParam;

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

        public FeatureDto getFeature(final String partitionid,
                                        final String itemid,
                                        final String label,
                                        final String dateTime,
                                        final Double latitude,
                                        final Double longitude) {

                final FeaturePrimaryKey featurePrimaryKey = new FeaturePrimaryKey();

                final CqlVector<Float> centroidVector = CqlVector.newInstance(longitude.floatValue(),latitude.floatValue());
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
                final String dateTime = (String) (properties.containsKey("datetime") ? properties.get("datetime") : properties.get("start_datetime"));
                final Instant datetime = Instant.parse(dateTime);
                final OffsetDateTime offDatetime = OffsetDateTime.parse(dateTime) ;

                final GeometryDto geometryDto = dto.getGeometry();
                final Geometry geometry;
                try {
                        geometry = createGeometryFromDto(geometryDto);
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                }


                final Point centroid = geometry.getCentroid();

                final String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, offDatetime);
                final String id = dto.getId();
        
                final FeaturePrimaryKey pk = new FeaturePrimaryKey();
                final String label = dto.getLabel();
                pk.setItem_id(id);
                pk.setPartition_id(partitionId);
                pk.setLabel(label);
                pk.setDatetime(datetime);
                CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));
                pk.setCentroid(centroidVector);

                feature.setId(pk);

                feature.setGeometry(GeometryUtil.toByteBuffer(geometry));
                
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
