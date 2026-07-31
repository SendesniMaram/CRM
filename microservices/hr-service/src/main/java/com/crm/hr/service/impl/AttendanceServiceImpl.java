package com.crm.hr.service.impl;

import com.crm.hr.dto.AttendanceRequest;
import com.crm.hr.dto.AttendanceResponse;
import com.crm.hr.entity.Attendance;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.mapper.AttendanceMapper;
import com.crm.hr.repository.AttendanceRepository;
import com.crm.hr.service.IAttendanceService;
import com.crm.hr.util.AttendanceSpecification;
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
public class AttendanceServiceImpl implements IAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, AttendanceMapper attendanceMapper) {
        this.attendanceRepository = attendanceRepository;
        this.attendanceMapper = attendanceMapper;
    }

    @Override
    public AttendanceResponse createAttendance(AttendanceRequest request) {
        // Check if attendance already exists for this employee on this date
        if (attendanceRepository.findByEmployeeIdAndDate(request.getEmployeeId(), request.getDate()).isPresent()) {
            throw new IllegalArgumentException("Attendance already exists for employee "
                    + request.getEmployeeId() + " on date " + request.getDate());
        }

        Attendance attendance = attendanceMapper.toEntity(request);
        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public AttendanceResponse getAttendanceById(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
        return attendanceMapper.toResponse(attendance);
    }

    @Override
    public List<AttendanceResponse> getAllAttendances() {
        return attendanceRepository.findAll().stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    @Override
    public Page<AttendanceResponse> getAllAttendancesPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Attendance> attendancePage;

        if (StringUtils.hasText(keyword)) {
            attendancePage = attendanceRepository.findAll(
                    AttendanceSpecification.searchByKeyword(keyword), pageable);
        } else {
            attendancePage = attendanceRepository.findAll(pageable);
        }

        return attendancePage.map(attendanceMapper::toResponse);
    }

    @Override
    public AttendanceResponse updateAttendance(Long id, AttendanceRequest request) {
        Attendance existing = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));

        // Check for duplicate on date change
        if (!existing.getDate().equals(request.getDate())
                || !existing.getEmployeeId().equals(request.getEmployeeId())) {
            if (attendanceRepository.findByEmployeeIdAndDate(
                    request.getEmployeeId(), request.getDate()).isPresent()) {
                throw new IllegalArgumentException("Attendance already exists for employee "
                        + request.getEmployeeId() + " on date " + request.getDate());
            }
        }

        attendanceMapper.updateEntityFromRequest(existing, request);
        Attendance saved = attendanceRepository.save(existing);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public void deleteAttendance(Long id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance not found with id: " + id));
        attendanceRepository.delete(attendance);
    }
}
