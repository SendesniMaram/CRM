package com.crm.customer.service;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ICustomerService {

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse getCustomerById(Long id);

    List<CustomerResponse> getAllCustomers();

    Page<CustomerResponse> getAllCustomersPaged(int page, int size, String sortBy, String direction, String keyword);

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    void deleteCustomer(Long id);
}

