package com.datastax.oss.cass_stac.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datastax.oss.cass_stac.dto.ItemDto;

@RestController
@RequestMapping("/item")
public class ItemController {
	
	@PostMapping
	public ResponseEntity<?> addItem(@RequestBody final ItemDto dto) {
		
		final Map<String, String> message = new HashMap<>();
		try {
			message.put("message", "Item Added Suucessful");
			return new ResponseEntity<>(message, HttpStatus.OK);
		} catch (Exception ex) {
			message.put("message", ex.getLocalizedMessage());
			return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}