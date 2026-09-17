package com.kosa.fitlog.member.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.kosa.fitlog.member.dto.MemberDTO;
import com.kosa.fitlog.member.mapper.MemberMapper;

@Service
public class MemberService {

    private final MemberMapper memberMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberService(MemberMapper memberMapper) {
        this.memberMapper = memberMapper;
    }

    public void signup(MemberDTO input) {
        if (input == null) {
            throw new IllegalArgumentException("가입 정보를 입력해 주세요.");
        }

        String loginId = required(input.getLoginId(), "아이디", 50);
        String password = input.getPassword();
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("비밀번호를 입력해 주세요.");
        }
        String nickname = required(input.getNickname(), "닉네임", 30);
        String email = optional(input.getEmail(), "이메일", 100);

        if (memberMapper.findByLoginId(loginId) != null) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        MemberDTO member = new MemberDTO();
        member.setLoginId(loginId);
        member.setPassword(passwordEncoder.encode(password));
        member.setNickname(nickname);
        member.setEmail(email);

        try {
            if (memberMapper.insertMember(member) != 1) {
                throw new IllegalStateException("회원가입 저장에 실패했습니다.");
            }
        } catch (DuplicateKeyException exception) {
            // 동시 가입 요청으로 조회 직후 아이디가 등록된 경우
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.", exception);
        }
    }

    public MemberDTO login(String loginId, String rawPassword) {
        if (loginId == null || loginId.trim().isEmpty()
                || rawPassword == null || rawPassword.isEmpty()) {
            return null;
        }

        MemberDTO member = memberMapper.findByLoginId(loginId.trim());
        if (member == null || member.getPassword() == null
                || !passwordEncoder.matches(rawPassword, member.getPassword())) {
            return null;
        }
        return member;
    }

    private String required(String value, String fieldName, int maxLength) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "을(를) 입력해 주세요.");
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "은(는) " + maxLength + "자 이하로 입력해 주세요.");
        }
        return trimmed;
    }

    private String optional(String value, String fieldName, int maxLength) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "은(는) " + maxLength + "자 이하로 입력해 주세요.");
        }
        return trimmed;
    }
}
