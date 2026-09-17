package com.kosa.fitlog.routine.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.mapper.RoutineMapper;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;

@Service
public class RoutineService {

    private final RoutineMapper routineMapper;
    private final RoutineExerciseMapper routineExerciseMapper;

    public RoutineService(RoutineMapper routineMapper, RoutineExerciseMapper routineExerciseMapper) {
        this.routineMapper = routineMapper;
        this.routineExerciseMapper = routineExerciseMapper;
    }

    public void register(Long memberId, String routineName, String description) {
        if (memberId == null) {
            throw new IllegalArgumentException("로그인 회원 정보가 필요합니다.");
        }

        RoutineDTO routine = new RoutineDTO();
        routine.setMemberId(memberId);
        routine.setRoutineName(validateRoutineName(routineName));
        routine.setDescription(validateDescription(description));

        if (routineMapper.insertRoutine(routine) != 1) {
            throw new IllegalStateException("루틴 등록에 실패했습니다.");
        }
    }

    public List<RoutineDTO> getRoutines(Long memberId) {
        return routineMapper.findAllByMemberId(memberId);
    }

    public RoutineDTO getRoutine(Long routineId, Long memberId) {
        return routineMapper.findByRoutineIdAndMemberId(routineId, memberId);
    }

    public boolean modify(Long memberId, Long routineId, String routineName, String description) {
        if (memberId == null || routineId == null) {
            throw new IllegalArgumentException("수정할 루틴 정보가 필요합니다.");
        }

        RoutineDTO routine = new RoutineDTO();
        routine.setRoutineId(routineId);
        routine.setMemberId(memberId);
        routine.setRoutineName(validateRoutineName(routineName));
        routine.setDescription(validateDescription(description));
        return routineMapper.updateRoutine(routine) == 1;
    }

    @Transactional
    public boolean remove(Long routineId, Long memberId) {
        if (routineId == null || memberId == null) {
            return false;
        }
        if (routineMapper.findByRoutineIdAndMemberId(routineId, memberId) == null) {
            return false;
        }

        routineExerciseMapper.deleteAllByRoutineId(routineId);
        if (routineMapper.deleteRoutine(routineId, memberId) != 1) {
            throw new IllegalStateException("루틴 삭제에 실패했습니다.");
        }
        return true;
    }

    private String validateRoutineName(String routineName) {
        String trimmedName = routineName == null ? "" : routineName.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("루틴명을 입력해 주세요.");
        }
        if (trimmedName.length() > 100) {
            throw new IllegalArgumentException("루틴명은 100자 이하로 입력해 주세요.");
        }
        return trimmedName;
    }

    private String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        String trimmedDescription = description.trim();
        if (trimmedDescription.length() > 500) {
            throw new IllegalArgumentException("설명은 500자 이하로 입력해 주세요.");
        }
        return trimmedDescription;
    }
}
