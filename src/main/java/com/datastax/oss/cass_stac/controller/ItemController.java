package com.datastax.oss.cass_stac.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.datastax.oss.cass_stac.dto.FeatureDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.oss.cass_stac.dto.ItemDto;
import com.datastax.oss.cass_stac.service.ItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/item")
@Tag(name="Item", description="The STAC Item to insert and get")
@Schema(hidden = true)
public class ItemController {
	
	private final ItemService itemService;
	
	
	@Operation(description="POST method to store Feature data")
	@PostMapping
	public ResponseEntity<?> addItem(@RequestBody final ItemDto dto) {
		
		final Map<String, String> message = new HashMap<>();
		
		try {
			message.put("message", "Item Added Suucessful");
			itemService.add(dto);
			return new ResponseEntity<>(message, HttpStatus.OK);
		} catch (Exception ex) {
			message.put("message", ex.getLocalizedMessage());
			return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@Operation(description="Get method to fetch Item data based on Partition id and ID")
	@GetMapping
	public ResponseEntity<?> getItem(@RequestParam final String partitionid, @RequestParam(required = false) final String id) {
		try {
			final List<ItemDto> dto = itemService.getItem(partitionid, id);
			final Map<String, List<ItemDto>> message = new HashMap<>();
			message.put("item", dto);
			return new ResponseEntity<>(dto, HttpStatus.OK);
		} catch (Exception ex) {
			final Map<String, String> message = new HashMap<>();
			message.put("message", ex.getLocalizedMessage());
			return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
