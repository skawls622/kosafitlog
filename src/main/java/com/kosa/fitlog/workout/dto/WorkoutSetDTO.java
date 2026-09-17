package com.kosa.fitlog.workout.dto;

import java.math.BigDecimal;

public class WorkoutSetDTO {

    private Long workoutSetId;
    private Long workoutExerciseId;
    private Integer setNo;
    private BigDecimal weight;
    private Integer reps;
    private String memo;

    public Long getWorkoutSetId() { return workoutSetId; }
    public void setWorkoutSetId(Long workoutSetId) { this.workoutSetId = workoutSetId; }
    public Long getWorkoutExerciseId() { return workoutExerciseId; }
    public void setWorkoutExerciseId(Long workoutExerciseId) { this.workoutExerciseId = workoutExerciseId; }
    public Integer getSetNo() { return setNo; }
    public void setSetNo(Integer setNo) { this.setNo = setNo; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getReps() { return reps; }
    public void setReps(Integer reps) { this.reps = reps; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
