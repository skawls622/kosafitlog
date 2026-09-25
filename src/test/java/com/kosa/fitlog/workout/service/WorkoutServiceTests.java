package com.kosa.fitlog.workout.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;
import com.kosa.fitlog.routine.mapper.RoutineMapper;
import com.kosa.fitlog.workout.dto.WorkoutExerciseDTO;
import com.kosa.fitlog.workout.dto.WorkoutLogDTO;
import com.kosa.fitlog.workout.mapper.WorkoutExerciseMapper;
import com.kosa.fitlog.workout.mapper.WorkoutLogMapper;

@ExtendWith(MockitoExtension.class)
class WorkoutServiceTests {

    @Mock private RoutineMapper routineMapper;
    @Mock private RoutineExerciseMapper routineExerciseMapper;
    @Mock private WorkoutLogMapper workoutLogMapper;
    @Mock private WorkoutExerciseMapper workoutExerciseMapper;

    @Test
    void ownRoutineCreatesLogAndCopiesEveryExerciseWithoutRenumbering() {
        RoutineExerciseDTO first = routineExercise(11L, 2, "천천히");
        RoutineExerciseDTO second = routineExercise(22L, 4, null);
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L))
                .thenReturn(new RoutineDTO());
        when(routineExerciseMapper.findByRoutineId(7L))
                .thenReturn(Arrays.asList(first, second));
        when(workoutLogMapper.insertWorkoutLog(any())).thenAnswer(invocation -> {
            WorkoutLogDTO log = invocation.getArgument(0);
            log.setWorkoutLogId(100L);
            return 1;
        });
        when(workoutExerciseMapper.insertWorkoutExercise(any())).thenReturn(1);

        Long workoutLogId = service().startWorkout(10L, 7L);

        assertThat(workoutLogId).isEqualTo(100L);
        ArgumentCaptor<WorkoutLogDTO> logCaptor =
                ArgumentCaptor.forClass(WorkoutLogDTO.class);
        verify(workoutLogMapper).insertWorkoutLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getMemberId()).isEqualTo(10L);

        ArgumentCaptor<WorkoutExerciseDTO> exerciseCaptor =
                ArgumentCaptor.forClass(WorkoutExerciseDTO.class);
        verify(workoutExerciseMapper, times(2))
                .insertWorkoutExercise(exerciseCaptor.capture());
        List<WorkoutExerciseDTO> copied = exerciseCaptor.getAllValues();
        assertThat(copied).extracting(WorkoutExerciseDTO::getWorkoutLogId)
                .containsExactly(100L, 100L);
        assertThat(copied).extracting(WorkoutExerciseDTO::getExerciseId)
                .containsExactly(11L, 22L);
        assertThat(copied).extracting(WorkoutExerciseDTO::getExerciseOrder)
                .containsExactly(2, 4);
        assertThat(copied).extracting(WorkoutExerciseDTO::getMemo)
                .containsExactly("천천히", null);
    }

    @Test
    void anotherMembersOrMissingRoutineCannotStartWorkout() {
        when(routineMapper.findByRoutineIdAndMemberId(99L, 10L)).thenReturn(null);

        assertThat(service().startWorkout(10L, 99L)).isNull();
        verify(routineExerciseMapper, never()).findByRoutineId(any());
        verify(workoutLogMapper, never()).insertWorkoutLog(any());
    }

    @Test
    void emptyRoutineDoesNotCreateWorkoutLog() {
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L))
                .thenReturn(new RoutineDTO());
        when(routineExerciseMapper.findByRoutineId(7L))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service().startWorkout(10L, 7L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("운동이 등록되지 않은 루틴입니다.");
        verify(workoutLogMapper, never()).insertWorkoutLog(any());
        verify(workoutExerciseMapper, never()).insertWorkoutExercise(any());
    }

    @Test
    void childInsertFailureEscapesTransactionalMethodForRollback() throws Exception {
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L))
                .thenReturn(new RoutineDTO());
        when(routineExerciseMapper.findByRoutineId(7L))
                .thenReturn(Arrays.asList(routineExercise(11L, 1, null)));
        when(workoutLogMapper.insertWorkoutLog(any())).thenAnswer(invocation -> {
            WorkoutLogDTO log = invocation.getArgument(0);
            log.setWorkoutLogId(100L);
            return 1;
        });
        when(workoutExerciseMapper.insertWorkoutExercise(any()))
                .thenThrow(new IllegalStateException("DB 오류"));

        assertThatThrownBy(() -> service().startWorkout(10L, 7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DB 오류");
        assertThat(WorkoutService.class
                .getMethod("startWorkout", Long.class, Long.class)
                .isAnnotationPresent(Transactional.class)).isTrue();
    }

    @Test
    void workoutLogOwnershipLookupUsesBothIds() {
        WorkoutLogDTO log = new WorkoutLogDTO();
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(100L, 10L))
                .thenReturn(log);

        assertThat(service().getWorkoutLog(100L, 10L)).isSameAs(log);
        verify(workoutLogMapper).findByWorkoutLogIdAndMemberId(100L, 10L);
    }

    @Test
    void joinedWorkoutExercisesAreReturnedForRecordScreen() {
        WorkoutExerciseDTO exercise = new WorkoutExerciseDTO();
        exercise.setExerciseName("벤치프레스");
        when(workoutExerciseMapper.findByWorkoutLogId(100L))
                .thenReturn(Arrays.asList(exercise));

        assertThat(service().getWorkoutExercises(100L)).containsExactly(exercise);
    }

    @Test
    void ownWorkoutCanBeCompletedWithBothOwnershipValues() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(100L, 10L))
                .thenReturn(new WorkoutLogDTO());
        when(workoutLogMapper.completeWorkout(100L, 10L)).thenReturn(1);

        assertThat(service().completeWorkout(100L, 10L)).isTrue();
        verify(workoutLogMapper).findByWorkoutLogIdAndMemberId(100L, 10L);
        verify(workoutLogMapper).completeWorkout(100L, 10L);
    }

    @Test
    void anotherMembersWorkoutCannotBeCompleted() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(100L, 99L))
                .thenReturn(null);

        assertThat(service().completeWorkout(100L, 99L)).isFalse();
        verify(workoutLogMapper, never()).completeWorkout(any(), any());
    }

    @Test
    void missingWorkoutCannotBeCompleted() {
        when(workoutLogMapper.findByWorkoutLogIdAndMemberId(999L, 10L))
                .thenReturn(null);

        assertThat(service().completeWorkout(999L, 10L)).isFalse();
        verify(workoutLogMapper, never()).completeWorkout(any(), any());
    }

    @Test
    void workoutListUsesMemberIdMapperCondition() {
        WorkoutLogDTO log = new WorkoutLogDTO();
        when(workoutLogMapper.findAllByMemberId(10L))
                .thenReturn(Arrays.asList(log));

        assertThat(service().getWorkoutLogs(10L)).containsExactly(log);
        verify(workoutLogMapper).findAllByMemberId(10L);
    }

    @Test
    void completedDatesAndAllWorkoutsForOneDateStayScopedToMember() {
        LocalDate date = LocalDate.of(2026, 9, 23);
        WorkoutLogDTO first = new WorkoutLogDTO();
        WorkoutLogDTO second = new WorkoutLogDTO();
        when(workoutLogMapper.findCompletedWorkoutDates(10L))
                .thenReturn(Arrays.asList(date));
        when(workoutLogMapper.findCompletedByMemberIdAndDate(10L, date))
                .thenReturn(Arrays.asList(first, second));

        assertThat(service().getCompletedWorkoutDates(10L)).containsExactly(date);
        assertThat(service().getCompletedWorkoutLogsByDate(10L, date))
                .containsExactly(first, second);
        verify(workoutLogMapper).findCompletedWorkoutDates(10L);
        verify(workoutLogMapper).findCompletedByMemberIdAndDate(10L, date);
    }

    private RoutineExerciseDTO routineExercise(Long exerciseId,
            Integer exerciseOrder, String memo) {
        RoutineExerciseDTO exercise = new RoutineExerciseDTO();
        exercise.setExerciseId(exerciseId);
        exercise.setExerciseOrder(exerciseOrder);
        exercise.setMemo(memo);
        return exercise;
    }

    private WorkoutService service() {
        return new WorkoutService(routineMapper, routineExerciseMapper,
                workoutLogMapper, workoutExerciseMapper);
    }
}
