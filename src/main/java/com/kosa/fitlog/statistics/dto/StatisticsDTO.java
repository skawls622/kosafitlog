package com.kosa.fitlog.statistics.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StatisticsDTO {

    private LocalDate workoutDate;

    private Long exerciseId;

    private String exerciseName;

    private String bodyPart;

    private Double totalVolume;

    private Double maxWeight;

    private Double estimated1Rm;

    private Integer totalReps;

    private Integer totalSets;
}