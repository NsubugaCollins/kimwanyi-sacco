package org.kimwanyi.sacco.service;

import org.kimwanyi.sacco.dto.member.CreateMemberRequest;
import org.kimwanyi.sacco.dto.member.MemberResponse;

import java.util.List;

public interface MemberService {
    MemberResponse registerMember(CreateMemberRequest request);
    MemberResponse findById(Long id);
    MemberResponse findByMembershipNumber(String membershipNumber);
    List<MemberResponse> findAll();
    void deactivateMember(Long memberId);

}
