package com.kosa.fitlog.routine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.routine.dto.RoutineDTO;

@Mapper
public interface RoutineMapper {

    int insertRoutine(RoutineDTO routine);

    List<RoutineDTO> findAllByMemberId(@Param("memberId") Long memberId);

    RoutineDTO findByRoutineIdAndMemberId(
            @Param("routineId") Long routineId,
            @Param("memberId") Long memberId);

    Long lockOwnedRoutine(
            @Param("routineId") Long routineId,
            @Param("memberId") Long memberId);

    int updateRoutine(RoutineDTO routine);

    int deleteRoutine(
            @Param("routineId") Long routineId,
            @Param("memberId") Long memberId);
}
