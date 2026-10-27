package com.crm.hr;

import com.crm.hr.dto.EmployeeContractRequest;
import com.crm.hr.entity.EmployeeContract;
import com.crm.hr.repository.EmployeeContractRepository;
import com.crm.hr.service.EmployeeContractService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeContractServiceTest {
    private final EmployeeContractRepository repository=mock(EmployeeContractRepository.class);
    private final EmployeeContractService service=new EmployeeContractService(repository);
    @Test void rejectsEndBeforeStart(){
        var request=new EmployeeContractRequest("E1","Fixed term",LocalDate.parse("2026-02-01"),LocalDate.parse("2026-01-01"),EmployeeContract.Status.DRAFT,null,null);
        assertThrows(IllegalArgumentException.class,()->service.create(request));verifyNoInteractions(repository);
    }
    @Test void amendmentMustReferToContractOfSameEmployee(){
        EmployeeContract parent=new EmployeeContract();parent.setId(4L);parent.setEmployeeId("E1");when(repository.findById(4L)).thenReturn(Optional.of(parent));
        var request=new EmployeeContractRequest("E2","Amendment",LocalDate.parse("2026-02-01"),null,EmployeeContract.Status.DRAFT,4L,null);
        assertThrows(IllegalArgumentException.class,()->service.create(request));verify(repository,never()).save(any());
    }
    @Test void amendmentUpdateCannotCreateAParentCycle(){
        EmployeeContract current=new EmployeeContract();current.setId(5L);current.setEmployeeId("E1");
        EmployeeContract parent=new EmployeeContract();parent.setId(4L);parent.setEmployeeId("E1");parent.setParentContractId(5L);
        when(repository.findById(4L)).thenReturn(Optional.of(parent));when(repository.findById(5L)).thenReturn(Optional.of(current));
        var request=new EmployeeContractRequest("E1","Amendment",LocalDate.parse("2026-02-01"),null,EmployeeContract.Status.DRAFT,4L,null);
        assertThrows(IllegalArgumentException.class,()->service.update(5L,request));verify(repository,never()).save(any());
    }
}
