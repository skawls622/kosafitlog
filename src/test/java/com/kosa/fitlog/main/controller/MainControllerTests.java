package com.kosa.fitlog.main.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.kosa.fitlog.member.dto.LoginMember;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.service.WorkoutService;

@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkoutService workoutService;

    @Test
    void redirectsRootToMain() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"));
    }

    @Test
    void showsPublicMainWithoutLogin() throws Exception {
        mockMvc.perform(get("/main"))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(content().string(containsString("기록을 쌓고,")))
                .andExpect(content().string(containsString("href=\"/member/login\"")))
                .andExpect(content().string(containsString("href=\"/member/signup\"")))
                .andExpect(content().string(not(containsString("action=\"/member/logout\""))))
                .andExpect(content().string(not(containsString("href=\"/routine/list\""))))
                .andExpect(content().string(not(containsString("href=\"/workout/list\""))))
                .andExpect(content().string(not(containsString("운동 캘린더"))));
        verifyNoInteractions(workoutService);
    }

    @Test
    void showsMemberMainAndKeepsLoginSession() throws Exception {
        LoginMember loginMember = new LoginMember(10L, "member10", "회원10");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMember", loginMember);
        when(workoutService.getCompletedWorkoutDates(10L))
                .thenReturn(Collections.singletonList(LocalDate.of(2026, 9, 23)));

        mockMvc.perform(get("/main").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(request().sessionAttribute("loginMember", loginMember))
                .andExpect(content().string(containsString("안녕하세요,")))
                .andExpect(content().string(containsString("회원10")))
                .andExpect(content().string(containsString("href=\"/routine/list\"")))
                .andExpect(content().string(containsString("href=\"/workout/list\"")))
                .andExpect(content().string(containsString("운동 캘린더")))
                .andExpect(content().string(containsString("const workoutDates = [\"2026-09-23\"];")))
                .andExpect(content().string(containsString("const workoutListUrl = \"\\/workout\\/list\";")))
                .andExpect(content().string(containsString("action=\"/member/logout\" method=\"post\"")))
                .andExpect(content().string(not(containsString("href=\"/member/signup\""))))
                .andExpect(content().string(not(containsString(">로그인</a>"))));
        verify(workoutService).getCompletedWorkoutDates(10L);
    }

    @Test
    void calendarDateOpensEveryWorkoutOnThatDate() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 23);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMember", new LoginMember(10L, "member10", "회원10"));
        WorkoutLogDTO first = new WorkoutLogDTO();
        first.setWorkoutLogId(100L);
        first.setWorkoutDate(LocalDateTime.of(2026, 9, 23, 9, 0));
        first.setUpdatedAt(LocalDateTime.of(2026, 9, 23, 10, 0));
        WorkoutLogDTO second = new WorkoutLogDTO();
        second.setWorkoutLogId(101L);
        second.setWorkoutDate(LocalDateTime.of(2026, 9, 23, 18, 0));
        second.setUpdatedAt(LocalDateTime.of(2026, 9, 23, 19, 0));
        when(workoutService.getCompletedWorkoutLogsByDate(10L, date))
                .thenReturn(Arrays.asList(first, second));

        mockMvc.perform(get("/workout/list").session(session).param("date", "2026-09-23"))
                .andExpect(status().isOk())
                .andExpect(view().name("workout/list"))
                .andExpect(content().string(containsString("2026-09-23 운동 기록")))
                .andExpect(content().string(containsString("전체 운동 기록 보기")))
                .andExpect(content().string(containsString("workoutLogId=100")))
                .andExpect(content().string(containsString("workoutLogId=101")));
        verify(workoutService).getCompletedWorkoutLogsByDate(10L, date);
    }

    @Test
    void logoutReturnsMainToPublicState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMember", new LoginMember(10L, "member10", "회원10"));

        mockMvc.perform(post("/member/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"));

        mockMvc.perform(get("/main"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/member/login\"")))
                .andExpect(content().string(containsString("href=\"/member/signup\"")))
                .andExpect(content().string(not(containsString("회원10"))));
    }
}
