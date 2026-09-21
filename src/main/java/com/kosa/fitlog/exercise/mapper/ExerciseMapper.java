package com.kosa.fitlog.exercise.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.kosa.fitlog.exercise.dto.ExerciseDTO;

@Mapper
public interface ExerciseMapper {

	List<ExerciseDTO> findAll();

	ExerciseDTO findById(@Param("exerciseId") Long exerciseId);
}
