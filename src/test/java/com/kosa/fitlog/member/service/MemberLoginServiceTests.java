package com.kosa.fitlog.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.kosa.fitlog.member.dto.MemberDTO;
import com.kosa.fitlog.member.mapper.MemberMapper;

@ExtendWith(MockitoExtension.class)
class MemberLoginServiceTests {

    @Mock
    private MemberMapper memberMapper;

    @Test
    void correctPasswordReturnsMember() {
        MemberDTO member = memberWithPassword("correct-password");
        when(memberMapper.findByLoginId("known")).thenReturn(member);

        assertThat(new MemberService(memberMapper).login("known", "correct-password")).isSameAs(member);
    }

    @Test
    void wrongPasswordFails() {
        when(memberMapper.findByLoginId("known")).thenReturn(memberWithPassword("correct-password"));

        assertThat(new MemberService(memberMapper).login("known", "wrong-password")).isNull();
    }

    @Test
    void unknownLoginIdFails() {
        assertThat(new MemberService(memberMapper).login("unknown", "any-password")).isNull();
    }

    private MemberDTO memberWithPassword(String rawPassword) {
        MemberDTO member = new MemberDTO();
        member.setLoginId("known");
        member.setPassword(new BCryptPasswordEncoder().encode(rawPassword));
        return member;
    }
}
