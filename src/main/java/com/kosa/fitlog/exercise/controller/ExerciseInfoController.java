package com.kosa.fitlog.exercise.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kosa.fitlog.api.dto.ExerciseApiDTO;
import com.kosa.fitlog.api.service.ExerciseApiService;
import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.service.ExerciseService;

@Controller
public class ExerciseInfoController {

    private final ExerciseService exerciseService;
    private final ExerciseApiService exerciseApiService;

    public ExerciseInfoController(
            ExerciseService exerciseService,
            ExerciseApiService exerciseApiService) {

        this.exerciseService = exerciseService;
        this.exerciseApiService = exerciseApiService;
    }

    @GetMapping("/exercise/info")
    public String info(
            @RequestParam("exerciseId") Long exerciseId,
            Model model) {

        // 1. 우리 DB 운동 조회
        ExerciseDTO exercise =
                exerciseService.findById(exerciseId);

        if (exercise == null) {
            return "redirect:/routine/list";
        }

        // 2. API 결과 중 대표 운동 1개 선택
        ExerciseApiDTO apiExercise = null;

        if (exercise.getApiKeyword() != null) {

            List<ExerciseApiDTO> apiExercises =
                    exerciseApiService.searchByName(
                            exercise.getApiKeyword());

            if (!apiExercises.isEmpty()) {
                apiExercise = apiExercises.get(0);
            }
        }

        // 3. 화면으로 전달
        model.addAttribute("exercise", exercise);
        model.addAttribute("apiExercise", apiExercise);

        return "exercise/info";
    }
}