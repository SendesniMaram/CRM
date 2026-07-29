package com.crm.customer.service.impl;

import com.crm.customer.dto.CustomerRequest;
import com.crm.customer.dto.CustomerResponse;
import com.crm.customer.entity.Customer;
import com.crm.customer.exception.ResourceNotFoundException;
import com.crm.customer.mapper.CustomerMapper;
import com.crm.customer.repository.CustomerRepository;
import com.crm.customer.service.ICustomerService;
import com.crm.customer.util.CustomerSpecification;
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
public class CustomerServiceImpl implements ICustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                                CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Customer name already exists: " + request.getName());
        }
        if (StringUtils.hasText(request.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Customer email already exists: " + request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())
                && customerRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Customer phone already exists: " + request.getPhone());
        }

        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);
        return customerMapper.toResponse(saved);
    }

    @Override
    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        return customerMapper.toResponse(customer);
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAll().stream()
                .map(customerMapper::toResponse)
                .toList();
    }

    @Override
    public Page<CustomerResponse> getAllCustomersPaged(int page, int size, String sortBy, String direction, String keyword) {
        String sortField = StringUtils.hasText(sortBy) ? sortBy : "id";
        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(sortDirection, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Customer> customerPage;

        if (StringUtils.hasText(keyword)) {
            customerPage = customerRepository.findAll(
                    CustomerSpecification.searchByKeyword(keyword), pageable);
        } else {
            customerPage = customerRepository.findAll(pageable);
        }

        return customerPage.map(customerMapper::toResponse);
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (!existing.getName().equals(request.getName())
                && customerRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Customer name already exists: " + request.getName());
        }
        if (StringUtils.hasText(request.getEmail())
                && !request.getEmail().equals(existing.getEmail())
                && customerRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Customer email already exists: " + request.getEmail());
        }
        if (StringUtils.hasText(request.getPhone())
                && !request.getPhone().equals(existing.getPhone())
                && customerRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("Customer phone already exists: " + request.getPhone());
        }

        customerMapper.updateEntityFromRequest(existing, request);
        Customer saved = customerRepository.save(existing);
        return customerMapper.toResponse(saved);
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
        customerRepository.delete(customer);
    }
}

