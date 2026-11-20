package com.crm.identity.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="administration_events",indexes={@Index(name="idx_admin_event_time",columnList="occurred_at,id"),@Index(name="idx_admin_event_target",columnList="target_id,id")})
public class AdministrationEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt;
 @Column(name="actor_id",updatable=false) private Long actorId;
 @Column(name="target_id",nullable=false,updatable=false) private Long targetId;
 @Column(nullable=false,length=40,updatable=false) private String action;
 @Column(nullable=false,length=20,updatable=false) private String outcome="SUCCESS";
 protected AdministrationEvent(){}
 public AdministrationEvent(Long actor,Long target,String action){this.actorId=actor;this.targetId=target;this.action=action;this.occurredAt=Instant.now();}
 public Long getId(){return id;} public Instant getOccurredAt(){return occurredAt;} public Long getActorId(){return actorId;} public Long getTargetId(){return targetId;} public String getAction(){return action;} public String getOutcome(){return outcome;}
}
