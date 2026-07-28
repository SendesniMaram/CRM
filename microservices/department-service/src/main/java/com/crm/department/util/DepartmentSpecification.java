package com.crm.department.util;

import com.crm.department.entity.Department;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for Department search.
 */
public class DepartmentSpecification {

    private DepartmentSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search departments by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<Department> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
