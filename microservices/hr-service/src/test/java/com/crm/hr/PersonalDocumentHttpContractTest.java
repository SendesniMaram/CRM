package com.crm.hr;
import com.crm.hr.controller.PersonalDocumentController;
import com.crm.hr.service.PersonalDocumentService;
import com.crm.hr.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class PersonalDocumentHttpContractTest {
 final PersonalDocumentService service=mock(PersonalDocumentService.class);
 final MockMvc mvc=standaloneSetup(new PersonalDocumentController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
 @Test void listIsNotCached()throws Exception{when(service.list(any())).thenReturn(List.of());mvc.perform(get("/api/documents/me")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().json("[]"));}
 @Test void suppliedTargetIsRejectedBeforeService()throws Exception{mvc.perform(get("/api/documents/me").param("employeeId","999")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
 @Test void downloadIsAttachmentAndNotCached()throws Exception{when(service.download(any(),eq("7"))).thenReturn(new PersonalDocumentService.Download(new PersonalDocumentService.Summary("7","attestation.pdf","CERTIFICATE","application/pdf",4,null),new byte[]{1}));mvc.perform(get("/api/documents/me/7/content")).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff")).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.startsWith("attachment;")));}
}
