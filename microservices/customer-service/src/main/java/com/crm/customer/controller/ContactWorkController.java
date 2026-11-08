package com.crm.customer.controller;
import com.crm.customer.service.*;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;
@RestController @RequestMapping("/api/contact-work") public class ContactWorkController {private final AssignedContactService assigned;private final ClientCaseService cases;public ContactWorkController(AssignedContactService a,ClientCaseService c){assigned=a;cases=c;}
 @ModelAttribute public void parameters(jakarta.servlet.http.HttpServletRequest request){if(request.getParameterMap().keySet().stream().anyMatch(k->!java.util.Set.of("page","keyword","status").contains(k)))throw new IllegalArgumentException("Target parameters are not allowed");}
 @GetMapping("/assigned") public ResponseEntity<?> assigned(@RequestHeader("Authorization") String bearer,@RequestParam(defaultValue="") String keyword,@RequestParam(defaultValue="0") int page){return ok(assigned.list(bearer,keyword,page));}
 @GetMapping("/assigned/{id}") public ResponseEntity<?> contact(@RequestHeader("Authorization") String bearer,@PathVariable long id){return ok(assigned.detail(bearer,id));}
 @PutMapping("/assigned/{id}") public ResponseEntity<?> edit(@RequestHeader("Authorization") String bearer,@PathVariable long id,@Valid @RequestBody AssignedContactService.Edit input){return ok(assigned.edit(bearer,id,input));}
 @GetMapping("/assigned/{id}/interactions") public ResponseEntity<?> interactions(@RequestHeader("Authorization") String bearer,@PathVariable long id,@RequestParam(defaultValue="0") int page){return ok(assigned.interactions(bearer,id,page));}
 @PostMapping("/assigned/{id}/interactions") public ResponseEntity<?> add(@RequestHeader("Authorization") String bearer,@PathVariable long id,@Valid @RequestBody AssignedContactService.Interaction input){return ok(assigned.add(bearer,id,input));}
 @GetMapping("/{space:client|admin}/cases") public ResponseEntity<?> list(@PathVariable String space,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page){return ok(cases.list(admin(space),status,page));}
 @PostMapping("/client/cases") public ResponseEntity<?> create(@Valid @RequestBody ClientCaseService.NewCase input){return ok(cases.create(input));}
 @GetMapping("/{space:client|admin}/cases/{id}") public ResponseEntity<?> detail(@PathVariable String space,@PathVariable long id){return ok(cases.detail(admin(space),id));}
 @GetMapping("/{space:client|admin}/cases/{id}/messages") public ResponseEntity<?> messages(@PathVariable String space,@PathVariable long id,@RequestParam(defaultValue="0") int page){return ok(cases.messages(admin(space),id,page));}
 @PostMapping("/{space:client|admin}/cases/{id}/messages") public ResponseEntity<?> send(@PathVariable String space,@PathVariable long id,@Valid @RequestBody ClientCaseService.Message input){return ok(cases.send(admin(space),id,input));}
 @GetMapping("/{space:client|admin}/cases/{id}/history") public ResponseEntity<?> history(@PathVariable String space,@PathVariable long id,@RequestParam(defaultValue="0") int page){return ok(cases.history(admin(space),id,page));}
 @PatchMapping("/admin/cases/{id}/status") public ResponseEntity<?> state(@PathVariable long id,@Valid @RequestBody ClientCaseService.State input){return ok(cases.state(id,input));}
 private boolean admin(String space){return space.equals("admin");}private ResponseEntity<?> ok(Object body){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);}
}
