package com.crm.hr.service;

import com.crm.hr.entity.EmployeeDocument;
import com.crm.hr.repository.EmployeeDocumentRepository;
import com.crm.hr.exception.ResourceNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly=true)
public class PersonalDocumentService {
    private final EmployeeAssociationService associations;
    private final EmployeeDocumentRepository repository;
    private final EmployeeDocumentService storage;
    public PersonalDocumentService(EmployeeAssociationService associations, EmployeeDocumentRepository repository, EmployeeDocumentService storage) {
        this.associations=associations; this.repository=repository; this.storage=storage;
    }
    public record Summary(String id, String name, String type, String mediaType, long size, LocalDateTime createdAt) {}
    public record Download(Summary metadata, byte[] body) {}
    public List<Summary> list(Authentication authentication) {
        String employee=associations.readOwn(authentication).getEmployeeId();
        return repository.findAllByEmployeeIdOrderByCreatedAtDesc(employee).stream()
            .filter(d -> employee.equals(d.getEmployeeId()) && d.isEmployeeVisible()).map(this::summary).toList();
    }
    public Download download(Authentication authentication, String id) {
        String employee=associations.readOwn(authentication).getEmployeeId();
        long key=EmployeeAssociationService.parseId(id);
        EmployeeDocument document=repository.findById(key)
            .filter(d -> employee.equals(d.getEmployeeId()) && d.isEmployeeVisible())
            .orElseThrow(() -> new ResourceNotFoundException("Document unavailable"));
        return new Download(summary(document),storage.download(key));
    }
    private Summary summary(EmployeeDocument document) {
        return new Summary(document.getId().toString(),document.getName(),document.getType(),document.getMediaType(),document.getSize(),document.getCreatedAt());
    }
}
