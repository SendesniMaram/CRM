package com.crm.hr.controller;

import com.crm.hr.dto.*;
import com.crm.hr.entity.*;
import com.crm.hr.service.HrWorkflowService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class HrWorkflowController {
    private final HrWorkflowService service;
    public HrWorkflowController(HrWorkflowService service){this.service=service;}
    @GetMapping("/api/evaluations") public Page<EmployeeEvaluation> evaluations(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String employeeId,@RequestParam(required=false)String keyword){return service.evaluations(page,size,employeeId,keyword);}
    @GetMapping("/api/evaluations/{id}") public EmployeeEvaluation evaluation(@PathVariable Long id){return service.getEvaluation(id);}
    @PostMapping("/api/evaluations") @ResponseStatus(HttpStatus.CREATED) public EmployeeEvaluation createEvaluation(@Valid @RequestBody EvaluationRequest r){return service.saveEvaluation(null,r);}
    @PutMapping("/api/evaluations/{id}") public EmployeeEvaluation updateEvaluation(@PathVariable Long id,@Valid @RequestBody EvaluationRequest r){return service.saveEvaluation(id,r);}
    @DeleteMapping("/api/evaluations/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteEvaluation(@PathVariable Long id){service.deleteEvaluation(id);}
    @GetMapping("/api/job-offers") public Page<JobOffer> offers(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)String keyword){return service.offers(page,size,keyword);}
    @GetMapping("/api/job-offers/{id}") public JobOffer offer(@PathVariable Long id){return service.getOffer(id);}
    @PostMapping("/api/job-offers") @ResponseStatus(HttpStatus.CREATED) public JobOffer createOffer(@Valid @RequestBody JobOfferRequest r){return service.saveOffer(null,r);}
    @PutMapping("/api/job-offers/{id}") public JobOffer updateOffer(@PathVariable Long id,@Valid @RequestBody JobOfferRequest r){return service.saveOffer(id,r);}
    @DeleteMapping("/api/job-offers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteOffer(@PathVariable Long id){service.deleteOffer(id);}
    @GetMapping("/api/applications") public Page<JobApplication> applications(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size,@RequestParam(required=false)Long offerId,@RequestParam(required=false)String keyword){return service.applications(page,size,offerId,keyword);}
    @GetMapping("/api/applications/{id}") public JobApplication application(@PathVariable Long id){return service.getApplication(id);}
    @PostMapping("/api/applications") @ResponseStatus(HttpStatus.CREATED) public JobApplication createApplication(@Valid @RequestBody JobApplicationRequest r){return service.saveApplication(null,r);}
    @PutMapping("/api/applications/{id}") public JobApplication updateApplication(@PathVariable Long id,@Valid @RequestBody JobApplicationRequest r){return service.saveApplication(id,r);}
    @DeleteMapping("/api/applications/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteApplication(@PathVariable Long id){service.deleteApplication(id);}
}
