package com.kosa.fitlog.workout.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.workout.dto.WorkoutExerciseDTO;

@Mapper
public interface WorkoutExerciseMapper {

    int insertWorkoutExercise(WorkoutExerciseDTO workoutExercise);

    List<WorkoutExerciseDTO> findByWorkoutLogId(
            @Param("workoutLogId") Long workoutLogId);

    int countOwnedWorkoutExercise(
            @Param("workoutExerciseId") Long workoutExerciseId,
            @Param("workoutLogId") Long workoutLogId,
            @Param("memberId") Long memberId);
}
