package com.kosa.fitlog.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    void multipleExercisesKeepSelectionOrderAndTheirOwnMemos() {
        ExerciseDTO exercise = new ExerciseDTO();
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        when(exerciseMapper.findById(any())).thenReturn(exercise);
        when(routineExerciseMapper.findNextExerciseOrder(7L)).thenReturn(5);
        when(routineExerciseMapper.insertRoutineExercise(any())).thenReturn(1);

        assertThat(service().add(10L, 7L, Arrays.asList(3L, 1L, 2L),
                Map.of(3L, "  첫 운동 메모  ", 1L, "   ", 2L, "마지막 운동 메모"))).isTrue();

        ArgumentCaptor<RoutineExerciseDTO> captor =
                ArgumentCaptor.forClass(RoutineExerciseDTO.class);
        verify(routineExerciseMapper,
                org.mockito.Mockito.times(3)).insertRoutineExercise(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(RoutineExerciseDTO::getExerciseOrder)
                .containsExactly(5, 6, 7);
        assertThat(captor.getAllValues()).extracting(RoutineExerciseDTO::getExerciseId)
                .containsExactly(3L, 1L, 2L);
        assertThat(captor.getAllValues()).extracting(RoutineExerciseDTO::getMemo)
                .containsExactly("첫 운동 메모", null, "마지막 운동 메모");
        verify(routineExerciseMapper).findNextExerciseOrder(7L);
    }

    @Test
    void anotherMembersRoutineCannotReceiveExercise() {
        when(routineMapper.lockOwnedRoutine(99L, 10L)).thenReturn(null);

        assertThat(service().add(10L, 99L, Arrays.asList(1L, 2L), Collections.emptyMap())).isFalse();
        verify(exerciseMapper, never()).findById(any());
        verify(routineExerciseMapper, never()).findByRoutineId(any());
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void missingMasterExerciseCannotBeAdded() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        when(exerciseMapper.findById(1L)).thenReturn(new ExerciseDTO());
        when(exerciseMapper.findById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service().add(10L, 7L, Arrays.asList(1L, 999L), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void oneSelectedExerciseStillWorks() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        when(exerciseMapper.findById(1L)).thenReturn(new ExerciseDTO());
        when(routineExerciseMapper.findNextExerciseOrder(7L)).thenReturn(1);
        when(routineExerciseMapper.insertRoutineExercise(any())).thenReturn(1);
        assertThat(service().add(10L, 7L, Collections.singletonList(1L), null)).isTrue();
        ArgumentCaptor<RoutineExerciseDTO> captor = ArgumentCaptor.forClass(RoutineExerciseDTO.class);
        verify(routineExerciseMapper).insertRoutineExercise(captor.capture());
        assertThat(captor.getValue().getExerciseOrder()).isEqualTo(1);
        assertThat(captor.getValue().getMemo()).isNull();
    }

    @Test
    void emptyAndMissingSelectionsAreRejected() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        assertThatThrownBy(() -> service().add(10L, 7L, null, null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("하나 이상");
        assertThatThrownBy(() -> service().add(10L, 7L, Collections.emptyList(), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void existingExerciseRejectsWholeSelectionBeforeAnyInsert() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        RoutineExerciseDTO existing = new RoutineExerciseDTO();
        existing.setExerciseId(2L);
        when(routineExerciseMapper.findByRoutineId(7L)).thenReturn(Collections.singletonList(existing));
        when(exerciseMapper.findById(any())).thenReturn(new ExerciseDTO());
        assertThatThrownBy(() -> service().add(10L, 7L, Arrays.asList(1L, 2L), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("이미 추가된");
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void repeatedRequestIdsAreRejectedBeforeAnyInsert() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        when(exerciseMapper.findById(1L)).thenReturn(new ExerciseDTO());
        assertThatThrownBy(() -> service().add(10L, 7L, Arrays.asList(1L, 1L), null))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("중복");
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void invalidSecondMemoCannotLeaveFirstExerciseInserted() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        when(exerciseMapper.findById(any())).thenReturn(new ExerciseDTO());
        assertThatThrownBy(() -> service().add(10L, 7L, Arrays.asList(1L, 2L), Map.of(2L, "a".repeat(501))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("500자");
        verify(routineExerciseMapper, never()).insertRoutineExercise(any());
    }

    @Test
    void nullExerciseIdIsRejected() {
        when(routineMapper.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        assertThatThrownBy(() -> service().add(10L, 7L, Collections.singletonList(null), null))
                .isInstanceOf(IllegalArgumentException.class);
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
