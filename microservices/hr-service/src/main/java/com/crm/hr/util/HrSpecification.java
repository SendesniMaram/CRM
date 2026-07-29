package com.crm.hr.util;

import com.crm.hr.entity.HrRecord;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for HR record search.
 */
public class HrSpecification {

    private HrSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search HR records by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<HrRecord> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("contractType")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("jobTitle")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("manager")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
