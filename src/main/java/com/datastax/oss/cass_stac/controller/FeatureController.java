package com.datastax.oss.cass_stac.controller;

import com.datastax.oss.cass_stac.dto.FeatureDto;
import com.datastax.oss.cass_stac.service.FeatureService;

import com.datastax.oss.driver.api.core.data.CqlVector;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/feature")
@Tag(name="Feature", description="The STAC Feature to insert and get")
@Schema(hidden = true)
public class FeatureController {
	private final FeatureService featureService;
	
	@Operation(description="Add Feature")
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

	@Operation(description = "Get Feature by passing the values")
	@GetMapping
	public ResponseEntity<?> getFeature(@RequestParam final String partitionid,
										@RequestParam final String itemid,
										@RequestParam final String label,
										@RequestParam final String datetime,
										@RequestParam final Double latitude,
										@RequestParam final Double longitude) {

		
		try {
			final FeatureDto dto = featureService.getFeature(partitionid, itemid, label, datetime, latitude, longitude);
			return new ResponseEntity<>(dto, HttpStatus.OK);
		} catch (Exception ex) {
			final Map<String, String> message = new HashMap<>();
			message.put("message", ex.getLocalizedMessage());
			return new ResponseEntity<>(message, HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
