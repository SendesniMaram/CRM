package com.crm.payroll.util;

import com.crm.payroll.entity.Payroll;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for Payroll search.
 */
public class PayrollSpecification {

    private PayrollSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search Payroll records by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<Payroll> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("payPeriod")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("currency")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}

