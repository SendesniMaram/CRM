package com.crm.fees.mapper;

import java.math.BigDecimal;
import com.crm.fees.dto.FeeRequest;
import com.crm.fees.dto.FeeResponse;
import com.crm.fees.entity.Fee;
import org.springframework.stereotype.Component;

@Component
public class FeeMapper {

    public FeeResponse toResponse(Fee fee) {
        if (fee == null) {
            return null;
        }

        FeeResponse response = new FeeResponse();
        response.setId(fee.getId());
        response.setEmployeeId(fee.getEmployeeId());
        response.setCustomerId(fee.getCustomerId());
        response.setServiceName(fee.getServiceName());
        response.setDescription(fee.getDescription());
        response.setHourlyRate(fee.getHourlyRate());
        response.setWorkedHours(fee.getWorkedHours() != null ? BigDecimal.valueOf(fee.getWorkedHours()) : null);
        response.setAmount(fee.getAmount());
        response.setInvoiceStatus(fee.getInvoiceStatus());
        response.setPaymentStatus(fee.getPaymentStatus());
        response.setWorkDate(fee.getWorkDate());
        response.setCreatedAt(fee.getCreatedAt());
        response.setUpdatedAt(fee.getUpdatedAt());

        return response;
    }

    public Fee toEntity(FeeRequest request) {
        if (request == null) {
            return null;
        }

        Fee fee = new Fee();
        fee.setEmployeeId(request.getEmployeeId());
        fee.setCustomerId(request.getCustomerId());
        fee.setServiceName(request.getServiceName());
        fee.setDescription(request.getDescription());
        fee.setHourlyRate(request.getHourlyRate());
        if (request.getWorkedHours() != null) {
            fee.setWorkedHours(request.getWorkedHours().doubleValue());
        }
        fee.setInvoiceStatus(request.getInvoiceStatus());
        fee.setPaymentStatus(request.getPaymentStatus());
        fee.setWorkDate(request.getWorkDate());

        return fee;
    }

    public void updateEntityFromRequest(Fee fee, FeeRequest request) {
        if (request == null) {
            return;
        }
        fee.setEmployeeId(request.getEmployeeId());
        fee.setCustomerId(request.getCustomerId());
        fee.setServiceName(request.getServiceName());
        fee.setDescription(request.getDescription());
        fee.setHourlyRate(request.getHourlyRate());
        if (request.getWorkedHours() != null) {
            fee.setWorkedHours(request.getWorkedHours().doubleValue());
        }
        fee.setInvoiceStatus(request.getInvoiceStatus());
        fee.setPaymentStatus(request.getPaymentStatus());
        fee.setWorkDate(request.getWorkDate());
    }
}
