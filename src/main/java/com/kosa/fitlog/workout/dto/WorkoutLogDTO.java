package com.kosa.fitlog.workout.dto;

import java.time.LocalDateTime;

public class WorkoutLogDTO {

    private Long workoutLogId;
    private Long memberId;
    private LocalDateTime workoutDate;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getWorkoutLogId() { return workoutLogId; }
    public void setWorkoutLogId(Long workoutLogId) { this.workoutLogId = workoutLogId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public LocalDateTime getWorkoutDate() { return workoutDate; }
    public void setWorkoutDate(LocalDateTime workoutDate) { this.workoutDate = workoutDate; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
