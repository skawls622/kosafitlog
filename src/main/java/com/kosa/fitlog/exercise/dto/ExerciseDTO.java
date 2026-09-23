package com.kosa.fitlog.exercise.dto;

import java.time.LocalDateTime;

public class ExerciseDTO {

	private Long exerciseId;
	private String exerciseName;
	private String bodyPart;
	private String description;
	private LocalDateTime createdAt;
	private String apiKeyword;
	
	
	public Long getExerciseId() {
		return exerciseId;
	}

	public void setExerciseId(Long exerciseId) {
		this.exerciseId = exerciseId;
	}

	public String getExerciseName() {
		return exerciseName;
	}

	public void setExerciseName(String exerciseName) {
		this.exerciseName = exerciseName;
	}

	public String getBodyPart() {
		return bodyPart;
	}

	public void setBodyPart(String bodyPart) {
		this.bodyPart = bodyPart;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	public String getApiKeyword() {
		return apiKeyword;
	}
	
	public void setApiKeyword(String apiKeyword) {
		this.apiKeyword = apiKeyword;
	
	}
}
