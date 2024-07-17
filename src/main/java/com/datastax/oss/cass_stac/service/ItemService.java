package com.datastax.oss.cass_stac.service;

import org.springframework.stereotype.Service;

import com.datastax.oss.cass_stac.dao.ItemDao;
import com.datastax.oss.cass_stac.dto.ItemDto;
import com.datastax.oss.cass_stac.entity.Item;

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
		item.setCollection(dto.getCollection());
		return item;
	}
}
