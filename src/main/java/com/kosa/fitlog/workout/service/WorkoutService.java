package com.kosa.fitlog.workout.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;
import com.kosa.fitlog.routine.mapper.RoutineMapper;
import com.kosa.fitlog.workout.dto.WorkoutExerciseDTO;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.mapper.WorkoutExerciseMapper;
import com.kosa.fitlog.workout.mapper.WorkoutLogMapper;

@Service
public class WorkoutService {

    private final RoutineMapper routineMapper;
    private final RoutineExerciseMapper routineExerciseMapper;
    private final WorkoutLogMapper workoutLogMapper;
    private final WorkoutExerciseMapper workoutExerciseMapper;

    public WorkoutService(RoutineMapper routineMapper,
            RoutineExerciseMapper routineExerciseMapper,
            WorkoutLogMapper workoutLogMapper,
            WorkoutExerciseMapper workoutExerciseMapper) {
        this.routineMapper = routineMapper;
        this.routineExerciseMapper = routineExerciseMapper;
        this.workoutLogMapper = workoutLogMapper;
        this.workoutExerciseMapper = workoutExerciseMapper;
    }

    @Transactional
    public Long startWorkout(Long memberId, Long routineId) {
        if (memberId == null || routineId == null
                || routineMapper.findByRoutineIdAndMemberId(routineId, memberId) == null) {
            return null;
        }

        List<RoutineExerciseDTO> routineExercises =
                routineExerciseMapper.findByRoutineId(routineId);
        if (routineExercises == null || routineExercises.isEmpty()) {
            throw new IllegalArgumentException("운동이 등록되지 않은 루틴입니다.");
        }

        WorkoutLogDTO workoutLog = new WorkoutLogDTO();
        workoutLog.setMemberId(memberId);
        if (workoutLogMapper.insertWorkoutLog(workoutLog) != 1
                || workoutLog.getWorkoutLogId() == null) {
            throw new IllegalStateException("운동 기록 생성에 실패했습니다.");
        }

        for (RoutineExerciseDTO routineExercise : routineExercises) {
            WorkoutExerciseDTO workoutExercise = new WorkoutExerciseDTO();
            workoutExercise.setWorkoutLogId(workoutLog.getWorkoutLogId());
            workoutExercise.setExerciseId(routineExercise.getExerciseId());
            workoutExercise.setExerciseOrder(routineExercise.getExerciseOrder());
            workoutExercise.setMemo(routineExercise.getMemo());

            if (workoutExerciseMapper.insertWorkoutExercise(workoutExercise) != 1) {
                throw new IllegalStateException("운동 종목 복사에 실패했습니다.");
            }
        }

        return workoutLog.getWorkoutLogId();
    }

    public WorkoutLogDTO getWorkoutLog(Long workoutLogId, Long memberId) {
        if (workoutLogId == null || memberId == null) {
            return null;
        }
        return workoutLogMapper.findByWorkoutLogIdAndMemberId(workoutLogId, memberId);
    }

    public List<WorkoutExerciseDTO> getWorkoutExercises(Long workoutLogId) {
        return workoutExerciseMapper.findByWorkoutLogId(workoutLogId);
    }

    public boolean completeWorkout(Long workoutLogId, Long memberId) {
        if (workoutLogId == null || memberId == null
                || workoutLogMapper.findByWorkoutLogIdAndMemberId(
                        workoutLogId, memberId) == null) {
            return false;
        }
        return workoutLogMapper.completeWorkout(workoutLogId, memberId) == 1;
    }

    public List<WorkoutLogDTO> getWorkoutLogs(Long memberId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        return workoutLogMapper.findAllByMemberId(memberId);
    }
}
