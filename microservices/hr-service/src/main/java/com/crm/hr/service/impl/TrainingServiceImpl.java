package com.crm.hr.service.impl;

import com.crm.hr.dto.TrainingRequest;
import com.crm.hr.dto.TrainingResponse;
import com.crm.hr.entity.Training;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.mapper.TrainingMapper;
import com.crm.hr.repository.TrainingRepository;
import com.crm.hr.service.ITrainingService;
import com.crm.hr.util.TrainingSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class TrainingServiceImpl implements ITrainingService {

    private final TrainingRepository trainingRepository;
    private final TrainingMapper trainingMapper;

    public TrainingServiceImpl(TrainingRepository trainingRepository, TrainingMapper trainingMapper) {
        this.trainingRepository = trainingRepository;
        this.trainingMapper = trainingMapper;
    }

    @Override
    public TrainingResponse createTraining(TrainingRequest request) {
        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        // Validate status on create
        String status = request.getStatus();
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        Training.TrainingStatus trainingStatus;
        try {
            trainingStatus = Training.TrainingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status + ". Allowed values: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED");
        }

        // certificateObtained can only be true if status is COMPLETED
        if (trainingStatus == Training.TrainingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot create a training with COMPLETED status. Use update to mark as completed.");
        }

        Training training = trainingMapper.toEntity(request);
        training.setStatus(trainingStatus);

        // Force certificateObtained to false if not COMPLETED
        if (trainingStatus != Training.TrainingStatus.COMPLETED) {
            training.setCertificateObtained(false);
        }

        Training saved = trainingRepository.save(training);
        return trainingMapper.toResponse(saved);
    }

    @Override
    public TrainingResponse getTrainingById(Long id) {
        Training training = trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training not found with id: " + id));
        return trainingMapper.toResponse(training);
    }

    @Override
    public List<TrainingResponse> getAllTrainings() {
        return trainingRepository.findAll().stream()
                .map(trainingMapper::toResponse)
                .toList();
    }

    @Override
    public Page<TrainingResponse> getAllTrainingsPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Training> trainingPage;

        if (StringUtils.hasText(keyword)) {
            trainingPage = trainingRepository.findAll(
                    TrainingSpecification.searchByKeyword(keyword), pageable);
        } else {
            trainingPage = trainingRepository.findAll(pageable);
        }

        return trainingPage.map(trainingMapper::toResponse);
    }

    @Override
    public TrainingResponse updateTraining(Long id, TrainingRequest request) {
        Training existing = trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training not found with id: " + id));

        // Prevent modification of COMPLETED trainings
        if (existing.getStatus() == Training.TrainingStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot modify a completed training");
        }

        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after or equal to start date");
        }

        // Parse and validate status
        String newStatus = request.getStatus();
        Training.TrainingStatus trainingStatus;
        try {
            trainingStatus = Training.TrainingStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Allowed values: PLANNED, IN_PROGRESS, COMPLETED, CANCELLED");
        }

        trainingMapper.updateEntityFromRequest(existing, request);
        existing.setStatus(trainingStatus);

        // certificateObtained can only be true if status is COMPLETED
        // If status is not COMPLETED, force certificateObtained to false
        if (trainingStatus != Training.TrainingStatus.COMPLETED) {
            existing.setCertificateObtained(false);
        }

        Training saved = trainingRepository.save(existing);
        return trainingMapper.toResponse(saved);
    }

    @Override
    public void deleteTraining(Long id) {
        Training training = trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Training not found with id: " + id));
        trainingRepository.delete(training);
    }
}
