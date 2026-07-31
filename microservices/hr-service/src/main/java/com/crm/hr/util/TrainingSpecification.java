package com.crm.hr.util;

import com.crm.hr.entity.Training;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for Training search.
 */
public class TrainingSpecification {

    private TrainingSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search trainings by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<Training> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("provider")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
