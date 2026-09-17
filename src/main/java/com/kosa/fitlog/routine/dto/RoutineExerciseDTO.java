package com.kosa.fitlog.routine.dto;

public class RoutineExerciseDTO {

    private Long routineExerciseId;
    private Long routineId;
    private Long exerciseId;
    private String exerciseName;
    private String bodyPart;
    private Integer exerciseOrder;
    private String memo;

    public Long getRoutineExerciseId() { return routineExerciseId; }
    public void setRoutineExerciseId(Long routineExerciseId) { this.routineExerciseId = routineExerciseId; }
    public Long getRoutineId() { return routineId; }
    public void setRoutineId(Long routineId) { this.routineId = routineId; }
    public Long getExerciseId() { return exerciseId; }
    public void setExerciseId(Long exerciseId) { this.exerciseId = exerciseId; }
    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }
    public String getBodyPart() { return bodyPart; }
    public void setBodyPart(String bodyPart) { this.bodyPart = bodyPart; }
    public Integer getExerciseOrder() { return exerciseOrder; }
    public void setExerciseOrder(Integer exerciseOrder) { this.exerciseOrder = exerciseOrder; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
