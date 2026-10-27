package com.crm.hr.service;

import com.crm.hr.client.LeaveDirectoryClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Component;

/** Fails closed if Employee Service is unavailable or the reference is not real. */
@Component
public class EmployeeReferenceValidator {
    private final LeaveDirectoryClient directory;
    public EmployeeReferenceValidator(LeaveDirectoryClient directory) { this.directory = directory; }
    public void requireExists(String employeeId, String authorization) {
        long expected=EmployeeAssociationService.parseId(employeeId);
        JsonNode employee=directory.read("employee-service", "/api/employees/"+employeeId, authorization);
        if(employee==null||!employee.isObject()||!employee.path("id").canConvertToLong()||employee.path("id").longValue()!=expected)
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Invalid employee reference response");
    }
}
