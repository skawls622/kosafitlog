package com.kosa.fitlog.exercise.service;

import org.springframework.stereotype.Service;

import com.kosa.fitlog.exercise.dto.ExerciseDTO;
import com.kosa.fitlog.exercise.mapper.ExerciseMapper;

@Service
public class ExerciseService {

    private final ExerciseMapper exerciseMapper;

    public ExerciseService(ExerciseMapper exerciseMapper) {
        this.exerciseMapper = exerciseMapper;
    }

    public ExerciseDTO findById(Long exerciseId) {
        return exerciseMapper.findById(exerciseId);
    }
}