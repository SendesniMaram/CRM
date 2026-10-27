package com.crm.hr.service;

import com.crm.hr.entity.EmployeeDocument; import com.crm.hr.exception.ResourceNotFoundException; import com.crm.hr.repository.EmployeeDocumentRepository;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.web.multipart.MultipartFile;
import java.io.IOException; import java.nio.file.*; import java.util.*;

@Service @Transactional
public class EmployeeDocumentService {
 private static final long MAX=10L*1024*1024;
 private final EmployeeDocumentRepository repository; private final EmployeeReferenceValidator employees; private final Path root;
 public EmployeeDocumentService(EmployeeDocumentRepository repository,EmployeeReferenceValidator employees,@Value("${app.hr.documents.storage-root:./private/hr-documents}") String storageRoot){this.repository=repository;this.employees=employees;this.root=Path.of(storageRoot).toAbsolutePath().normalize();}
 @Transactional(readOnly=true) public List<EmployeeDocument> list(String employeeId,String authorization){employees.requireExists(employeeId,authorization);return repository.findAllByEmployeeIdOrderByCreatedAtDesc(employeeId);}
 public EmployeeDocument upload(String employeeId,String type,MultipartFile file,String authorization){
  employees.requireExists(employeeId,authorization); if(type==null||!Set.of("CV","DIPLOMA","CERTIFICATE","OTHER").contains(type))throw new IllegalArgumentException("Unsupported document type");
  if(file==null||file.isEmpty()||file.getSize()>MAX)throw new IllegalArgumentException("Document must be between 1 byte and 10 MiB");
  String name=Objects.requireNonNullElse(file.getOriginalFilename(),"document").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1); if(name.isBlank()||name.length()>255||name.chars().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Invalid document name");
  String media=detect(file);String key=UUID.randomUUID()+extension(media);Path target=root.resolve(key).normalize();if(!target.startsWith(root))throw new IllegalStateException("Invalid storage key");
  try {Files.createDirectories(root);Files.write(target,file.getBytes(),StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);EmployeeDocument d=new EmployeeDocument();d.setEmployeeId(employeeId);d.setName(name);d.setType(type);d.setMediaType(media);d.setSize(file.getSize());d.setStorageKey(key);return repository.save(d);}
  catch(IOException|RuntimeException ex){try{Files.deleteIfExists(target);}catch(IOException ignored){}throw ex instanceof IllegalArgumentException ia?ia:new IllegalStateException("Document storage unavailable");}
 }
 @Transactional(readOnly=true) public byte[] download(long id){EmployeeDocument d=get(id);try{return Files.readAllBytes(path(d));}catch(IOException ex){throw new IllegalStateException("Document storage unavailable");}}
 @Transactional(readOnly=true) public EmployeeDocument metadata(long id){return get(id);}
 public void delete(long id){EmployeeDocument d=get(id);repository.delete(d);try{Files.deleteIfExists(path(d));}catch(IOException ex){throw new IllegalStateException("Document storage unavailable");}}
 private EmployeeDocument get(long id){return repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Document not found"));}
 private Path path(EmployeeDocument d){Path p=root.resolve(d.getStorageKey()).normalize();if(!p.startsWith(root)||d.getStorageKey().contains("/")||d.getStorageKey().contains("\\"))throw new IllegalStateException("Invalid storage reference");return p;}
 private static String detect(MultipartFile f){try{byte[] b=f.getBytes();String ext=Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);if(ext.endsWith(".pdf")&&b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F'&&b[4]=='-')return "application/pdf";if(ext.endsWith(".png")&&b.length>=8&&(b[0]&255)==137&&b[1]=='P'&&b[2]=='N'&&b[3]=='G')return "image/png";if(ext.matches(".*\\.jpe?g")&&b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255)return "image/jpeg";throw new IllegalArgumentException("Only valid PDF, PNG and JPEG files are accepted");}catch(IOException ex){throw new IllegalArgumentException("Unable to read uploaded document");}}
 private static String extension(String media){return switch(media){case "application/pdf"->".pdf";case "image/png"->".png";default->".jpg";};}
}
