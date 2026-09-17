package com.kosa.fitlog.workout.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.workout.dto.WorkoutLogDTO;

@Mapper
public interface WorkoutLogMapper {

    int insertWorkoutLog(WorkoutLogDTO workoutLog);

    WorkoutLogDTO findByWorkoutLogIdAndMemberId(
            @Param("workoutLogId") Long workoutLogId,
            @Param("memberId") Long memberId);

    int completeWorkout(
            @Param("workoutLogId") Long workoutLogId,
            @Param("memberId") Long memberId);

    List<WorkoutLogDTO> findAllByMemberId(
            @Param("memberId") Long memberId);
}
