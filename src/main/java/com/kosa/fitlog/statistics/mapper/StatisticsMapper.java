package com.kosa.fitlog.statistics.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.statistics.dto.StatisticsDTO;

@Mapper
public interface StatisticsMapper {

    List<StatisticsDTO> findExerciseStatistics(
            @Param("memberId") Long memberId);

}
