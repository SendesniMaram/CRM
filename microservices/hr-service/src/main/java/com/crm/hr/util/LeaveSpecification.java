package com.crm.hr.util;

import com.crm.hr.entity.LeaveRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for LeaveRequest search.
 */
public class LeaveSpecification {

    private LeaveSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search leave requests by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<LeaveRequest> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("reason")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
