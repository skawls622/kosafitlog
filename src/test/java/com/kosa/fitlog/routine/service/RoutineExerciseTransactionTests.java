package com.kosa.fitlog.routine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.mapper.ExerciseMapper;
import com.kosa.fitlog.routine.dto.RoutineExerciseDTO;
import com.kosa.fitlog.routine.mapper.RoutineExerciseMapper;
import com.kosa.fitlog.routine.mapper.RoutineMapper;

class RoutineExerciseTransactionTests {
    private RoutineExerciseService service;
    private RoutineExerciseMapper mapper;
    private MemoryTransactionManager transactionManager;
    private final List<Long> rows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        RoutineMapper routines = mock(RoutineMapper.class);
        mapper = mock(RoutineExerciseMapper.class);
        ExerciseMapper exercises = mock(ExerciseMapper.class);
        rows.clear();
        rows.add(99L);
        when(routines.lockOwnedRoutine(7L, 10L)).thenReturn(7L);
        when(mapper.findByRoutineId(7L)).thenReturn(Collections.emptyList());
        when(mapper.findNextExerciseOrder(7L)).thenReturn(4);
        when(exercises.findById(any())).thenReturn(new ExerciseDTO());
        transactionManager = new MemoryTransactionManager(rows);
        ProxyFactory factory = new ProxyFactory(new RoutineExerciseService(routines, mapper, exercises));
        factory.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        service = (RoutineExerciseService) factory.getProxy();
    }

    @Test
    void wholeSelectionCommitsInOneTransaction() {
        when(mapper.insertRoutineExercise(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            rows.add(((RoutineExerciseDTO) invocation.getArgument(0)).getExerciseId());
            return 1;
        });
        assertThat(service.add(10L, 7L, Arrays.asList(3L, 1L), Collections.emptyMap())).isTrue();
        assertThat(rows).containsExactly(99L, 3L, 1L);
        assertThat(transactionManager.begins).isEqualTo(1);
        assertThat(transactionManager.commits).isEqualTo(1);
        assertThat(transactionManager.rollbacks).isZero();
    }

    @Test
    void secondInsertExceptionRollsBackFirstInsert() {
        when(mapper.insertRoutineExercise(any())).thenAnswer(invocation -> {
            RoutineExerciseDTO dto = invocation.getArgument(0);
            if (dto.getExerciseId().equals(1L)) throw new DataIntegrityViolationException("second insert failed");
            rows.add(dto.getExerciseId());
            return 1;
        });
        assertThatThrownBy(() -> service.add(10L, 7L, Arrays.asList(3L, 1L), Collections.emptyMap()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(rows).containsExactly(99L);
        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    void zeroRowsOnSecondInsertAlsoRollsBackFirstInsert() {
        when(mapper.insertRoutineExercise(any())).thenAnswer(invocation -> {
            RoutineExerciseDTO dto = invocation.getArgument(0);
            if (dto.getExerciseId().equals(1L)) return 0;
            rows.add(dto.getExerciseId());
            return 1;
        });
        assertThatThrownBy(() -> service.add(10L, 7L, Arrays.asList(3L, 1L), Collections.emptyMap()))
                .isInstanceOf(IllegalStateException.class);
        assertThat(rows).containsExactly(99L);
        assertThat(transactionManager.commits).isZero();
        assertThat(transactionManager.rollbacks).isEqualTo(1);
    }

    @Test
    void oracleLockQueryIncludesOwnershipAndBindsBothIds() throws Exception {
        Configuration configuration = new Configuration();
        try (InputStream xml = Resources.getResourceAsStream("mapper/RoutineMapper.xml")) {
            new XMLMapperBuilder(xml, configuration, "mapper/RoutineMapper.xml", configuration.getSqlFragments()).parse();
        }
        BoundSql sql = configuration.getMappedStatement("com.kosa.fitlog.routine.mapper.RoutineMapper.lockOwnedRoutine")
                .getBoundSql(Map.of("routineId", 7L, "memberId", 10L));
        assertThat(sql.getSql().replaceAll("\\s+", " ").trim())
                .isEqualTo("SELECT routine_id FROM routines WHERE routine_id = ? AND member_id = ? FOR UPDATE");
        assertThat(sql.getParameterMappings()).extracting(ParameterMapping::getProperty)
                .containsExactly("routineId", "memberId");
    }

    // Oracle 접속 없이 실제 Spring 트랜잭션 advice의 commit/rollback 경계를 검증하는 테스트 자원이다.
    private static class MemoryTransactionManager extends AbstractPlatformTransactionManager {
        private final List<Long> rows;
        private List<Long> before;
        private int begins;
        private int commits;
        private int rollbacks;

        MemoryTransactionManager(List<Long> rows) { this.rows = rows; }
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            before = new ArrayList<>(rows);
            begins++;
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            rows.clear();
            rows.addAll(before);
            rollbacks++;
        }
    }
}
