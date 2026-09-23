package com.kosa.fitlog.bodycomposition.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.bodycomposition.dto.BodyCompositionDTO;

@Mapper
public interface BodyCompositionMapper {

    int insert(BodyCompositionDTO bodyComposition);

    List<BodyCompositionDTO> findByMemberId(@Param("memberId") Long memberId);

    int countByMemberIdAndMeasuredDate(
            @Param("memberId") Long memberId,
            @Param("measuredDate") LocalDate measuredDate);

    int deleteByIdAndMemberId(
            @Param("bodyCompositionId") Long bodyCompositionId,
            @Param("memberId") Long memberId);
}
