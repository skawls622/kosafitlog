package com.kosa.fitlog.statistics.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.kosa.fitlog.statistics.dto.StatisticsDTO;
import com.kosa.fitlog.statistics.mapper.StatisticsMapper;

@Service
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    public StatisticsService(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    public List<StatisticsDTO> findExerciseStatistics(Long memberId) {
        return statisticsMapper.findExerciseStatistics(memberId);
    }

}
