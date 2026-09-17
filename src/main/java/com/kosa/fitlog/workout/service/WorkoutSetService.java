package com.kosa.fitlog.workout.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.kosa.fitlog.workout.dto.WorkoutExerciseDTO;
import com.kosa.fitlog.workout.dto.WorkoutSetDTO;
import com.kosa.fitlog.workout.mapper.WorkoutExerciseMapper;
import com.kosa.fitlog.workout.mapper.WorkoutLogMapper;
import com.kosa.fitlog.workout.mapper.WorkoutSetMapper;

@Service
public class WorkoutSetService {

    private static final BigDecimal MAX_WEIGHT = new BigDecimal("9999.99");

    private final WorkoutLogMapper workoutLogMapper;
    private final WorkoutExerciseMapper workoutExerciseMapper;
    private final WorkoutSetMapper workoutSetMapper;

    public WorkoutSetService(WorkoutLogMapper workoutLogMapper,
            WorkoutExerciseMapper workoutExerciseMapper,
            WorkoutSetMapper workoutSetMapper) {
        this.workoutLogMapper = workoutLogMapper;
        this.workoutExerciseMapper = workoutExerciseMapper;
        this.workoutSetMapper = workoutSetMapper;
    }

    public Map<Long, List<WorkoutSetDTO>> getSetsByWorkoutExercises(
            List<WorkoutExerciseDTO> workoutExercises) {
        Map<Long, List<WorkoutSetDTO>> result = new LinkedHashMap<>();
        if (workoutExercises == null) {
            return result;
        }

        for (WorkoutExerciseDTO workoutExercise : workoutExercises) {
            Long workoutExerciseId = workoutExercise.getWorkoutExerciseId();
            result.put(workoutExerciseId,
                    workoutSetMapper.findByWorkoutExerciseId(workoutExerciseId));
        }
        return result;
    }

    public boolean add(Long memberId, Long workoutLogId, Long workoutExerciseId,
            BigDecimal weight, Integer reps, String memo) {
        if (!isOwnedWorkoutExercise(memberId, workoutLogId, workoutExerciseId)) {
            return false;
        }

        BigDecimal validatedWeight = validateWeight(weight);
        Integer validatedReps = validateReps(reps);
        String validatedMemo = validateMemo(memo);

        WorkoutSetDTO workoutSet = new WorkoutSetDTO();
        workoutSet.setWorkoutExerciseId(workoutExerciseId);
        workoutSet.setSetNo(workoutSetMapper.findNextSetNo(workoutExerciseId));
        workoutSet.setWeight(validatedWeight);
        workoutSet.setReps(validatedReps);
        workoutSet.setMemo(validatedMemo);
        return workoutSetMapper.insertWorkoutSet(workoutSet) == 1;
    }

    public boolean remove(Long memberId, Long workoutLogId,
            Long workoutExerciseId, Long workoutSetId) {
        if (workoutSetId == null
                || !isOwnedWorkoutExercise(memberId, workoutLogId, workoutExerciseId)) {
            return false;
        }
        return workoutSetMapper.deleteWorkoutSet(
                workoutSetId, workoutExerciseId, workoutLogId, memberId) == 1;
    }

    private boolean isOwnedWorkoutExercise(Long memberId, Long workoutLogId,
            Long workoutExerciseId) {
        if (memberId == null || workoutLogId == null || workoutExerciseId == null) {
            return false;
        }
        if (workoutLogMapper.findByWorkoutLogIdAndMemberId(
                workoutLogId, memberId) == null) {
            return false;
        }
        return workoutExerciseMapper.countOwnedWorkoutExercise(
                workoutExerciseId, workoutLogId, memberId) == 1;
    }

    private Integer validateReps(Integer reps) {
        if (reps == null || reps < 1 || reps > 999) {
            throw new IllegalArgumentException("반복 횟수는 1 이상 999 이하로 입력해 주세요.");
        }
        return reps;
    }

    private BigDecimal validateWeight(BigDecimal weight) {
        if (weight == null) {
            return null;
        }
        if (weight.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("중량은 0 이상 입력해 주세요.");
        }
        if (weight.scale() > 2 || weight.compareTo(MAX_WEIGHT) > 0) {
            throw new IllegalArgumentException("중량은 9999.99 이하, 소수 둘째 자리까지 입력해 주세요.");
        }
        return weight;
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
