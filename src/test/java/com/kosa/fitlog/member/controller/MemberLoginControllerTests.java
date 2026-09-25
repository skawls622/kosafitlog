package com.kosa.fitlog.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import javax.servlet.http.HttpSession;

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
import com.kosa.fitlog.member.dto.MemberDTO;
import com.kosa.fitlog.member.service.MemberService;

@ExtendWith(MockitoExtension.class)
class MemberLoginControllerTests {

    @Mock
    private MemberService memberService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MemberController(memberService)).build();
    }

    @Test
    void successStoresPasswordFreeMemberInSession() throws Exception {
        MemberDTO member = new MemberDTO();
        member.setMemberId(10L);
        member.setLoginId("known");
        member.setNickname("회원");
        member.setPassword("bcrypt-hash");
        when(memberService.login("known", "correct-password")).thenReturn(member);

        MvcResult result = mockMvc.perform(post("/member/login")
                .param("loginId", "known")
                .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/main"))
                .andReturn();

        HttpSession session = result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        LoginMember loginMember = (LoginMember) session.getAttribute("loginMember");
        assertThat(loginMember.getLoginId()).isEqualTo("known");
        assertThat(loginMember.getNickname()).isEqualTo("회원");
        assertThat(LoginMember.class.getDeclaredFields())
                .extracting("name").doesNotContain("password");

        mockMvc.perform(get("/member/login-success").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(view().name("member/login-success"))
                .andExpect(model().attribute("loginMember", loginMember));
    }

    @Test
    void wrongPasswordDoesNotCreateLoginSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/member/login")
                .param("loginId", "known")
                .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login?error"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void failedLoginClearsPreviousLoginMember() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMember", new LoginMember(10L, "known", "회원"));

        mockMvc.perform(post("/member/login")
                .session(session)
                .param("loginId", "known")
                .param("password", "wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login?error"));

        assertThat(session.getAttribute("loginMember")).isNull();
    }

    @Test
    void unknownLoginIdDoesNotCreateLoginSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/member/login")
                .param("loginId", "unknown")
                .param("password", "any-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login?error"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void logoutInvalidatesSessionAndBlocksSuccessPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginMember", new LoginMember(10L, "known", "회원"));

        mockMvc.perform(post("/member/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"));

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/member/login-success"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login"));
    }
}
