package com.kosa.fitlog.main.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.kosa.fitlog.member.dto.LoginMember;

@SpringBootTest
@AutoConfigureMockMvc
class MainControllerTests {

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(content().string(not(containsString("href=\"/workout/list\""))));
    }

    @Test
    void showsMemberMainAndKeepsLoginSession() throws Exception {
        LoginMember loginMember = new LoginMember(10L, "member10", "회원10");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMember", loginMember);

        mockMvc.perform(get("/main").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("main"))
                .andExpect(request().sessionAttribute("loginMember", loginMember))
                .andExpect(content().string(containsString("안녕하세요,")))
                .andExpect(content().string(containsString("회원10")))
                .andExpect(content().string(containsString("href=\"/routine/list\"")))
                .andExpect(content().string(containsString("href=\"/workout/list\"")))
                .andExpect(content().string(containsString("action=\"/member/logout\" method=\"post\"")))
                .andExpect(content().string(not(containsString("href=\"/member/signup\""))))
                .andExpect(content().string(not(containsString(">로그인</a>"))));
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
