package com.kosa.fitlog.routine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.service.RoutineExerciseService;
import com.kosa.fitlog.routine.service.RoutineService;

@WebMvcTest(RoutineController.class)
class RoutineExerciseSelectionViewTests {
    @Autowired private MockMvc mvc;
    @MockBean private RoutineService routineService;
    @MockBean private RoutineExerciseService exerciseService;
    private MockHttpSession session;

    @BeforeEach
    void setup() {
        session = new MockHttpSession();
        session.setAttribute("loginMember", new LoginMember(10L, "test10", "테스트 회원"));
        RoutineDTO routine = new RoutineDTO();
        routine.setRoutineId(7L); routine.setRoutineName("상체 루틴");
        when(routineService.getRoutine(7L, 10L)).thenReturn(routine);
        when(exerciseService.getAllExercises()).thenReturn(Arrays.asList(
                exercise(1L, "벤치프레스", "가슴"), exercise(2L, "랫풀다운", "등"),
                exercise(3L, "긴운동이름ShoulderPressWithoutSpaces".repeat(2), "어깨")));
    }

    @Test
    void existingExerciseIsDisabledAndNewMemosAreKeyedById() throws Exception {
        when(exerciseService.getRoutineExercises(7L)).thenReturn(Collections.singletonList(existing(1L)));
        String html = render("selection");
        assertThat(html).containsPattern("id=\"exercise-select-1\"[^>]*disabled=\"disabled\"");
        assertThat(html).contains("추가됨", "name=\"memos[2]\"", "name=\"memos[3]\"");
        assertThat(html).doesNotContain("name=\"memos[1]\"", "name=\"memo\"");
        assertThat(html.split("action=\"/routine/exercise/add\"", -1)).hasSize(2);
        assertThat(html).contains("id=\"routineExerciseAdd\"", "method=\"post\"", "name=\"routineId\" value=\"7\"");
        assertThat(html).containsPattern("id=\"addSelectedExercises\"[^>]*disabled");
    }

    @Test
    void allExistingExercisesHaveNoEditableMemos() throws Exception {
        when(exerciseService.getRoutineExercises(7L)).thenReturn(Arrays.asList(existing(1L), existing(2L), existing(3L)));
        String html = render("all-added");
        assertThat(html).doesNotContain("name=\"memos[");
        assertThat(html.split("disabled=\"disabled\"", -1)).hasSize(4);
    }

    @Test
    void emptyCatalogKeepsEmptyStateWithoutBulkForm() throws Exception {
        when(exerciseService.getAllExercises()).thenReturn(Collections.emptyList());
        String html = render("empty");
        assertThat(html).contains("추가할 운동이 없습니다.");
        assertThat(html).doesNotContain("id=\"routineExerciseAdd\"", "id=\"addSelectedExercises\"");
    }

    private String render(String name) throws Exception {
        String html = mvc.perform(get("/routine/exercise/add").param("routineId", "7").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        if (Boolean.getBoolean("routine.ui.preview")) {
            Files.createDirectories(Path.of("target/routine-preview"));
            Files.writeString(Path.of("target/routine-preview", name + ".html"), html, StandardCharsets.UTF_8);
        }
        return html.replaceAll("\\s+", " ");
    }

    private ExerciseDTO exercise(Long id, String name, String bodyPart) {
        ExerciseDTO exercise = new ExerciseDTO();
        exercise.setExerciseId(id); exercise.setExerciseName(name); exercise.setBodyPart(bodyPart);
        exercise.setDescription("자세를 확인하며 수행하는 운동입니다.");
        return exercise;
    }

    private RoutineExerciseDTO existing(Long id) {
        RoutineExerciseDTO exercise = new RoutineExerciseDTO();
        exercise.setExerciseId(id);
        return exercise;
    }
}
