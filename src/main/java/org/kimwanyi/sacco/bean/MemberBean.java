package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.member.CreateMemberRequest;
import org.kimwanyi.sacco.dto.member.MemberResponse;
import org.kimwanyi.sacco.mapper.MemberMapper;
import org.kimwanyi.sacco.repositoryImpl.AuditRepositoryImpl;
import org.kimwanyi.sacco.repositoryImpl.MemberRepositoryImpl;
import org.kimwanyi.sacco.service.MemberService;
import org.kimwanyi.sacco.serviceImpl.AuditServiceImpl;
import org.kimwanyi.sacco.serviceImpl.MemberServiceImpl;
import org.kimwanyi.sacco.validation.MemberValidator;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Named("memberBean")
@RequestScoped
public class MemberBean implements Serializable {

    private MemberService memberService;
    private CreateMemberRequest newMember = new CreateMemberRequest();
    private List<MemberResponse> members = Collections.emptyList();
    private String message;
    private String errorMessage;

    @PostConstruct
    public void init() {
        try {
            MemberRepositoryImpl memberRepo = new MemberRepositoryImpl();
            AuditRepositoryImpl auditRepo = new AuditRepositoryImpl();
            AuditService auditService = new AuditServiceImpl(auditRepo);
            this.memberService = new MemberServiceImpl(memberRepo, new MemberValidator(), new MemberMapper(), auditService);
            loadMembers();
        } catch (Exception e) {
            // Graceful initialization for view rendering
        }
    }

    public void loadMembers() {
        if (memberService != null) {
            try {
                this.members = memberService.findAll();
            } catch (Exception e) {
                this.errorMessage = "Failed to load members: " + e.getMessage();
            }
        }
    }

    public String registerMember() {
        try {
            if (memberService != null) {
                memberService.registerMember(newMember);
                this.message = "Member registered successfully!";
                this.newMember = new CreateMemberRequest();
                loadMembers();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public String deactivateMember(Long id) {
        try {
            if (memberService != null && id != null) {
                memberService.deactivateMember(id);
                this.message = "Member deactivated successfully.";
                loadMembers();
            }
        } catch (Exception e) {
            this.errorMessage = e.getMessage();
        }
        return null;
    }

    public CreateMemberRequest getNewMember() { return newMember; }
    public void setNewMember(CreateMemberRequest newMember) { this.newMember = newMember; }
    public List<MemberResponse> getMembers() { return members; }
    public String getMessage() { return message; }
    public String getErrorMessage() { return errorMessage; }
}
