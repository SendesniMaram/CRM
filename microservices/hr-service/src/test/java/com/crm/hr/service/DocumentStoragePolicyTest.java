package com.crm.hr.service;
import com.crm.hr.entity.EmployeeDocument;
import com.crm.hr.repository.EmployeeDocumentRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.nio.file.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class DocumentStoragePolicyTest {
 @TempDir Path root;
 final EmployeeDocumentRepository repository=mock(EmployeeDocumentRepository.class);
 EmployeeDocumentService service;
 EmployeeDocument document;
 @BeforeEach void setup(){service=new EmployeeDocumentService(repository,mock(EmployeeReferenceValidator.class),root.toString());document=new EmployeeDocument();document.setId(7L);document.setStorageKey("document.pdf");when(repository.findById(7L)).thenReturn(Optional.of(document));}
 @AfterEach void clear(){SecurityContextHolder.clearContext();}
 @Test void readsOnlyTheStoredFile()throws Exception{Files.write(root.resolve("document.pdf"),new byte[]{1,2});assertArrayEquals(new byte[]{1,2},service.download(7L));}
 @Test void relativeEscapeIsRejected(){document.setStorageKey("../private.pdf");assertThrows(IllegalStateException.class,()->service.download(7L));}
 @Test void missingFileHasNoPathInPublicError(){var error=assertThrows(IllegalStateException.class,()->service.download(7L));assertEquals("Document storage unavailable",error.getMessage());}
 @Test void oversizedFileIsRejected()throws Exception{Files.write(root.resolve("document.pdf"),new byte[10485761]);assertThrows(IllegalStateException.class,()->service.download(7L));}
 @Test void directoryIsRejected()throws Exception{Files.createDirectory(root.resolve("document.pdf"));assertThrows(IllegalStateException.class,()->service.download(7L));}
 @Test void visibilityRequiresAnAdministrator(){assertThrows(AccessDeniedException.class,()->service.visibility(7L,true));assertFalse(document.isEmployeeVisible());}
 @Test void newDocumentsArePrivate(){assertFalse(new EmployeeDocument().isEmployeeVisible());}
}
