package com.crm.identity.service;
import com.crm.identity.entity.*;import com.crm.identity.repository.AdministrationEventRepository;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import org.springframework.security.core.context.SecurityContextHolder;
@Service public class AdministrationJournal {
 private final AdministrationEventRepository events;
 public AdministrationJournal(AdministrationEventRepository events){this.events=events;}
 @Transactional(propagation=Propagation.MANDATORY) public void record(User actor,Long target,String action){if(actor==null||actor.getId()==null)throw new IllegalStateException("Audit actor unavailable");events.save(new AdministrationEvent(actor.getId(),target,action));}
 public User current(){var auth=SecurityContextHolder.getContext().getAuthentication();return auth!=null&&auth.getPrincipal() instanceof User u?u:null;}
}
