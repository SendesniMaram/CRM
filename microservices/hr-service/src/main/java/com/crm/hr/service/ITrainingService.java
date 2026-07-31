package com.crm.hr.service;

import com.crm.hr.dto.TrainingRequest;
import com.crm.hr.dto.TrainingResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ITrainingService {

    TrainingResponse createTraining(TrainingRequest request);

    TrainingResponse getTrainingById(Long id);

    List<TrainingResponse> getAllTrainings();

    Page<TrainingResponse> getAllTrainingsPaged(int page, int size, String sortBy, String direction, String keyword);

    TrainingResponse updateTraining(Long id, TrainingRequest request);

    void deleteTraining(Long id);
}
