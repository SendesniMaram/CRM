package com.crm.hr.service;

import com.crm.hr.dto.AttendanceRequest;
import com.crm.hr.dto.AttendanceResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IAttendanceService {

    AttendanceResponse createAttendance(AttendanceRequest request);

    AttendanceResponse getAttendanceById(Long id);

    List<AttendanceResponse> getAllAttendances();

    Page<AttendanceResponse> getAllAttendancesPaged(int page, int size, String sortBy, String direction, String keyword);

    AttendanceResponse updateAttendance(Long id, AttendanceRequest request);

    void deleteAttendance(Long id);
}
