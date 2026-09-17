package com.kosa.fitlog.workout.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.service.WorkoutService;

@ExtendWith(MockitoExtension.class)
class WorkoutControllerTests {

    @Mock private WorkoutService workoutService;
    private MockMvc mockMvc;
    private MockHttpSession loginSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WorkoutController(workoutService))
                .build();
        loginSession = new MockHttpSession();
        loginSession.setAttribute(
                "loginMember", new LoginMember(10L, "member10", "회원10"));
    }

    @Test
    void startUsesLoginMemberIdAndRedirectsToCreatedWorkout() throws Exception {
        when(workoutService.startWorkout(10L, 7L)).thenReturn(100L);

        mockMvc.perform(post("/workout/start")
                .session(loginSession)
                .param("memberId", "999")
                .param("routineId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workout/record?workoutLogId=100"));

        verify(workoutService).startWorkout(10L, 7L);
    }

    @Test
    void anotherMembersOrMissingRoutineCannotStart() throws Exception {
        when(workoutService.startWorkout(10L, 99L)).thenReturn(null);

        mockMvc.perform(post("/workout/start")
                .session(loginSession)
                .param("routineId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"));
    }

    @Test
    void emptyRoutineReturnsToDetailWithMessage() throws Exception {
        when(workoutService.startWorkout(10L, 7L))
                .thenThrow(new IllegalArgumentException(
                        "운동이 등록되지 않은 루틴입니다."));

        mockMvc.perform(post("/workout/start")
                .session(loginSession)
                .param("routineId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/read?routineId=7"))
                .andExpect(flash().attribute(
                        "startError", "운동이 등록되지 않은 루틴입니다."));
    }

    @Test
    void ownWorkoutRecordProvidesLogAndJoinedExercises() throws Exception {
        WorkoutLogDTO log = new WorkoutLogDTO();
        log.setWorkoutLogId(100L);
        when(workoutService.getWorkoutLog(100L, 10L)).thenReturn(log);
        when(workoutService.getWorkoutExercises(100L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/workout/record")
                .session(loginSession)
                .param("workoutLogId", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("workout/record"))
                .andExpect(model().attribute("workoutLog", log))
                .andExpect(model().attributeExists("workoutExercises"));

        verify(workoutService).getWorkoutLog(100L, 10L);
        verify(workoutService).getWorkoutExercises(100L);
    }

    @Test
    void anotherMembersWorkoutRecordIsNotExposed() throws Exception {
        when(workoutService.getWorkoutLog(999L, 10L)).thenReturn(null);

        mockMvc.perform(get("/workout/record")
                .session(loginSession)
                .param("workoutLogId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"))
                .andExpect(model().attributeDoesNotExist("workoutLog"));

        verify(workoutService, never()).getWorkoutExercises(any());
    }

    @Test
    void unauthenticatedWorkoutEndpointsRedirectWithoutCreatingSession()
            throws Exception {
        MvcResult start = mockMvc.perform(post("/workout/start")
                .param("routineId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult record = mockMvc.perform(get("/workout/record")
                .param("workoutLogId", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();

        assertThat(start.getRequest().getSession(false)).isNull();
        assertThat(record.getRequest().getSession(false)).isNull();
        verify(workoutService, never()).startWorkout(any(), any());
        verify(workoutService, never()).getWorkoutLog(any(), any());
    }
}
