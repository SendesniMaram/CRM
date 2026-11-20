package com.crm.invoice.util;

import com.crm.invoice.entity.Invoice;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<Invoice> searchByKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(keyword)) {
                return criteriaBuilder.conjunction();
            }

            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("invoiceNumber")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("customerId")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("currency")), pattern));
            predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("status")), pattern));

            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}
