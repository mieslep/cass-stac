package com.datastax.oss.cass_stac.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Service;

import com.datastax.oss.cass_stac.dao.ItemDao;
import com.datastax.oss.cass_stac.dto.ItemDto;
import com.datastax.oss.cass_stac.entity.Item;
import com.datastax.oss.cass_stac.entity.ItemPrimaryKey;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
	private final ItemDao itemDao;
	
	public void add(ItemDto dto) {
		log.debug("Calling add");
		final Item item = convertItemToDao(dto);
		itemDao.save(item);
		
	}
	
	private Item convertItemToDao(ItemDto dto) {
		final Item item = new Item();
		final String id = dto.getId();
        final LocalDateTime dt = LocalDateTime.now();
        final Long ms = dt.toEpochSecond(ZoneOffset.UTC);
		final String partionid = id + ms;
		final ItemPrimaryKey pk = new ItemPrimaryKey();
		pk.setId(id);
		pk.setPartition_id(partionid);
        item.setId(pk);
		item.setCollection(dto.getCollection());
		final Map<String,Object> properties = dto.getProperties();
		final Object strDateTime = properties.containsKey("datetime") ? properties.get("datetime") : properties.get("start_datetime");
		OffsetDateTime datetime = (OffsetDateTime) strDateTime;
		item.setDatetime(null);
		
		return item;
	}

}
