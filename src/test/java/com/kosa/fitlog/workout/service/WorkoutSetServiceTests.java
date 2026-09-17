package com.kosa.fitlog.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kosa.fitlog.workout.dto.WorkoutExerciseDTO;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.dto.WorkoutSetDTO;
import com.kosa.fitlog.workout.mapper.WorkoutExerciseMapper;
import com.kosa.fitlog.workout.mapper.WorkoutLogMapper;
import com.kosa.fitlog.workout.mapper.WorkoutSetMapper;

@ExtendWith(MockitoExtension.class)
class WorkoutSetServiceTests {

    @Mock private WorkoutLogMapper workoutLogMapper;
    @Mock private WorkoutExerciseMapper workoutExerciseMapper;
    @Mock private WorkoutSetMapper workoutSetMapper;

    @Test
    void firstSetUsesOneAndPassesWeightRepsAndMemo() {
        allowOwnedExercise(10L, 100L, 1000L);
        when(workoutSetMapper.findNextSetNo(1000L)).thenReturn(1);
        when(workoutSetMapper.insertWorkoutSet(any())).thenReturn(1);

        boolean added = service().add(10L, 100L, 1000L,
                new BigDecimal("30.50"), 12, "마지막 두 개 힘들었음");

        assertThat(added).isTrue();
        ArgumentCaptor<WorkoutSetDTO> captor =
                ArgumentCaptor.forClass(WorkoutSetDTO.class);
        verify(workoutSetMapper).insertWorkoutSet(captor.capture());
        WorkoutSetDTO saved = captor.getValue();
        assertThat(saved.getWorkoutExerciseId()).isEqualTo(1000L);
        assertThat(saved.getSetNo()).isEqualTo(1);
        assertThat(saved.getWeight()).isEqualByComparingTo("30.50");
        assertThat(saved.getReps()).isEqualTo(12);
        assertThat(saved.getMemo()).isEqualTo("마지막 두 개 힘들었음");
    }

    @Test
    void maxSetNoTwoMakesNextSetThree() {
        allowOwnedExercise(10L, 100L, 1000L);
        when(workoutSetMapper.findNextSetNo(1000L)).thenReturn(3);
        when(workoutSetMapper.insertWorkoutSet(any())).thenReturn(1);

        service().add(10L, 100L, 1000L, null, 10, null);

        ArgumentCaptor<WorkoutSetDTO> captor =
                ArgumentCaptor.forClass(WorkoutSetDTO.class);
        verify(workoutSetMapper).insertWorkoutSet(captor.capture());
        assertThat(captor.getValue().getSetNo()).isEqualTo(3);
        assertThat(captor.getValue().getWeight()).isNull();
    }

    @Test
    void setListsAreMappedToEachWorkoutExerciseInMapperOrder() {
        WorkoutExerciseDTO firstExercise = workoutExercise(1000L);
        WorkoutExerciseDTO secondExercise = workoutExercise(2000L);
        WorkoutSetDTO set1 = workoutSet(1);
        WorkoutSetDTO set2 = workoutSet(2);
        when(workoutSetMapper.findByWorkoutExerciseId(1000L))
                .thenReturn(Arrays.asList(set1, set2));
        when(workoutSetMapper.findByWorkoutExerciseId(2000L))
                .thenReturn(Arrays.asList());

        Map<Long, List<WorkoutSetDTO>> result = service()
                .getSetsByWorkoutExercises(Arrays.asList(firstExercise, secondExercise));

        assertThat(result.get(1000L)).containsExactly(set1, set2);
        assertThat(result.get(2000L)).isEmpty();
    }

    @Test
    void anotherMembersWorkoutCannotReceiveSet() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(999L, 10L))
                .thenReturn(null);

        assertThat(service().add(10L, 999L, 1000L, null, 10, null)).isFalse();
        verify(workoutExerciseMapper, never())
                .countOwnedWorkoutExercise(any(), any(), any());
        verify(workoutSetMapper, never()).insertWorkoutSet(any());
    }

    @Test
    void exerciseFromDifferentWorkoutLogCannotReceiveSet() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(100L, 10L))
                .thenReturn(new WorkoutLogDTO());
        when(workoutExerciseMapper.countOwnedWorkoutExercise(2000L, 100L, 10L))
                .thenReturn(0);

        assertThat(service().add(10L, 100L, 2000L, null, 10, null)).isFalse();
        verify(workoutSetMapper, never()).findNextSetNo(any());
        verify(workoutSetMapper, never()).insertWorkoutSet(any());
    }

    @Test
    void missingWorkoutExerciseCannotReceiveSet() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(100L, 10L))
                .thenReturn(new WorkoutLogDTO());
        when(workoutExerciseMapper.countOwnedWorkoutExercise(9999L, 100L, 10L))
                .thenReturn(0);

        assertThat(service().add(10L, 100L, 9999L, null, 10, null)).isFalse();
        verify(workoutSetMapper, never()).insertWorkoutSet(any());
    }

    @Test
    void nullOrZeroRepsAreRejectedBeforeSetNumberQuery() {
        allowOwnedExercise(10L, 100L, 1000L);

        assertThatThrownBy(() ->
                service().add(10L, 100L, 1000L, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                service().add(10L, 100L, 1000L, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(workoutSetMapper, never()).findNextSetNo(any());
        verify(workoutSetMapper, never()).insertWorkoutSet(any());
    }

    @Test
    void repsOverOracleNumberThreeLimitAreRejected() {
        allowOwnedExercise(10L, 100L, 1000L);

        assertThatThrownBy(() ->
                service().add(10L, 100L, 1000L, null, 1000, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("반복 횟수는 1 이상 999 이하로 입력해 주세요.");
        verify(workoutSetMapper, never()).findNextSetNo(any());
        verify(workoutSetMapper, never()).insertWorkoutSet(any());
    }

    @Test
    void negativeWeightIsRejected() {
        allowOwnedExercise(10L, 100L, 1000L);

        assertThatThrownBy(() -> service().add(
                10L, 100L, 1000L, new BigDecimal("-0.01"), 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("중량은 0 이상 입력해 주세요.");
        verify(workoutSetMapper, never()).insertWorkoutSet(any());
    }

    @Test
    void ownSetCanBeRemovedWithEveryOwnershipValue() {
        allowOwnedExercise(10L, 100L, 1000L);
        when(workoutSetMapper.deleteWorkoutSet(5000L, 1000L, 100L, 10L))
                .thenReturn(1);

        assertThat(service().remove(10L, 100L, 1000L, 5000L)).isTrue();
        verify(workoutSetMapper).deleteWorkoutSet(5000L, 1000L, 100L, 10L);
    }

    @Test
    void anotherMembersSetCannotBeRemoved() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(999L, 10L))
                .thenReturn(null);

        assertThat(service().remove(10L, 999L, 1000L, 5000L)).isFalse();
        verify(workoutSetMapper, never())
                .deleteWorkoutSet(any(), any(), any(), any());
    }

    private void allowOwnedExercise(Long memberId, Long workoutLogId,
            Long workoutExerciseId) {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(workoutLogId, memberId))
                .thenReturn(new WorkoutLogDTO());
        when(workoutExerciseMapper.countOwnedWorkoutExercise(
                workoutExerciseId, workoutLogId, memberId)).thenReturn(1);
    }

    private WorkoutExerciseDTO workoutExercise(Long id) {
        WorkoutExerciseDTO exercise = new WorkoutExerciseDTO();
        exercise.setWorkoutExerciseId(id);
        return exercise;
    }

    private WorkoutSetDTO workoutSet(Integer setNo) {
        WorkoutSetDTO workoutSet = new WorkoutSetDTO();
        workoutSet.setSetNo(setNo);
        return workoutSet;
    }

    private WorkoutSetService service() {
        return new WorkoutSetService(
                workoutLogMapper, workoutExerciseMapper, workoutSetMapper);
    }
}
