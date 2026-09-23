package com.kosa.fitlog.bodycomposition.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BodyCompositionDTO {

    private Long bodyCompositionId;
    private Long memberId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate measuredDate;

    private Double weight;
    private Double skeletalMuscle;
    private Double bodyFatPercentage;
    private LocalDateTime createdAt;
}
