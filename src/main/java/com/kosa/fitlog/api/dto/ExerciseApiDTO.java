package com.kosa.fitlog.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExerciseApiDTO {
	
	private String name;
    private String type;
    private String muscle;
    private String difficulty;
    private String instructions;
    private List<String> equipments;
    
    @JsonProperty("safety_info")
    private String safetyInfo;
}
