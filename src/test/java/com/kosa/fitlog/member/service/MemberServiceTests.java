package com.kosa.fitlog.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.kosa.fitlog.member.dto.MemberDTO;
import com.kosa.fitlog.member.mapper.MemberMapper;

@ExtendWith(MockitoExtension.class)
class MemberServiceTests {

    @Mock
    private MemberMapper memberMapper;

    @Test
    void signupStoresBcryptHashInsteadOfRawPassword() {
        MemberDTO input = new MemberDTO();
        input.setLoginId("newuser");
        input.setPassword("plain-password");
        input.setNickname("새회원");

        when(memberMapper.insertMember(any(MemberDTO.class))).thenReturn(1);
        new MemberService(memberMapper).signup(input);

        ArgumentCaptor<MemberDTO> captor = ArgumentCaptor.forClass(MemberDTO.class);
        verify(memberMapper).insertMember(captor.capture());
        String storedPassword = captor.getValue().getPassword();
        assertThat(storedPassword).isNotEqualTo("plain-password");
        assertThat(storedPassword.length()).isLessThanOrEqualTo(100);
        assertThat(new BCryptPasswordEncoder().matches("plain-password", storedPassword)).isTrue();
    }

    @Test
    void duplicateLoginIdIsNotInserted() {
        MemberDTO input = new MemberDTO();
        input.setLoginId("existing");
        input.setPassword("plain-password");
        input.setNickname("새회원");

        when(memberMapper.findByLoginId("existing")).thenReturn(new MemberDTO());
        assertThatThrownBy(() -> new MemberService(memberMapper).signup(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
        verify(memberMapper, never()).insertMember(any(MemberDTO.class));
    }
}
