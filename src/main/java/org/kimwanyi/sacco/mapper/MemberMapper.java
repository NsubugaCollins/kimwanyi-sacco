package org.kimwanyi.sacco.mapper;

import org.kimwanyi.sacco.dto.member.CreateMemberRequest;
import org.kimwanyi.sacco.dto.member.MemberResponse;
import org.kimwanyi.sacco.entity.Member;

public class MemberMapper {

    public Member toEntity(CreateMemberRequest request) {

        Member member = new Member();

        member.setFirstName(request.getFirstName());
        member.setLastName(request.getLastName());
        member.setNationalId(request.getNationalId());
        member.setPhoneNumber(request.getPhoneNumber());
        member.setEmail(request.getEmail());
        member.setAddress(request.getAddress());
        member.setDateOfBirth(request.getDateOfBirth());

        return member;

    }

    public MemberResponse toResponse(Member member) {

        MemberResponse response = new MemberResponse();

        response.setId(member.getId());
        response.setMembershipNumber(member.getMembershipNumber());
        response.setFirstName(member.getFirstName());
        response.setLastName(member.getLastName());
        response.setNationalId(member.getNationalId());
        response.setPhoneNumber(member.getPhoneNumber());
        response.setEmail(member.getEmail());
        response.setStatus(member.getStatus());

        return response;

    }

}