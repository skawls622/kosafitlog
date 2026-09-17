package com.kosa.fitlog.routine.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.mapper.ExerciseMapper;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;
import com.kosa.fitlog.routine.mapper.RoutineMapper;

@Service
public class RoutineExerciseService {

    private final RoutineMapper routineMapper;
    private final RoutineExerciseMapper routineExerciseMapper;
    private final ExerciseMapper exerciseMapper;

    public RoutineExerciseService(RoutineMapper routineMapper,
            RoutineExerciseMapper routineExerciseMapper,
            ExerciseMapper exerciseMapper) {
        this.routineMapper = routineMapper;
        this.routineExerciseMapper = routineExerciseMapper;
        this.exerciseMapper = exerciseMapper;
    }

    public List<RoutineExerciseDTO> getRoutineExercises(Long routineId) {
        return routineExerciseMapper.findByRoutineId(routineId);
    }

    public List<ExerciseDTO> getAllExercises() {
        return exerciseMapper.findAll();
    }

    public boolean add(Long memberId, Long routineId, Long exerciseId, String memo) {
        if (routineMapper.findByRoutineIdAndMemberId(routineId, memberId) == null
                || exerciseMapper.findById(exerciseId) == null) {
            return false;
        }

        RoutineExerciseDTO routineExercise = new RoutineExerciseDTO();
        routineExercise.setRoutineId(routineId);
        routineExercise.setExerciseId(exerciseId);
        routineExercise.setExerciseOrder(routineExerciseMapper.findNextExerciseOrder(routineId));
        routineExercise.setMemo(validateMemo(memo));
        return routineExerciseMapper.insertRoutineExercise(routineExercise) == 1;
    }

    public boolean remove(Long memberId, Long routineId, Long routineExerciseId) {
        if (routineMapper.findByRoutineIdAndMemberId(routineId, memberId) == null) {
            return false;
        }
        return routineExerciseMapper.deleteRoutineExercise(
                routineExerciseId, routineId, memberId) == 1;
    }

    private String validateMemo(String memo) {
        if (memo == null || memo.trim().isEmpty()) {
            return null;
        }
        String trimmedMemo = memo.trim();
        if (trimmedMemo.length() > 500) {
            throw new IllegalArgumentException("메모는 500자 이하로 입력해 주세요.");
        }
        return trimmedMemo;
    }
}
