package com.kosa.fitlog.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.member.dto.MemberDTO;

@Mapper
public interface MemberMapper {

    int insertMember(MemberDTO member);

    MemberDTO findByLoginId(@Param("loginId") String loginId);
}
