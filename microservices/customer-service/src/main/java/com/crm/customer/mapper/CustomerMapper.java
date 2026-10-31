package com.crm.customer.mapper;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public CustomerResponse toResponse(Customer customer) {
        if (customer == null) {
            return null;
        }

        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setEmail(customer.getEmail());
        response.setPhone(customer.getPhone());
        response.setCompany(customer.getCompany());
        response.setAddress(customer.getAddress());
        response.setContactType(customer.getContactType());
        response.setCategory(customer.getCategory());
        response.setSegment(customer.getSegment());
        response.setAssignedEmployeeId(customer.getAssignedEmployeeId());
        response.setCreatedAt(customer.getCreatedAt());
        response.setUpdatedAt(customer.getUpdatedAt());

        return response;
    }

    public Customer toEntity(CustomerRequest request) {
        if (request == null) {
            return null;
        }

        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setCompany(request.getCompany());
        customer.setAddress(request.getAddress());
        customer.setContactType(request.getContactType() == null ? "CUSTOMER" : request.getContactType());
        customer.setCategory(request.getCategory());
        customer.setSegment(request.getSegment());
        customer.setAssignedEmployeeId(request.getAssignedEmployeeId());

        return customer;
    }

    public void updateEntityFromRequest(Customer customer, CustomerRequest request) {
        if (request == null) {
            return;
        }
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setCompany(request.getCompany());
        customer.setAddress(request.getAddress());
        if (request.getContactType() != null) customer.setContactType(request.getContactType());
        customer.setCategory(request.getCategory());
        customer.setSegment(request.getSegment());
        customer.setAssignedEmployeeId(request.getAssignedEmployeeId());
    }
}

