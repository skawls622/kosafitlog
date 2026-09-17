package com.kosa.fitlog.workout.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.workout.dto.WorkoutSetDTO;

@Mapper
public interface WorkoutSetMapper {

    List<WorkoutSetDTO> findByWorkoutExerciseId(
            @Param("workoutExerciseId") Long workoutExerciseId);

    Integer findNextSetNo(
            @Param("workoutExerciseId") Long workoutExerciseId);

    int insertWorkoutSet(WorkoutSetDTO workoutSet);

    int deleteWorkoutSet(
            @Param("workoutSetId") Long workoutSetId,
            @Param("workoutExerciseId") Long workoutExerciseId,
            @Param("workoutLogId") Long workoutLogId,
            @Param("memberId") Long memberId);
}
