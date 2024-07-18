package com.datastax.oss.cass_stac.controller;

import com.datastax.oss.cass_stac.dto.FeatureDto;
import com.datastax.oss.cass_stac.service.FeatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feature")
public class FeatureController {
	private final FeatureService featureService;
	
	@PostMapping
	public ResponseEntity<?> addFeature(@RequestBody final FeatureDto dto) {
		
		final Map<String, String> message = new HashMap<>();
		
		try {
			message.put("message", "Feature Added Suucessful");
			featureService.add(dto);
			return new ResponseEntity<>(message, HttpStatus.OK);
		} catch (Exception ex) {
			message.put("message", ex.getLocalizedMessage());
			return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	@GetMapping
	public ResponseEntity<?> getFeature(@RequestBody final FeatureDto dto) {

		final Map<String, String> message = new HashMap<>();

		try {
			message.put("message", "Feature Retrieved Successful");
			featureService.add(dto);
			return new ResponseEntity<>(message, HttpStatus.OK);
		} catch (Exception ex) {
			message.put("message", ex.getLocalizedMessage());
			return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
