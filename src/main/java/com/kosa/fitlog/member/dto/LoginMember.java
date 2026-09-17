package com.kosa.fitlog.member.dto;

public class LoginMember {

    private final Long memberId;
    private final String loginId;
    private final String nickname;

    public LoginMember(Long memberId, String loginId, String nickname) {
        this.memberId = memberId;
        this.loginId = loginId;
        this.nickname = nickname;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getNickname() {
        return nickname;
    }
}
