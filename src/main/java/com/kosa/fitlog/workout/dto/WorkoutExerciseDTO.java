package com.kosa.fitlog.workout.dto;

public class WorkoutExerciseDTO {

    private Long workoutExerciseId;
    private Long workoutLogId;
    private Long exerciseId;
    private String exerciseName;
    private String bodyPart;
    private Integer exerciseOrder;
    private String memo;

    public Long getWorkoutExerciseId() { return workoutExerciseId; }
    public void setWorkoutExerciseId(Long workoutExerciseId) { this.workoutExerciseId = workoutExerciseId; }
    public Long getWorkoutLogId() { return workoutLogId; }
    public void setWorkoutLogId(Long workoutLogId) { this.workoutLogId = workoutLogId; }
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
