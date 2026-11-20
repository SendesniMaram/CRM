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
        var page=customerRepository.findAll(PageRequest.of(0,101,Sort.by("id")));
        if(page.getTotalElements()>100)throw new IllegalArgumentException("More than 100 contacts: use explicit pagination");
        return page.getContent().stream().map(customerMapper::toResponse).toList();
    }

    @Override
    public Page<CustomerResponse> getAllCustomersPaged(int page, int size, String sortBy, String direction, String keyword) {
        return getAllCustomersPaged(page, size, sortBy, direction, keyword, null, null, null, null);
    }

    @Override
    public Page<CustomerResponse> getAllCustomersPaged(int page, int size, String sortBy, String direction, String keyword, String contactType, String category, String segment, Long assignedEmployeeId) {
        return search(page,size,sortBy,direction,keyword,contactType,category,segment,assignedEmployeeId,null);
    }
    @Override public Page<CustomerResponse> search(int page,int size,String sortBy,String direction,String keyword,String contactType,String category,String segment,Long assignedEmployeeId,String status){
        if(page<0||size<1||size>100||keyword!=null&&keyword.length()>200||category!=null&&category.length()>100||segment!=null&&segment.length()>100||assignedEmployeeId!=null&&assignedEmployeeId<=0)throw new IllegalArgumentException("Invalid page or filters");
        String field=StringUtils.hasText(sortBy)?sortBy:"id";
        if(!java.util.Set.of("id","name","email","phone","company","contactType","category","segment","assignedEmployeeId","createdAt").contains(field)||!java.util.Set.of("asc","desc").contains(direction.toLowerCase(java.util.Locale.ROOT))||contactType!=null&&!java.util.Set.of("LEAD","CUSTOMER","SUPPLIER").contains(contactType))throw new IllegalArgumentException("Invalid sort or contact type");
        var filters=CustomerSpecification.filters(keyword,contactType,category,segment,assignedEmployeeId).and(CustomerSpecification.state(status));
        return customerRepository.findAll(filters,PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(direction),field).and(Sort.by("id")))).map(customerMapper::toResponse);
    }
    @Override public CustomerResponse restore(Long id){var row=locked(id);row.setArchived(false);return customerMapper.toResponse(row);}
    private Customer locked(Long id){return customerRepository.lockById(id).orElseThrow(()->new ResourceNotFoundException("Contact unavailable"));}
    private void active(Customer row){if(row.isArchived())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Archived contact is read-only");}

    @Override
    public CustomerResponse convertLead(Long id) {
        Customer existing = locked(id);active(existing);
        if (!"LEAD".equals(existing.getContactType())) throw new IllegalArgumentException("Only leads can be converted");
        existing.setContactType("CUSTOMER");
        return customerMapper.toResponse(customerRepository.save(existing));
    }

    @Override
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer existing = locked(id);active(existing);

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
        Customer customer=locked(id);
        customer.setArchived(true);
    }
}
