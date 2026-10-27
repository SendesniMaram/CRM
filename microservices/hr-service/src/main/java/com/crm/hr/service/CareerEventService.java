package com.crm.hr.service;
import com.crm.hr.dto.CareerEventRequest; import com.crm.hr.entity.CareerEvent; import com.crm.hr.exception.ResourceNotFoundException; import com.crm.hr.repository.CareerEventRepository; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.List;
@Service @Transactional public class CareerEventService{
 private final CareerEventRepository repository;private final EmployeeReferenceValidator employees;public CareerEventService(CareerEventRepository r,EmployeeReferenceValidator e){repository=r;employees=e;}
 @Transactional(readOnly=true) public List<CareerEvent> list(String employeeId,String authorization){employees.requireExists(employeeId,authorization);return repository.findAllByEmployeeIdOrderByEffectiveDateDescIdDesc(employeeId);}
 public CareerEvent create(CareerEventRequest request,String authorization){employees.requireExists(request.employeeId(),authorization);CareerEvent e=new CareerEvent();e.setEmployeeId(request.employeeId());e.setEffectiveDate(request.effectiveDate());e.setType(request.type());e.setPreviousValue(request.previousValue());e.setNewValue(request.newValue());e.setComment(request.comment());return repository.save(e);}
 public void delete(long id){repository.delete(repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Career event not found")));}
}
