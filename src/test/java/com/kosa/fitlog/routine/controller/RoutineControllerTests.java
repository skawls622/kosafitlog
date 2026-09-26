package com.kosa.fitlog.routine.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

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
import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.service.RoutineService;
import com.kosa.fitlog.routine.service.RoutineExerciseService;

@ExtendWith(MockitoExtension.class)
class RoutineControllerTests {

    @Mock
    private RoutineService routineService;

    @Mock
    private RoutineExerciseService routineExerciseService;

    private MockMvc mockMvc;
    private MockHttpSession loginSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RoutineController(routineService, routineExerciseService))
                .build();
        loginSession = new MockHttpSession();
        loginSession.setAttribute("loginMember", new LoginMember(10L, "member10", "회원10"));
    }

    @Test
    void registerUsesSessionMemberIdAndDoesNotNeedMemberIdParameter() throws Exception {
        mockMvc.perform(post("/routine/register")
                .session(loginSession)
                .param("routineName", "아침 루틴")
                .param("description", "상체 운동"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list"));

        verify(routineService).register(10L, "아침 루틴", "상체 운동");
    }

    @Test
    void requestMemberIdCannotOverrideSessionMemberId() throws Exception {
        mockMvc.perform(post("/routine/register")
                .session(loginSession)
                .param("memberId", "999")
                .param("routineName", "내 루틴"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list"));

        verify(routineService).register(10L, "내 루틴", null);
    }

    @Test
    void listUsesSessionMemberId() throws Exception {
        when(routineService.getRoutines(10L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/routine/list").session(loginSession))
                .andExpect(status().isOk())
                .andExpect(view().name("routine/list"))
                .andExpect(model().attributeExists("routines"));

        verify(routineService).getRoutines(10L);
    }

    @Test
    void detailUsesRoutineIdAndSessionMemberId() throws Exception {
        RoutineDTO routine = new RoutineDTO();
        routine.setRoutineId(7L);
        routine.setMemberId(10L);
        when(routineService.getRoutine(7L, 10L)).thenReturn(routine);

        mockMvc.perform(get("/routine/read")
                .session(loginSession)
                .param("routineId", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("routine/read"))
                .andExpect(model().attribute("routine", routine))
                .andExpect(model().attributeExists("routineExercises"));

        verify(routineService).getRoutine(7L, 10L);
        verify(routineExerciseService).getRoutineExercises(7L);
    }

    @Test
    void anotherMembersRoutineIsNotExposed() throws Exception {
        when(routineService.getRoutine(99L, 10L)).thenReturn(null);

        mockMvc.perform(get("/routine/read")
                .session(loginSession)
                .param("routineId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"))
                .andExpect(model().attributeDoesNotExist("routine"));
    }

    @Test
    void ownRoutineModifyFormIsShown() throws Exception {
        RoutineDTO routine = new RoutineDTO();
        routine.setRoutineId(7L);
        routine.setMemberId(10L);
        when(routineService.getRoutine(7L, 10L)).thenReturn(routine);

        mockMvc.perform(get("/routine/modify")
                .session(loginSession)
                .param("routineId", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("routine/modify"))
                .andExpect(model().attribute("routine", routine));
    }

    @Test
    void anotherMembersRoutineModifyFormIsNotShown() throws Exception {
        when(routineService.getRoutine(99L, 10L)).thenReturn(null);

        mockMvc.perform(get("/routine/modify")
                .session(loginSession)
                .param("routineId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"))
                .andExpect(model().attributeDoesNotExist("routine"));
    }

    @Test
    void modifyUsesSessionMemberIdAndRedirectsToDetail() throws Exception {
        when(routineService.modify(10L, 7L, "수정 루틴", "수정 설명")).thenReturn(true);

        mockMvc.perform(post("/routine/modify")
                .session(loginSession)
                .param("memberId", "999")
                .param("routineId", "7")
                .param("routineName", "수정 루틴")
                .param("description", "수정 설명"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/read?routineId=7"));

        verify(routineService).modify(10L, 7L, "수정 루틴", "수정 설명");
    }

    @Test
    void anotherMembersRoutineCannotBeModified() throws Exception {
        when(routineService.modify(10L, 99L, "변경 시도", null)).thenReturn(false);

        mockMvc.perform(post("/routine/modify")
                .session(loginSession)
                .param("routineId", "99")
                .param("routineName", "변경 시도"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"));
    }

    @Test
    void removeUsesSessionMemberId() throws Exception {
        when(routineService.remove(7L, 10L)).thenReturn(true);

        mockMvc.perform(post("/routine/remove")
                .session(loginSession)
                .param("memberId", "999")
                .param("routineId", "7"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list"));

        verify(routineService).remove(7L, 10L);
    }

    @Test
    void anotherMembersRoutineCannotBeRemoved() throws Exception {
        when(routineService.remove(99L, 10L)).thenReturn(false);

        mockMvc.perform(post("/routine/remove")
                .session(loginSession)
                .param("routineId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list"));

        verify(routineService).remove(99L, 10L);
    }

    @Test
    void ownRoutineExerciseAddFormIsShown() throws Exception {
        RoutineDTO routine = new RoutineDTO();
        routine.setRoutineId(7L);
        when(routineService.getRoutine(7L, 10L)).thenReturn(routine);
        when(routineExerciseService.getAllExercises()).thenReturn(Collections.emptyList());
        RoutineExerciseDTO existing = new RoutineExerciseDTO();
        existing.setExerciseId(3L);
        when(routineExerciseService.getRoutineExercises(7L)).thenReturn(Collections.singletonList(existing));

        mockMvc.perform(get("/routine/exercise/add")
                .session(loginSession)
                .param("routineId", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("routine/exercise-add"))
                .andExpect(model().attribute("routine", routine))
                .andExpect(model().attributeExists("exercises"))
                .andExpect(model().attribute("addedExerciseIds", Set.of(3L)));
    }

    @Test
    void anotherMembersRoutineExerciseAddFormIsBlocked() throws Exception {
        when(routineService.getRoutine(99L, 10L)).thenReturn(null);

        mockMvc.perform(get("/routine/exercise/add")
                .session(loginSession)
                .param("routineId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"));

        verify(routineExerciseService, never()).getAllExercises();
    }

    @Test
    void addExerciseUsesSessionMemberId() throws Exception {
        when(routineExerciseService.add(10L, 7L, Collections.singletonList(3L), Map.of(3L, "가볍게"))).thenReturn(true);

        mockMvc.perform(post("/routine/exercise/add")
                .session(loginSession)
                .param("memberId", "999")
                .param("routineId", "7")
                .param("exerciseIds", "3")
                .param("memos[3]", "가볍게"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/read?routineId=7"));

        verify(routineExerciseService).add(10L, 7L, Collections.singletonList(3L), Map.of(3L, "가볍게"));
    }

    @Test
    void multipleSelectionKeepsOrderAndIgnoresUnselectedMemo() throws Exception {
        when(routineExerciseService.add(10L, 7L, Arrays.asList(3L, 2L),
                Map.of(3L, "첫 번째", 2L, "두 번째"))).thenReturn(true);
        mockMvc.perform(post("/routine/exercise/add").session(loginSession)
                .param("memberId", "999").param("routineId", "7")
                .param("exerciseIds", "3", "2")
                .param("memos[3]", "첫 번째").param("memos[2]", "두 번째")
                .param("memos[99]", "선택하지 않은 운동"))
                .andExpect(redirectedUrl("/routine/read?routineId=7"));
        verify(routineExerciseService).add(10L, 7L, Arrays.asList(3L, 2L),
                Map.of(3L, "첫 번째", 2L, "두 번째"));
    }

    @Test
    void emptySelectionReturnsToSelectionWithMessage() throws Exception {
        when(routineExerciseService.add(10L, 7L, null, Collections.emptyMap()))
                .thenThrow(new IllegalArgumentException("추가할 운동을 하나 이상 선택해 주세요."));
        mockMvc.perform(post("/routine/exercise/add").session(loginSession).param("routineId", "7"))
                .andExpect(redirectedUrl("/routine/exercise/add?routineId=7"))
                .andExpect(flash().attribute("exerciseError", "추가할 운동을 하나 이상 선택해 주세요."));
    }

    @Test
    void duplicateSelectionReturnsToFreshSelectionPage() throws Exception {
        when(routineExerciseService.add(10L, 7L, Arrays.asList(3L, 2L), Map.of(3L, "", 2L, "")))
                .thenThrow(new IllegalArgumentException("이미 추가된 운동이 있습니다."));
        mockMvc.perform(post("/routine/exercise/add").session(loginSession).param("routineId", "7")
                .param("exerciseIds", "3", "2").param("memos[3]", "").param("memos[2]", ""))
                .andExpect(redirectedUrl("/routine/exercise/add?routineId=7"))
                .andExpect(flash().attribute("exerciseError", "이미 추가된 운동이 있습니다."));
    }

    @Test
    void multipleAdditionStillRejectsAnotherMembersRoutine() throws Exception {
        when(routineExerciseService.add(10L, 99L, Arrays.asList(3L, 2L), Map.of(3L, "", 2L, ""))).thenReturn(false);
        mockMvc.perform(post("/routine/exercise/add").session(loginSession).param("routineId", "99")
                .param("exerciseIds", "3", "2").param("memos[3]", "").param("memos[2]", ""))
                .andExpect(redirectedUrl("/routine/list?notFound"));
    }

    @Test
    void failedBatchReturnsToSelectionWithGenericMessage() throws Exception {
        when(routineExerciseService.add(10L, 7L, Collections.singletonList(3L), Map.of(3L, "")))
                .thenThrow(new IllegalStateException("insert failed"));
        mockMvc.perform(post("/routine/exercise/add").session(loginSession).param("routineId", "7")
                .param("exerciseIds", "3").param("memos[3]", ""))
                .andExpect(redirectedUrl("/routine/exercise/add?routineId=7"))
                .andExpect(flash().attribute("exerciseError", "운동 추가에 실패했습니다. 다시 시도해 주세요."));
    }

    @Test
    void removeExerciseUsesSessionMemberId() throws Exception {
        when(routineExerciseService.remove(10L, 7L, 30L)).thenReturn(true);

        mockMvc.perform(post("/routine/exercise/remove")
                .session(loginSession)
                .param("memberId", "999")
                .param("routineId", "7")
                .param("routineExerciseId", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/read?routineId=7"));

        verify(routineExerciseService).remove(10L, 7L, 30L);
    }

    @Test
    void anotherMembersRoutineExerciseCannotBeRemoved() throws Exception {
        when(routineExerciseService.remove(10L, 99L, 30L)).thenReturn(false);

        mockMvc.perform(post("/routine/exercise/remove")
                .session(loginSession)
                .param("routineId", "99")
                .param("routineExerciseId", "30"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/routine/list?notFound"));
    }

    @Test
    void unauthenticatedRoutinePagesRedirectToLoginWithoutCreatingSession() throws Exception {
        MvcResult register = mockMvc.perform(get("/routine/register"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult list = mockMvc.perform(get("/routine/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult read = mockMvc.perform(get("/routine/read").param("routineId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult post = mockMvc.perform(post("/routine/register")
                .param("routineName", "다른 회원 루틴"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult modifyForm = mockMvc.perform(get("/routine/modify").param("routineId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult modify = mockMvc.perform(post("/routine/modify")
                .param("routineId", "1")
                .param("routineName", "수정 시도"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult remove = mockMvc.perform(post("/routine/remove").param("routineId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult exerciseAddForm = mockMvc.perform(
                get("/routine/exercise/add").param("routineId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult exerciseAdd = mockMvc.perform(post("/routine/exercise/add")
                .param("routineId", "1")
                .param("exerciseIds", "1", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();
        MvcResult exerciseRemove = mockMvc.perform(post("/routine/exercise/remove")
                .param("routineId", "1")
                .param("routineExerciseId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"))
                .andReturn();

        assertThat(register.getRequest().getSession(false)).isNull();
        assertThat(list.getRequest().getSession(false)).isNull();
        assertThat(read.getRequest().getSession(false)).isNull();
        assertThat(post.getRequest().getSession(false)).isNull();
        assertThat(modifyForm.getRequest().getSession(false)).isNull();
        assertThat(modify.getRequest().getSession(false)).isNull();
        assertThat(remove.getRequest().getSession(false)).isNull();
        assertThat(exerciseAddForm.getRequest().getSession(false)).isNull();
        assertThat(exerciseAdd.getRequest().getSession(false)).isNull();
        assertThat(exerciseRemove.getRequest().getSession(false)).isNull();
        verify(routineService, never()).register(any(), any(), any());
        verify(routineService, never()).modify(any(), any(), any(), any());
        verify(routineService, never()).remove(any(), any());
        verify(routineExerciseService, never()).add(any(), any(), any(), any());
        verify(routineExerciseService, never()).remove(any(), any(), any());
    }
}
