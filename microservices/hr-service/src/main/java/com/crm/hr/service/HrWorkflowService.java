package com.crm.hr.service;

import com.crm.hr.dto.*;
import com.crm.hr.entity.*;
import com.crm.hr.exception.ResourceNotFoundException;
import com.crm.hr.repository.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional
public class HrWorkflowService {
    private final EmployeeEvaluationRepository evaluations;
    private final JobOfferRepository offers;
    private final JobApplicationRepository applications;
    public HrWorkflowService(EmployeeEvaluationRepository evaluations, JobOfferRepository offers, JobApplicationRepository applications) {
        this.evaluations=evaluations; this.offers=offers; this.applications=applications;
    }
    @Transactional(readOnly=true) public Page<EmployeeEvaluation> evaluations(int page,int size,String employeeId,String keyword) {
        Specification<EmployeeEvaluation> spec=(r,q,c)->c.conjunction();
        if(employeeId!=null&&!employeeId.isBlank())spec=spec.and((r,q,c)->c.equal(r.get("employeeId"),employeeId.trim()));
        if(keyword!=null&&!keyword.isBlank()){String k="%"+keyword.trim().toLowerCase()+"%";spec=spec.and((r,q,c)->c.or(c.like(c.lower(r.get("employeeId")),k),c.like(c.lower(r.get("comments")),k),c.like(c.lower(r.get("criteria")),k)));}
        return evaluations.findAll(spec,paging(page,size));
    }
    public EmployeeEvaluation saveEvaluation(Long id,EvaluationRequest request){
        if(request.periodEnd().isBefore(request.periodStart()))throw new IllegalArgumentException("Evaluation period end precedes start");
        EmployeeEvaluation e=id==null?new EmployeeEvaluation():evaluations.findById(id).orElseThrow(()->new ResourceNotFoundException("Evaluation not found"));
        e.setEmployeeId(request.employeeId().trim());e.setPeriodStart(request.periodStart());e.setPeriodEnd(request.periodEnd());e.setCriteria(request.criteria());e.setComments(request.comments());e.setStatus(request.status());return evaluations.save(e);
    }
    @Transactional(readOnly=true) public EmployeeEvaluation getEvaluation(Long id){return evaluations.findById(id).orElseThrow(()->new ResourceNotFoundException("Evaluation not found"));}
    public void deleteEvaluation(Long id){evaluations.delete(evaluations.findById(id).orElseThrow(()->new ResourceNotFoundException("Evaluation not found")));}
    @Transactional(readOnly=true) public Page<JobOffer> offers(int page,int size,String keyword){
        Specification<JobOffer> spec=(r,q,c)->c.conjunction();if(keyword!=null&&!keyword.isBlank()){String k="%"+keyword.trim().toLowerCase()+"%";spec=spec.and((r,q,c)->c.or(c.like(c.lower(r.get("title")),k),c.like(c.lower(r.get("description")),k)));}
        return offers.findAll(spec,paging(page,size));
    }
    public JobOffer saveOffer(Long id,JobOfferRequest request){JobOffer e=id==null?new JobOffer():offers.findById(id).orElseThrow(()->new ResourceNotFoundException("Offer not found"));e.setTitle(request.title().trim());e.setDescription(request.description());e.setStatus(request.status());return offers.save(e);}
    @Transactional(readOnly=true) public JobOffer getOffer(Long id){return offers.findById(id).orElseThrow(()->new ResourceNotFoundException("Offer not found"));}
    public void deleteOffer(Long id){if(!offers.existsById(id))throw new ResourceNotFoundException("Offer not found");if(applications.exists((r,q,c)->c.equal(r.get("offerId"),id)))throw new IllegalArgumentException("Offer with applications cannot be deleted");offers.deleteById(id);}
    @Transactional(readOnly=true) public Page<JobApplication> applications(int page,int size,Long offerId,String keyword){
        Specification<JobApplication> spec=(r,q,c)->c.conjunction();if(offerId!=null)spec=spec.and((r,q,c)->c.equal(r.get("offerId"),offerId));
        if(keyword!=null&&!keyword.isBlank()){String k="%"+keyword.trim().toLowerCase()+"%";spec=spec.and((r,q,c)->c.or(c.like(c.lower(r.get("candidateName")),k),c.like(c.lower(r.get("candidateEmail")),k)));}
        return applications.findAll(spec,paging(page,size));
    }
    public JobApplication saveApplication(Long id,JobApplicationRequest request){
        if(!offers.existsById(request.offerId()))throw new ResourceNotFoundException("Job offer not found");
        JobApplication e=id==null?new JobApplication():applications.findById(id).orElseThrow(()->new ResourceNotFoundException("Application not found"));
        e.setOfferId(request.offerId());e.setCandidateName(request.candidateName().trim());e.setCandidateEmail(request.candidateEmail().trim());e.setNotes(request.notes());e.setStatus(request.status());return applications.save(e);
    }
    @Transactional(readOnly=true) public JobApplication getApplication(Long id){return applications.findById(id).orElseThrow(()->new ResourceNotFoundException("Application not found"));}
    public void deleteApplication(Long id){applications.delete(applications.findById(id).orElseThrow(()->new ResourceNotFoundException("Application not found")));}
    private Pageable paging(int page,int size){if(page<0||size<1||size>100)throw new IllegalArgumentException("Invalid pagination");return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"id"));}
}
