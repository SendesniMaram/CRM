package com.crm.identity.entity;
import jakarta.persistence.*;
@Entity @Table(name="crm_display_settings")
public class DisplaySettings {
 @Id private Long id=1L;
 @Version private long version;
 @Column(name="company_name",nullable=false,length=100) private String companyName="CRM Workspace";
 @Column(name="support_email",nullable=false,length=100) private String supportEmail="";
 @Column(name="welcome_message",nullable=false,length=500) private String welcomeMessage="";
 public Long getId(){return id;}public long getVersion(){return version;}public String getCompanyName(){return companyName;}public String getSupportEmail(){return supportEmail;}public String getWelcomeMessage(){return welcomeMessage;}
 public void update(String name,String email,String welcome){companyName=name;supportEmail=email;welcomeMessage=welcome;}
}
