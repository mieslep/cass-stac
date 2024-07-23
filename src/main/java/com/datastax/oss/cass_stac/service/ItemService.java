package com.datastax.oss.cass_stac.service;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.datastax.oss.cass_stac.model.ItemModelResponse;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import com.datastax.oss.cass_stac.dao.GeoTimePartition;
import com.datastax.oss.cass_stac.dao.ItemDao;
import com.datastax.oss.cass_stac.dao.ItemIdDao;
import com.datastax.oss.cass_stac.dto.GeometryDto;
import com.datastax.oss.cass_stac.dto.ItemDto;
import com.datastax.oss.cass_stac.entity.Item;
import com.datastax.oss.cass_stac.entity.ItemId;
import com.datastax.oss.cass_stac.entity.ItemPrimaryKey;
import com.datastax.oss.cass_stac.model.ItemModelRequest;
import com.datastax.oss.cass_stac.util.GeometryUtil;
import com.datastax.oss.cass_stac.util.PropertyUtil;
import com.datastax.oss.driver.api.core.data.CqlVector;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemDao itemDao;
    private final ItemIdDao itemIdDao;

    private static final Map<String, String> propertyIndexMap = PropertyUtil.getPropertyMap("dao.item.property.IndexList");

    public ItemModelResponse getItemById(final String id) {
        final ItemId itemId = itemIdDao.findById(id)
                .orElseThrow(() -> new RuntimeException(id + " is not found"));
        final String partitionId = itemId.getPartition_id();
        final ItemPrimaryKey pk = new ItemPrimaryKey();
        pk.setPartition_id(partitionId);
        pk.setId(id);
        final Item item = itemDao.findById(pk)
                .orElseThrow(() -> new RuntimeException("There are no item found for the " + id));
        final String collection = item.getCollection();
        final ByteBuffer geometryByteBuffer = item.getGeometry();
        final Geometry geometry = GeometryUtil.fromGeometryByteBuffer(geometryByteBuffer);

        final String propertiesString = item.getProperties();
        final String additionalAttributesString = item.getAdditional_attributes();

        try {
            return new ItemModelResponse((String) id, collection, geometry.toString(), propertiesString, additionalAttributesString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    public void save(final String json) {
        final Item item = converItemJsonToItem(json);
        final Item it = itemDao.save(item);
        final ItemId itemId = createItemId(it);
        itemIdDao.save(itemId);
    }

    private Item converItemJsonToItem(final String json) {
        final ObjectMapper objectMapper = new ObjectMapper();
        try {

            final int geoResolution = 6;
            final GeoTimePartition.TimeResolution timeResolution = GeoTimePartition.TimeResolution.valueOf("MONTH");
            final GeoTimePartition partitioner = new GeoTimePartition(geoResolution, timeResolution);

            final ItemModelRequest itemModel = objectMapper.readValue(json, ItemModelRequest.class);
            final PropertyUtil propertyUtil = new PropertyUtil(propertyIndexMap, itemModel);
            Point centroid = itemModel.getGeometry().getCentroid();
            CqlVector<Float> centroidVector = CqlVector.newInstance(Arrays.asList((float) centroid.getY(), (float) centroid.getX()));

            OffsetDateTime datetime = (OffsetDateTime) (itemModel.getProperties().containsKey("datetime") ? itemModel.getProperties().get("datetime") : itemModel.getProperties().get("start_datetime"));
            String partitionId = partitioner.getGeoTimePartitionForPoint(centroid, datetime);
            String id = itemModel.getId();
            final ItemPrimaryKey pk = new ItemPrimaryKey();

            pk.setId(id);
            pk.setPartition_id(partitionId);

            final Item item = new Item();
            item.setId(pk);
            item.setCollection(itemModel.getCollection());
            item.setGeometry(GeometryUtil.toByteBuffer(itemModel.getGeometry()));
            item.setDatetime(datetime.toInstant());
            item.setCentroid(centroidVector);
            item.setProperties(itemModel.getPropertiesAsString());
            item.setAdditional_attributes(itemModel.getAdditionalAttributesAsString());
            item.setIndexed_properties_boolean(propertyUtil.getIndexedBooleanProps());
            item.setIndexed_properties_double(propertyUtil.getIndexedNumberProps());
            item.setIndexed_properties_text(propertyUtil.getIndexedTextProps());
            item.setIndexed_properties_timestamp(propertyUtil.getIndexedTimestampProps());
            return item;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    public ItemDto getItem(final String id) {
        final ItemId itemId = getItemId(id);

        final ItemPrimaryKey itemPrimaryKey = new ItemPrimaryKey();
        itemPrimaryKey.setId(id);
        itemPrimaryKey.setPartition_id(itemId.getPartition_id());
        final Item item = itemDao.findById(itemPrimaryKey)
                .orElseThrow(() -> new RuntimeException("No data found"));
        final ItemDto itemDto = convertItemToDto(item);
        return itemDto;
    }

    public ItemId getItemId(final String id) {
        final ItemId itemId = itemIdDao.findById(id)
                .orElseThrow(() -> new RuntimeException("No data found for selected id"));
        return itemId;
    }

    private ItemId createItemId(final Item it) {
        final String id = it.getId().getId();
        final Instant datetime = it.getDatetime();
        final String partition_id = it.getId().getPartition_id();
        final ItemId itemId = new ItemId();
        itemId.setDatetime(datetime);
        itemId.setId(id);
        itemId.setPartition_id(partition_id);
        return itemId;
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

}