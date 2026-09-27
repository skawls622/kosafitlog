package com.kosa.fitlog.exercise.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.kosa.fitlog.api.dto.ExerciseApiDTO;
import com.kosa.fitlog.api.service.DeepLTranslationService;
import com.kosa.fitlog.api.service.ExerciseApiService;
import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.service.ExerciseService;
import com.kosa.fitlog.exercise.view.ExerciseDisplayLabels;
import com.kosa.fitlog.exercise.view.ExerciseInstructionSection;

@Controller
public class ExerciseInfoController {

    private final ExerciseService exerciseService;
    private final ExerciseApiService exerciseApiService;
    private final DeepLTranslationService deepLTranslationService;

    public ExerciseInfoController(
            ExerciseService exerciseService,
            ExerciseApiService exerciseApiService,
            DeepLTranslationService deepLTranslationService) {

        this.exerciseService = exerciseService;
        this.exerciseApiService = exerciseApiService;
        this.deepLTranslationService = deepLTranslationService;
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

            if (apiExercises != null && !apiExercises.isEmpty()) {
                apiExercise = apiExercises.get(0);
            }
        }

        // 3. 원본 API 데이터와 화면 표시값을 분리한다. 번역은 요청당 한 번만 수행한다.
        if (apiExercise != null) {
            addTranslatedGuidance(apiExercise, model);
            model.addAttribute("displayDifficulty", ExerciseDisplayLabels.difficulty(apiExercise.getDifficulty()));
            model.addAttribute("displayType", ExerciseDisplayLabels.type(apiExercise.getType()));
            model.addAttribute("displayMuscle", ExerciseDisplayLabels.muscle(apiExercise.getMuscle()));
            model.addAttribute("displayEquipments", ExerciseDisplayLabels.equipments(apiExercise.getEquipments()));
        }

        // 4. 기존 로컬 운동 정보와 API 원본도 유지한다.
        model.addAttribute("exercise", exercise);
        model.addAttribute("apiExercise", apiExercise);

        return "exercise/info";
    }

    private void addTranslatedGuidance(ExerciseApiDTO apiExercise, Model model) {
        // 번역 후에는 구분자 표현이 달라질 수 있으므로 반드시 영어 원문에서 먼저 나눈다.
        List<ExerciseInstructionSection> sections =
                ExerciseInstructionSection.split(apiExercise.getInstructions());
        List<String> texts = new ArrayList<>();
        for (ExerciseInstructionSection section : sections) {
            texts.add(section.getText());
        }
        texts.add(apiExercise.getSafetyInfo());
        List<String> translated = deepLTranslationService.translateAllToKorean(texts);

        List<ExerciseInstructionSection> displaySections = new ArrayList<>();
        StringJoiner instructions = new StringJoiner("\n\n");
        for (int i = 0; i < sections.size(); i++) {
            String title = sections.get(i).getTitle();
            String text = translated.get(i);
            displaySections.add(new ExerciseInstructionSection(title, text));
            instructions.add(title == null ? text : title + ":\n" + text);
        }
        model.addAttribute("instructionSections", displaySections);
        // 기존 Model 속성도 유지한다. 화면은 문단 목록을 사용한다.
        model.addAttribute("translatedInstructions",
                sections.isEmpty() ? apiExercise.getInstructions() : instructions.toString());
        model.addAttribute("translatedSafetyInfo", translated.get(sections.size()));
    }
}
