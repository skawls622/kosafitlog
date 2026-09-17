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

import java.math.BigDecimal;
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
import com.kosa.fitlog.workout.service.WorkoutSetService;

@ExtendWith(MockitoExtension.class)
class WorkoutControllerTests {

    @Mock private WorkoutService workoutService;
    @Mock private WorkoutSetService workoutSetService;
    private MockMvc mockMvc;
    private MockHttpSession loginSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WorkoutController(
                        workoutService, workoutSetService))
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
    void ownWorkoutRecordProvidesLogExercisesAndSetMap() throws Exception {
        WorkoutLogDTO log = new WorkoutLogDTO();
        log.setWorkoutLogId(100L);
        when(workoutService.getWorkoutLog(100L, 10L)).thenReturn(log);
        when(workoutService.getWorkoutExercises(100L))
                .thenReturn(Collections.emptyList());
        when(workoutSetService.getSetsByWorkoutExercises(Collections.emptyList()))
                .thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/workout/record")
                .session(loginSession)
                .param("workoutLogId", "100"))
                .andExpect(status().isOk())
                .andExpect(view().name("workout/record"))
                .andExpect(model().attribute("workoutLog", log))
                .andExpect(model().attributeExists("workoutExercises"))
                .andExpect(model().attributeExists("workoutSetsByExerciseId"));

        verify(workoutService).getWorkoutLog(100L, 10L);
        verify(workoutSetService)
                .getSetsByWorkoutExercises(Collections.emptyList());
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
        verify(workoutSetService, never()).getSetsByWorkoutExercises(any());
    }

    @Test
    void completeUsesSessionMemberIdAndRedirectsToWorkoutList() throws Exception {
        when(workoutService.completeWorkout(100L, 10L)).thenReturn(true);

        mockMvc.perform(post("/workout/complete")
                .session(loginSession)
                .param("memberId", "999")
                .param("workoutLogId", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workout/list"));

        verify(workoutService).completeWorkout(100L, 10L);
    }

    @Test
    void anotherMembersOrMissingWorkoutCannotBeCompleted() throws Exception {
        when(workoutService.completeWorkout(999L, 10L)).thenReturn(false);

        mockMvc.perform(post("/workout/complete")
                .session(loginSession)
                .param("workoutLogId", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workout/list?notFound"));
    }

    @Test
    void workoutListUsesSessionMemberId() throws Exception {
        when(workoutService.getWorkoutLogs(10L))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/workout/list").session(loginSession))
                .andExpect(status().isOk())
                .andExpect(view().name("workout/list"))
                .andExpect(model().attributeExists("workoutLogs"));

        verify(workoutService).getWorkoutLogs(10L);
    }

    @Test
    void addSetUsesSessionMemberIdAndRedirectsByPrg() throws Exception {
        when(workoutSetService.add(10L, 100L, 1000L,
                new BigDecimal("30.50"), 12, "가볍게")).thenReturn(true);

        mockMvc.perform(post("/workout/set/add")
                .session(loginSession)
                .param("memberId", "999")
                .param("workoutLogId", "100")
                .param("workoutExerciseId", "1000")
                .param("weight", "30.50")
                .param("reps", "12")
                .param("memo", "가볍게"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workout/record?workoutLogId=100"));

        verify(workoutSetService).add(10L, 100L, 1000L,
                new BigDecimal("30.50"), 12, "가볍게");
    }

    @Test
    void unsafeWorkoutExerciseSetAddIsBlocked() throws Exception {
        when(workoutSetService.add(10L, 100L, 9999L, null, 10, null))
                .thenReturn(false);

        mockMvc.perform(post("/workout/set/add")
                .session(loginSession)
                .param("workoutLogId", "100")
                .param("workoutExerciseId", "9999")
                .param("reps", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"));
    }

    @Test
    void invalidSetInputReturnsToRecordWithMessage() throws Exception {
        when(workoutSetService.add(10L, 100L, 1000L, null, 0, null))
                .thenThrow(new IllegalArgumentException(
                        "반복 횟수는 1 이상 입력해 주세요."));

        mockMvc.perform(post("/workout/set/add")
                .session(loginSession)
                .param("workoutLogId", "100")
                .param("workoutExerciseId", "1000")
                .param("reps", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workout/record?workoutLogId=100"))
                .andExpect(flash().attribute(
                        "setError", "반복 횟수는 1 이상 입력해 주세요."));
    }

    @Test
    void removeSetUsesSessionMemberId() throws Exception {
        when(workoutSetService.remove(10L, 100L, 1000L, 5000L))
                .thenReturn(true);

        mockMvc.perform(post("/workout/set/remove")
                .session(loginSession)
                .param("memberId", "999")
                .param("workoutLogId", "100")
                .param("workoutExerciseId", "1000")
                .param("workoutSetId", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/workout/record?workoutLogId=100"));

        verify(workoutSetService).remove(10L, 100L, 1000L, 5000L);
    }

    @Test
    void anotherMembersSetCannotBeRemoved() throws Exception {
        when(workoutSetService.remove(10L, 999L, 1000L, 5000L))
                .thenReturn(false);

        mockMvc.perform(post("/workout/set/remove")
                .session(loginSession)
                .param("workoutLogId", "999")
                .param("workoutExerciseId", "1000")
                .param("workoutSetId", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"));
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
        MvcResult complete = mockMvc.perform(post("/workout/complete")
                .param("workoutLogId", "100"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult list = mockMvc.perform(get("/workout/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult add = mockMvc.perform(post("/workout/set/add")
                .param("workoutLogId", "100")
                .param("workoutExerciseId", "1000")
                .param("reps", "10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult remove = mockMvc.perform(post("/workout/set/remove")
                .param("workoutLogId", "100")
                .param("workoutExerciseId", "1000")
                .param("workoutSetId", "5000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();

        assertThat(start.getRequest().getSession(false)).isNull();
        assertThat(record.getRequest().getSession(false)).isNull();
        assertThat(complete.getRequest().getSession(false)).isNull();
        assertThat(list.getRequest().getSession(false)).isNull();
        assertThat(add.getRequest().getSession(false)).isNull();
        assertThat(remove.getRequest().getSession(false)).isNull();
        verify(workoutService, never()).startWorkout(any(), any());
        verify(workoutService, never()).getWorkoutLog(any(), any());
        verify(workoutService, never()).completeWorkout(any(), any());
        verify(workoutService, never()).getWorkoutLogs(any());
        verify(workoutSetService, never())
                .add(any(), any(), any(), any(), any(), any());
        verify(workoutSetService, never())
                .remove(any(), any(), any(), any());
    }
}
