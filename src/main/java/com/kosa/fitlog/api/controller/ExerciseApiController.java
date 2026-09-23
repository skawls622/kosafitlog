package com.kosa.fitlog.api.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.kosa.fitlog.api.dto.ExerciseApiDTO;
import com.kosa.fitlog.api.service.ExerciseApiService;
import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.service.ExerciseService;

@RestController

public class ExerciseApiController {
	
	private final ExerciseApiService exerciseApiService;
	private final ExerciseService exerciseService;
	
	public ExerciseApiController(ExerciseApiService exerciseApiService, ExerciseService exerciseService) {
		this.exerciseApiService = exerciseApiService;
		this.exerciseService = exerciseService;
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
	
	
	@GetMapping(value = "/api/exercises",params= "exerciseId")
	public List<ExerciseApiDTO> searchByExerciseId(
			@RequestParam("exerciseId") Long exerciseId){
		
		ExerciseDTO exercise = 
				exerciseService.findById(exerciseId);
		
		if (exercise == null || exercise.getApiKeyword() == null) {
			return Collections.emptyList();
		}
		
		return exerciseApiService.searchByName(
				exercise.getApiKeyword());
	}

	
}
