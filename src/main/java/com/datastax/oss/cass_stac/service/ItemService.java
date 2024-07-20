package com.datastax.oss.cass_stac.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import com.datastax.oss.cass_stac.dao.GeoTimePartition;
import com.datastax.oss.cass_stac.dao.ItemDao;
import com.datastax.oss.cass_stac.dto.GeometryDto;
import com.datastax.oss.cass_stac.dto.ItemDto;
import com.datastax.oss.cass_stac.entity.Item;
import com.datastax.oss.cass_stac.entity.ItemPrimaryKey;
import com.datastax.oss.cass_stac.util.GeometryUtil;
import com.datastax.oss.cass_stac.util.PropertyUtil;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {
	private final ItemDao itemDao;
	
	public void add(ItemDto dto) {
		final Item item = convertItemToDao(dto);
		itemDao.save(item);	
		
	}

    public ItemDto getItem(final String partitionid, final String id) {
            final ItemPrimaryKey itemPrimaryKey = new ItemPrimaryKey();
            itemPrimaryKey.setId(id);
            itemPrimaryKey.setPartition_id(partitionid);
            final Item item = itemDao.findById(itemPrimaryKey)
                                    .orElseThrow(() -> new RuntimeException("No data found"));
            final ItemDto itemDto = convertItemToDto(item);
            return itemDto;
    }
    private ItemDto convertItemToDto(final Item item) {
            return ItemDto.builder()
                    .id(item.getId().getId())
                    .partition_id(item.getId().getPartition_id())
                    .collection(item.getCollection())
                    .additional_attributes(item.getAdditional_attributes())
                    //.properties(item.getProperties())
                    .build();
    }
	
	private Item convertItemToDao(ItemDto dto)  {
                final Item item = new Item();
                        
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
                final OffsetDateTime offDatetime = (OffsetDateTime.parse(dateTime)) ;
                
                if (datetime == null) {
                	throw new RuntimeException("No date time is set");
                }
                
                item.setDatetime(datetime);

                Map<String,Boolean> booleanMap = PropertyUtil.getBooleans(properties);
                Map<String,String> textMap = PropertyUtil.getTexts(properties);
                Map<String,Double> numberMap = PropertyUtil.getNumbers(properties);
                Map<String,OffsetDateTime> datetimeMap = PropertyUtil.getDateTimes(properties);
                
                item.setIndexed_properties_boolean(booleanMap);
                item.setIndexed_properties_double(numberMap);
                item.setIndexed_properties_text(textMap);
                item.setIndexed_properties_timestamp(datetimeMap);
        
                final Geometry geometry;
                try {
                        geometry = GeometryUtil.createGeometryFromDto(geometryDto);
                } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e.getLocalizedMessage());
                }
                
                final Point centroid = geometry.getCentroid();
                CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));
                item.setCentroid(centroidVector);

                String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, offDatetime);
                final String id = dto.getId();

                final ItemPrimaryKey pk = new ItemPrimaryKey();
                pk.setId(id);
                pk.setPartition_id(partitionId);
                item.setId(pk);

                item.setGeometry(GeometryUtil.toByteBuffer(geometry));
                
                item.setCollection(dto.getCollection());
                item.setAdditional_attributes(dto.getAdditional_attributes());
                final String propertiesText;
                try {
                        propertiesText = new ObjectMapper().writeValueAsString(dto.getProperties());
                } catch (Exception ex) {
                        throw new RuntimeException(ex.getLocalizedMessage());
                }
                item.setProperties(propertiesText);
                return item;
        }

}
