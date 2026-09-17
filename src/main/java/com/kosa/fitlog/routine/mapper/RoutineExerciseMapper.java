package com.kosa.fitlog.routine.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;

@Mapper
public interface RoutineExerciseMapper {

    List<RoutineExerciseDTO> findByRoutineId(@Param("routineId") Long routineId);

    Integer findNextExerciseOrder(@Param("routineId") Long routineId);

    int insertRoutineExercise(RoutineExerciseDTO routineExercise);

    int deleteRoutineExercise(
            @Param("routineExerciseId") Long routineExerciseId,
            @Param("routineId") Long routineId,
            @Param("memberId") Long memberId);

    int deleteAllByRoutineId(@Param("routineId") Long routineId);
}
