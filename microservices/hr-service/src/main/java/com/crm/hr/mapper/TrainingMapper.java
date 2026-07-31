package com.crm.hr.mapper;

import com.crm.hr.dto.TrainingRequest;
import com.crm.hr.dto.TrainingResponse;
import com.crm.hr.entity.Training;
import org.springframework.stereotype.Component;

@Component
public class TrainingMapper {

    public TrainingResponse toResponse(Training training) {
        if (training == null) {
            return null;
        }

        TrainingResponse response = new TrainingResponse();
        response.setId(training.getId());
        response.setEmployeeId(training.getEmployeeId());
        response.setTitle(training.getTitle());
        response.setProvider(training.getProvider());
        response.setStartDate(training.getStartDate());
        response.setEndDate(training.getEndDate());
        response.setCost(training.getCost());
        response.setStatus(training.getStatus() != null ? training.getStatus().name() : null);
        response.setCertificateObtained(training.getCertificateObtained());
        response.setCreatedAt(training.getCreatedAt());
        response.setUpdatedAt(training.getUpdatedAt());

        return response;
    }

    public Training toEntity(TrainingRequest request) {
        if (request == null) {
            return null;
        }

        Training training = new Training();
        training.setEmployeeId(request.getEmployeeId());
        training.setTitle(request.getTitle());
        training.setProvider(request.getProvider());
        training.setStartDate(request.getStartDate());
        training.setEndDate(request.getEndDate());
        training.setCost(request.getCost());
        training.setStatus(Training.TrainingStatus.valueOf(request.getStatus()));
        training.setCertificateObtained(false);

        return training;
    }

    public void updateEntityFromRequest(Training training, TrainingRequest request) {
        if (request == null) {
            return;
        }
        training.setEmployeeId(request.getEmployeeId());
        training.setTitle(request.getTitle());
        training.setProvider(request.getProvider());
        training.setStartDate(request.getStartDate());
        training.setEndDate(request.getEndDate());
        training.setCost(request.getCost());
    }
}
