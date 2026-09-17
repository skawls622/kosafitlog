package com.kosa.fitlog.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kosa.fitlog.routine.dto.RoutineDTO;
import com.kosa.fitlog.routine.mapper.RoutineMapper;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;

@ExtendWith(MockitoExtension.class)
class RoutineServiceTests {

    @Mock
    private RoutineMapper routineMapper;

    @Mock
    private RoutineExerciseMapper routineExerciseMapper;

    @Test
    void registerUsesMemberIdProvidedByController() {
        when(routineMapper.insertRoutine(any(RoutineDTO.class))).thenReturn(1);

        service().register(10L, "아침 루틴", "상체 운동");

        ArgumentCaptor<RoutineDTO> captor = ArgumentCaptor.forClass(RoutineDTO.class);
        verify(routineMapper).insertRoutine(captor.capture());
        assertThat(captor.getValue().getMemberId()).isEqualTo(10L);
        assertThat(captor.getValue().getRoutineName()).isEqualTo("아침 루틴");
    }

    @Test
    void blankRoutineNameIsRejectedBeforeInsert() {
        assertThatThrownBy(() -> service().register(10L, "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("루틴명을 입력해 주세요.");
        verify(routineMapper, never()).insertRoutine(any(RoutineDTO.class));
    }

    @Test
    void listUsesCurrentMemberId() {
        when(routineMapper.findAllByMemberId(10L)).thenReturn(Collections.emptyList());

        service().getRoutines(10L);

        verify(routineMapper).findAllByMemberId(10L);
    }

    @Test
    void detailUsesRoutineIdAndCurrentMemberId() {
        service().getRoutine(7L, 10L);

        verify(routineMapper).findByRoutineIdAndMemberId(7L, 10L);
    }

    @Test
    void ownRoutineCanBeModifiedWithMemberIdCondition() {
        when(routineMapper.updateRoutine(any(RoutineDTO.class))).thenReturn(1);

        boolean modified = service().modify(10L, 7L, "수정 루틴", "수정 설명");

        ArgumentCaptor<RoutineDTO> captor = ArgumentCaptor.forClass(RoutineDTO.class);
        verify(routineMapper).updateRoutine(captor.capture());
        assertThat(modified).isTrue();
        assertThat(captor.getValue().getRoutineId()).isEqualTo(7L);
        assertThat(captor.getValue().getMemberId()).isEqualTo(10L);
    }

    @Test
    void anotherMembersRoutineCannotBeModified() {
        when(routineMapper.updateRoutine(any(RoutineDTO.class))).thenReturn(0);

        boolean modified = service().modify(10L, 99L, "변경 시도", null);

        assertThat(modified).isFalse();
    }

    @Test
    void ownRoutineCanBeRemovedWithMemberIdCondition() {
        RoutineDTO routine = new RoutineDTO();
        when(routineMapper.findByRoutineIdAndMemberId(7L, 10L)).thenReturn(routine);
        when(routineMapper.deleteRoutine(7L, 10L)).thenReturn(1);

        boolean removed = service().remove(7L, 10L);

        assertThat(removed).isTrue();
        InOrder order = inOrder(routineExerciseMapper, routineMapper);
        order.verify(routineExerciseMapper).deleteAllByRoutineId(7L);
        order.verify(routineMapper).deleteRoutine(7L, 10L);
    }

    @Test
    void anotherMembersRoutineCannotBeRemoved() {
        when(routineMapper.findByRoutineIdAndMemberId(99L, 10L)).thenReturn(null);
        boolean removed = service().remove(99L, 10L);

        assertThat(removed).isFalse();
        verify(routineExerciseMapper, never()).deleteAllByRoutineId(99L);
        verify(routineMapper, never()).deleteRoutine(99L, 10L);
    }

    private RoutineService service() {
        return new RoutineService(routineMapper, routineExerciseMapper);
    }
}
