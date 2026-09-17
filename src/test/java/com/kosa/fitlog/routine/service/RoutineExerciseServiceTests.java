package com.kosa.fitlog.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.mapper.ExerciseMapper;
import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;
import com.kosa.fitlog.routine.mapper.RoutineMapper;

@ExtendWith(MockitoExtension.class)
class RoutineExerciseServiceTests {

    @Mock
    private RoutineMapper routineMapper;
    @Mock
    private RoutineExerciseMapper routineExerciseMapper;
    @Mock
    private ExerciseMapper exerciseMapper;

    @Test
    void routineExerciseJoinResultsAreReturned() {
        RoutineExerciseDTO item = new RoutineExerciseDTO();
        item.setExerciseName("벤치프레스");
        when(routineExerciseMapper.findByRoutineId(7L)).thenReturn(Arrays.asList(item));

        List<RoutineExerciseDTO> result = service().getRoutineExercises(7L);

        assertThat(result).containsExactly(item);
    }

    @Test
    void exerciseOrderUsesOneTwoThreeFromMapper() {
        RoutineDTO routine = new RoutineDTO();
        ExerciseDTO exercise = new ExerciseDTO();
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L)).thenReturn(routine);
        when(exerciseMapper.findById(any())).thenReturn(exercise);
        when(routineExerciseMapper.findNextExerciseOrder(7L)).thenReturn(1, 2, 3);
        when(routineExerciseMapper.insertRoutineExercise(any())).thenReturn(1);

        service().add(10L, 7L, 1L, null);
        service().add(10L, 7L, 2L, null);
        service().add(10L, 7L, 3L, null);

        ArgumentCaptor<RoutineExerciseDTO> captor =
                ArgumentCaptor.forClass(RoutineExerciseDTO.class);
        verify(routineExerciseMapper,
                org.mockito.Mockito.times(3)).insertRoutineExercise(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(RoutineExerciseDTO::getExerciseOrder)
                .containsExactly(1, 2, 3);
    }

    @Test
    void anotherMembersRoutineCannotReceiveExercise() {
        when(routineMapper.findByRoutineIdAndMemberId(99L, 10L)).thenReturn(null);

        assertThat(service().add(10L, 99L, 1L, null)).isFalse();
        verify(exerciseMapper, never()).findById(any());
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void missingMasterExerciseCannotBeAdded() {
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L))
                .thenReturn(new RoutineDTO());
        when(exerciseMapper.findById(999L)).thenReturn(null);

        assertThat(service().add(10L, 7L, 999L, null)).isFalse();
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void ownRoutineExerciseCanBeRemoved() {
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L))
                .thenReturn(new RoutineDTO());
        when(routineExerciseMapper.deleteRoutineExercise(30L, 7L, 10L)).thenReturn(1);

        assertThat(service().remove(10L, 7L, 30L)).isTrue();
        verify(routineExerciseMapper).deleteRoutineExercise(30L, 7L, 10L);
    }

    @Test
    void anotherMembersRoutineExerciseCannotBeRemoved() {
        when(routineMapper.findByRoutineIdAndMemberId(99L, 10L)).thenReturn(null);

        assertThat(service().remove(10L, 99L, 30L)).isFalse();
        verify(routineExerciseMapper, never()).deleteRoutineExercise(any(), any(), any());
    }

    private RoutineExerciseService service() {
        return new RoutineExerciseService(
                routineMapper, routineExerciseMapper, exerciseMapper);
    }
}
