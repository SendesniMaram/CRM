package com.crm.hr.util;

import com.crm.hr.entity.Attendance;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for Attendance search.
 */
public class AttendanceSpecification {

    private AttendanceSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search attendances by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<Attendance> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
