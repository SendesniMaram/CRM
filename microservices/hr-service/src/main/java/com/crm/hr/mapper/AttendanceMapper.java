package com.crm.hr.mapper;

import com.crm.hr.dto.AttendanceRequest;
import com.crm.hr.dto.AttendanceResponse;
import com.crm.hr.entity.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(Attendance attendance) {
        if (attendance == null) {
            return null;
        }

        AttendanceResponse response = new AttendanceResponse();
        response.setId(attendance.getId());
        response.setEmployeeId(attendance.getEmployeeId());
        response.setDate(attendance.getDate());
        response.setCheckIn(attendance.getCheckIn());
        response.setCheckOut(attendance.getCheckOut());
        response.setStatus(attendance.getStatus() != null ? attendance.getStatus().name() : null);
        response.setNotes(attendance.getNotes());
        response.setCreatedAt(attendance.getCreatedAt());
        response.setUpdatedAt(attendance.getUpdatedAt());

        return response;
    }

    public Attendance toEntity(AttendanceRequest request) {
        if (request == null) {
            return null;
        }

        Attendance attendance = new Attendance();
        attendance.setEmployeeId(request.getEmployeeId());
        attendance.setDate(request.getDate());
        attendance.setCheckIn(request.getCheckIn());
        attendance.setCheckOut(request.getCheckOut());
        if (request.getStatus() != null) {
            attendance.setStatus(Attendance.AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
        }
        attendance.setNotes(request.getNotes());

        return attendance;
    }

    public void updateEntityFromRequest(Attendance attendance, AttendanceRequest request) {
        if (request == null) {
            return;
        }
        attendance.setEmployeeId(request.getEmployeeId());
        attendance.setDate(request.getDate());
        attendance.setCheckIn(request.getCheckIn());
        attendance.setCheckOut(request.getCheckOut());
        if (request.getStatus() != null) {
            attendance.setStatus(Attendance.AttendanceStatus.valueOf(request.getStatus().toUpperCase()));
        }
        attendance.setNotes(request.getNotes());
    }
}
