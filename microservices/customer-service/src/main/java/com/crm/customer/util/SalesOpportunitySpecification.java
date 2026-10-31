package com.crm.customer.util;

import com.crm.customer.entity.SalesOpportunity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class SalesOpportunitySpecification {
    private SalesOpportunitySpecification() { }

    public static Specification<SalesOpportunity> filters(
            String keyword, String stage, Long employeeId, Long customerId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
            }
            if (StringUtils.hasText(stage)) predicates.add(cb.equal(root.get("stage"), stage));
            if (employeeId != null) predicates.add(cb.equal(root.get("employeeId"), employeeId));
            if (customerId != null) predicates.add(cb.equal(root.get("customerId"), customerId));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
