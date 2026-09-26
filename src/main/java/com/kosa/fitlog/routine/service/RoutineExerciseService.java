package com.kosa.fitlog.routine.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public boolean add(Long memberId, Long routineId, List<Long> exerciseIds,
            Map<Long, String> memos) {
        // 소유권 확인과 잠금을 함께 수행해 동시 요청의 중복/순서 충돌을 막는다.
        if (memberId == null || routineId == null
                || routineMapper.lockOwnedRoutine(routineId, memberId) == null) {
            return false;
        }

        if (exerciseIds == null || exerciseIds.isEmpty()) {
            throw new IllegalArgumentException("추가할 운동을 하나 이상 선택해 주세요.");
        }

        Set<Long> existingExerciseIds = new HashSet<>();
        for (RoutineExerciseDTO existing : routineExerciseMapper.findByRoutineId(routineId)) {
            existingExerciseIds.add(existing.getExerciseId());
        }

        Set<Long> selectedIds = new HashSet<>();
        List<RoutineExerciseDTO> additions = new ArrayList<>();
        // 전체 입력을 검증한 뒤 INSERT한다. 메모는 목록 위치가 아닌 운동 ID로 연결한다.
        for (Long exerciseId : exerciseIds) {
            if (exerciseId == null || exerciseId <= 0 || exerciseMapper.findById(exerciseId) == null) {
                throw new IllegalArgumentException("선택한 운동을 찾을 수 없습니다. 다시 선택해 주세요.");
            }
            if (!selectedIds.add(exerciseId) || existingExerciseIds.contains(exerciseId)) {
                throw new IllegalArgumentException("중복되거나 이미 추가된 운동이 있습니다. 목록을 확인해 주세요.");
            }

            RoutineExerciseDTO routineExercise = new RoutineExerciseDTO();
            routineExercise.setRoutineId(routineId);
            routineExercise.setExerciseId(exerciseId);
            routineExercise.setMemo(validateMemo(memos == null ? null : memos.get(exerciseId)));
            additions.add(routineExercise);
        }

        int nextOrder = routineExerciseMapper.findNextExerciseOrder(routineId);
        for (RoutineExerciseDTO routineExercise : additions) {
            routineExercise.setExerciseOrder(nextOrder++);
            if (routineExerciseMapper.insertRoutineExercise(routineExercise) != 1) {
                // false를 반환하면 앞선 INSERT가 커밋될 수 있으므로 예외로 전체 롤백한다.
                throw new IllegalStateException("선택한 운동을 추가하지 못했습니다.");
            }
        }
        return true;
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
