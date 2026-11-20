package com.crm.hr.mapper;

import com.crm.hr.dto.HrRequest;
import com.crm.hr.dto.HrResponse;
import com.crm.hr.entity.HrRecord;
import org.springframework.stereotype.Component;

@Component
public class HrMapper {

    public HrResponse toResponse(HrRecord record) {
        if (record == null) {
            return null;
        }

        HrResponse response = new HrResponse();
        response.setId(record.getId());
        response.setEmployeeId(record.getEmployeeId());
        response.setContractType(record.getContractType());
        response.setHireDate(record.getHireDate());
        response.setJobTitle(record.getJobTitle());
        response.setSalaryGrade(record.getSalaryGrade());
        response.setManager(record.getManager());
        response.setWorkLocation(record.getWorkLocation());
        response.setEmploymentStatus(record.getEmploymentStatus());
        response.setCreatedAt(record.getCreatedAt());
        response.setUpdatedAt(record.getUpdatedAt());

        return response;
    }

    public HrRecord toEntity(HrRequest request) {
        if (request == null) {
            return null;
        }

        HrRecord record = new HrRecord();
        record.setEmployeeId(request.getEmployeeId());
        record.setContractType(request.getContractType());
        record.setHireDate(request.getHireDate());
        record.setJobTitle(request.getJobTitle());
        record.setSalaryGrade(request.getSalaryGrade());
        record.setManager(request.getManager());
        record.setWorkLocation(request.getWorkLocation());
        record.setEmploymentStatus(request.getEmploymentStatus());

        return record;
    }

    public void updateEntityFromRequest(HrRecord record, HrRequest request) {
        if (request == null) {
            return;
        }
        record.setEmployeeId(request.getEmployeeId());
        record.setContractType(request.getContractType());
        record.setHireDate(request.getHireDate());
        record.setJobTitle(request.getJobTitle());
        record.setSalaryGrade(request.getSalaryGrade());
        record.setManager(request.getManager());
        record.setWorkLocation(request.getWorkLocation());
        record.setEmploymentStatus(request.getEmploymentStatus());
    }
}
