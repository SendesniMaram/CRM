package com.crm.fees.util;

import com.crm.fees.entity.Fee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for building dynamic JPA Specifications for Fee search.
 */
public class FeeSpecification {

    private FeeSpecification() {
        // utility class
    }

    /**
     * Build a Specification to search Fee records by keyword across multiple fields.
     *
     * @param keyword search keyword (nullable)
     * @return Specification for filtering
     */
    public static Specification<Fee> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("employeeId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customerId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("serviceName")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceStatus")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("paymentStatus")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
