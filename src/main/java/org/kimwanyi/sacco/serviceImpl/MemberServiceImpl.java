package org.kimwanyi.sacco.serviceImpl;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.member.CreateMemberRequest;
import org.kimwanyi.sacco.dto.member.MemberResponse;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.UserStatus;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.mapper.MemberMapper;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.service.MemberService;
import org.kimwanyi.sacco.util.MembershipNumberGenerator;
import org.kimwanyi.sacco.util.PhoneNumberUtil;
import org.kimwanyi.sacco.util.TransactionManager;
import org.kimwanyi.sacco.validation.MemberValidator;

import java.util.List;
import java.util.stream.Collectors;

public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberValidator memberValidator;
    private final MemberMapper memberMapper;
    private final AuditService auditService;

    public MemberServiceImpl(
            MemberRepository memberRepository,
            MemberValidator memberValidator,
            MemberMapper memberMapper,
            AuditService auditService
    ) {
        this.memberRepository = memberRepository;
        this.memberValidator = memberValidator;
        this.memberMapper = memberMapper;
        this.auditService = auditService;
    }

    @Override
    public MemberResponse registerMember(CreateMemberRequest request) {
        validateRequest(request);
        Member member = memberMapper.toEntity(request);
        normalize(member);
        validateBusinessRules(member);

        return TransactionManager.execute(session -> {
            checkDuplicates(session, member);
            assignMembershipNumber(session, member);

            Member savedMember = memberRepository.save(session, member);
            auditCreation(savedMember);

            return memberMapper.toResponse(savedMember);
        });
    }

    @Override
    public MemberResponse findById(Long memberId) {
        return TransactionManager.execute(session -> {
            Member member = memberRepository.findById(session, memberId).orElseThrow(()
                    -> new ValidationException("Member not found."));
            return memberMapper.toResponse(member);
        });
    }

    @Override
    public MemberResponse findByMembershipNumber(String membershipNumber) {
        if (membershipNumber == null || membershipNumber.isBlank()) {
            throw new ValidationException("Membership number is required.");
        }

        return TransactionManager.execute(session -> {
            Member member = memberRepository.findByMemberNumber(session, membershipNumber.trim());
            if (member == null) {
                throw new ValidationException("Member not found.");
            }
            return memberMapper.toResponse(member);
        });
    }

    @Override
    public List<MemberResponse> findAll() {
        return TransactionManager.execute(session -> {
            return memberRepository.findAll(session)
                    .stream()
                    .map(memberMapper::toResponse)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public void deactivateMember(Long memberId) {
        TransactionManager.execute(session -> {
            Member member = memberRepository.findById(session, memberId).orElseThrow(()
                    -> new ValidationException("Member not found."));

            if (member.getStatus() == UserStatus.INACTIVE) {
                throw new ValidationException("Member is already inactive.");
            }

            member.setStatus(UserStatus.INACTIVE);
            memberRepository.update(session, member);

            auditService.logSuccess(
                    null, // Replace with logged-in user id later
                    AuditAction.DEACTIVATE_MEMBER,
                    "Member",
                    member.getId(),
                    "Member account deactivated"
            );
            return null;
        });
    }

    private void validateRequest(CreateMemberRequest request) {
        if (request == null) {
            throw new ValidationException("Request cannot be null.");
        }
    }

    private void normalize(Member member) {
        member.setFirstName(member.getFirstName().trim());
        member.setLastName(member.getLastName().trim());
        member.setNationalId(member.getNationalId().trim().toUpperCase());
        if (member.getEmail() != null && !member.getEmail().isBlank()) {
            member.setEmail(member.getEmail().trim().toLowerCase());
        }

        if (member.getPhoneNumber() != null && !member.getPhoneNumber().isBlank()) {
            member.setPhoneNumber(PhoneNumberUtil.normalize(member.getPhoneNumber()));
        }

        if (member.getAddress() != null) {
            member.setAddress(member.getAddress().trim());
        }
    }

    private void validateBusinessRules(Member member) {
        memberValidator.validate(member);
    }

    private void checkDuplicates(org.hibernate.Session session, Member member) {
        if (memberRepository.existsByNationalId(session, member.getNationalId())) {
            throw new ValidationException("A member with this National ID already exists.");
        }
    }

    private void assignMembershipNumber(org.hibernate.Session session, Member member) {
        long nextSequence = memberRepository.count(session) + 1;
        String membershipNumber = MembershipNumberGenerator.generate(nextSequence);
        member.setMembershipNumber(membershipNumber);
    }

    private void auditCreation(Member member) {
        auditService.logSuccess(null, // Replace with authenticated user id
                AuditAction.CREATE_MEMBER,
                "Member",
                member.getId(),
                "Member registered successfully."
        );
    }
}