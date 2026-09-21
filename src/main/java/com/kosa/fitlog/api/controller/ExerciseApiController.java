package com.kosa.fitlog.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kosa.fitlog.api.dto.ExerciseApiDTO;
import com.kosa.fitlog.api.service.ExerciseApiService;

@RestController

public class ExerciseApiController {
	
	private final ExerciseApiService exerciseApiService;
	
	public ExerciseApiController(ExerciseApiService exerciseApiService) {
		this.exerciseApiService = exerciseApiService;
	}
	
	@GetMapping(value = "/api/exercises", params = "muscle")
	public List<ExerciseApiDTO> searchByMuscle(
			@RequestParam String muscle) {
		
		return exerciseApiService.searchByMuscle(muscle);
	}
	
	@GetMapping(value = "/api/exercises", params = "name")
	public List<ExerciseApiDTO> searchByName(
			@RequestParam String name) {
		
		return exerciseApiService.searchByName(name);
	}

	
}
