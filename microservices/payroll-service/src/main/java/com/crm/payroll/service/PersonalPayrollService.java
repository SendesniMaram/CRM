package com.crm.payroll.service;

import com.crm.payroll.client.PayrollEmployeeDirectoryClient;
import com.crm.payroll.dto.PayrollResponse;
import com.crm.payroll.entity.Payroll;
import com.crm.payroll.mapper.PayrollMapper;
import com.crm.payroll.repository.PayrollRepository;
import com.crm.security.AuthenticatedUserPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.time.Year;
import java.time.YearMonth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonalPayrollService {
    private static final Pattern PERIOD = Pattern.compile("[0-9]{4}(-[0-9]{2})?");
    private final PayrollRepository payrolls;
    private final PayrollMapper mapper;
    private final PayrollEmployeeDirectoryClient employees;

    public PersonalPayrollService(PayrollRepository payrolls, PayrollMapper mapper,
            PayrollEmployeeDirectoryClient employees) {
        this.payrolls = payrolls;
        this.mapper = mapper;
        this.employees = employees;
    }

    public Page<PayrollResponse> list(Authentication authentication, String authorization,
            int page, int size, String period) {
        String employeeId = employeeId(authentication, authorization);
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payroll page request");
        }
        String normalizedPeriod = period == null || period.isBlank() ? null : period.trim();
        if (normalizedPeriod != null && !PERIOD.matcher(normalizedPeriod).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period must be YYYY or YYYY-MM");
        }
        if (normalizedPeriod != null) {
            try {
                if (normalizedPeriod.length() == 4) Year.parse(normalizedPeriod);
                else YearMonth.parse(normalizedPeriod);
            } catch (java.time.DateTimeException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payroll period");
            }
        }
        Specification<Payroll> specification = (root, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.equal(root.get("employeeId"), employeeId));
            predicates.add(criteria.isTrue(root.get("published")));
            if (normalizedPeriod != null) {
                if (normalizedPeriod.length() == 4) {
                    predicates.add(criteria.like(root.get("payPeriod"), normalizedPeriod + "%"));
                } else {
                    predicates.add(criteria.equal(root.get("payPeriod"), normalizedPeriod));
                }
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };
        return payrolls.findAll(specification, PageRequest.of(page, size,
                        Sort.by(Sort.Order.desc("payPeriod"), Sort.Order.desc("id"))))
                .map(mapper::toResponse);
    }

    public PayrollResponse read(Authentication authentication, String authorization, Long id) {
        String employeeId = employeeId(authentication, authorization);
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll resource unavailable");
        }
        Payroll payroll = payrolls.findById(id)
                .filter(row -> employeeId.equals(row.getEmployeeId()))
                .filter(Payroll::isPublished)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll resource unavailable"));
        return mapper.toResponse(payroll);
    }

    private String employeeId(Authentication authentication, String authorization) {
        AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.require(authentication);
        if (principal.authorities().stream().noneMatch(authority -> "ROLE_EMPLOYEE".equals(authority.getAuthority()))) {
            throw new org.springframework.security.access.AccessDeniedException("Employee access required");
        }
        return employees.ownEmployeeId(authorization, principal.userId());
    }
}
