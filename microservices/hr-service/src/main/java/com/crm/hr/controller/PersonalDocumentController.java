package com.crm.hr.controller;
import com.crm.hr.service.PersonalDocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents/me")
public class PersonalDocumentController {
    private final PersonalDocumentService service;
    public PersonalDocumentController(PersonalDocumentService service){this.service=service;}
    @GetMapping
    public ResponseEntity<List<PersonalDocumentService.Summary>> list(Authentication authentication,HttpServletRequest request){
        noTarget(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(authentication));
    }
    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> download(Authentication authentication,@PathVariable String id,HttpServletRequest request){
        noTarget(request);
        var result=service.download(authentication,id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .contentType(MediaType.parseMediaType(result.metadata().mediaType()))
            .header("X-Content-Type-Options","nosniff")
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(result.metadata().name(),StandardCharsets.UTF_8).build().toString())
            .body(result.body());
    }
    private void noTarget(HttpServletRequest request){
        if(!request.getParameterMap().isEmpty())throw new IllegalArgumentException("Target parameters are not allowed");
    }
}
