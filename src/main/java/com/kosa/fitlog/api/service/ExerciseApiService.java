package com.kosa.fitlog.api.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.kosa.fitlog.api.dto.ExerciseApiDTO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;


@Service
public class ExerciseApiService {
	
	@Value("${api.ninjas.key}")
	private String apiKey;
	
	public List<ExerciseApiDTO> searchByMuscle(String muscle){
		
		HttpHeaders headers = new HttpHeaders();
		headers.set("X-Api-Key", apiKey);
		
		HttpEntity<String> entity = new HttpEntity<>(headers);
		
		RestTemplate restTemplate  = new RestTemplate();
		
		String url = "https://api.api-ninjas.com/v1/exercises?muscle=" + muscle;
		
		ResponseEntity<ExerciseApiDTO[]>response = 
				restTemplate.exchange(
						url,
						HttpMethod.GET,
						entity,
						ExerciseApiDTO[].class
						);
		
		ExerciseApiDTO[] body = response.getBody();
		if (body == null) {
		    return Collections.emptyList();
		}
		
		return Arrays.asList(body);
		
	}
	
	

}
