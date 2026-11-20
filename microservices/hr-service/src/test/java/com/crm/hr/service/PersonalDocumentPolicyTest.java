package com.crm.hr.service;
import com.crm.hr.entity.*;
import com.crm.hr.repository.EmployeeDocumentRepository;
import com.crm.hr.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PersonalDocumentPolicyTest {
 final EmployeeAssociationService associations=mock(EmployeeAssociationService.class);
 final EmployeeDocumentRepository repository=mock(EmployeeDocumentRepository.class);
 final EmployeeDocumentService storage=mock(EmployeeDocumentService.class);
 final Authentication auth=mock(Authentication.class);
 final PersonalDocumentService service=new PersonalDocumentService(associations,repository,storage);
 @BeforeEach void setup(){var link=new LeaveEmployeeAssociation();link.setEmployeeId("103");link.setUserId(42L);when(associations.readOwn(auth)).thenReturn(link);}
 EmployeeDocument document(String owner,boolean visible){var d=new EmployeeDocument();d.setId(7L);d.setEmployeeId(owner);d.setEmployeeVisible(visible);d.setName("attestation.pdf");d.setMediaType("application/pdf");d.setStorageKey("secret-private-path");return d;}
 @Test void listOnlyContainsSharedOwnedDocuments()throws Exception{
  when(repository.findAllByEmployeeIdOrderByCreatedAtDesc("103")).thenReturn(List.of(document("103",true),document("103",false),document("999",true)));
  var rows=service.list(auth);assertEquals(1,rows.size());assertEquals("7",rows.get(0).id());
  assertFalse(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(rows).contains("secret-private-path"));verifyNoInteractions(storage);
 }
 @Test void ownedSharedDownloadReadsStorage(){when(repository.findById(7L)).thenReturn(Optional.of(document("103",true)));when(storage.download(7L)).thenReturn(new byte[]{1});assertArrayEquals(new byte[]{1},service.download(auth,"7").body());}
 @Test void foreignDocumentNeverReadsStorage(){when(repository.findById(7L)).thenReturn(Optional.of(document("999",true)));assertThrows(ResourceNotFoundException.class,()->service.download(auth,"7"));verifyNoInteractions(storage);}
 @Test void privateDocumentNeverReadsStorage(){when(repository.findById(7L)).thenReturn(Optional.of(document("103",false)));assertThrows(ResourceNotFoundException.class,()->service.download(auth,"7"));verifyNoInteractions(storage);}
 @Test void missingDocumentNeverReadsStorage(){assertThrows(ResourceNotFoundException.class,()->service.download(auth,"7"));verifyNoInteractions(storage);}
 @Test void missingAssociationStopsLookup(){when(associations.readOwn(auth)).thenThrow(new ResourceNotFoundException("missing"));assertThrows(ResourceNotFoundException.class,()->service.list(auth));verifyNoInteractions(repository,storage);}
 @Test void deniedAssociationStopsLookup(){when(associations.readOwn(auth)).thenThrow(new AccessDeniedException("denied"));assertThrows(AccessDeniedException.class,()->service.download(auth,"7"));verifyNoInteractions(repository,storage);}
 @Test void invalidIdentifierStopsLookup(){assertThrows(IllegalArgumentException.class,()->service.download(auth,"01"));verifyNoInteractions(repository,storage);}
 @Test void storageFailureIsNotSuccess(){when(repository.findById(7L)).thenReturn(Optional.of(document("103",true)));when(storage.download(7L)).thenThrow(new IllegalStateException("unavailable"));assertThrows(IllegalStateException.class,()->service.download(auth,"7"));}
 @Test void emptyListIsEmpty(){assertTrue(service.list(auth).isEmpty());}
}
