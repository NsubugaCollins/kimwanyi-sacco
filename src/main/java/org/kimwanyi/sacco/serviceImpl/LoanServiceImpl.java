package org.kimwanyi.sacco.serviceImpl;

import org.kimwanyi.sacco.audit.AuditService;
import org.kimwanyi.sacco.dto.loan.LoanApplicationRequest;
import org.kimwanyi.sacco.dto.loan.LoanApprovalRequest;
import org.kimwanyi.sacco.dto.loan.LoanPaymentRequest;
import org.kimwanyi.sacco.dto.loan.LoanResponse;
import org.kimwanyi.sacco.dto.notification.SendNotificationRequest;
import org.kimwanyi.sacco.entity.Loan;
import org.kimwanyi.sacco.entity.LoanRepayment;
import org.kimwanyi.sacco.entity.Member;
import org.kimwanyi.sacco.entity.User;
import org.kimwanyi.sacco.enums.ApprovalStatus;
import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.LoanStatus;
import org.kimwanyi.sacco.enums.NotificationChannel;
import org.kimwanyi.sacco.enums.NotificationType;
import org.kimwanyi.sacco.exception.ValidationException;
import org.kimwanyi.sacco.repository.LoanRepaymentRepository;
import org.kimwanyi.sacco.repository.LoanRepository;
import org.kimwanyi.sacco.repository.MemberRepository;
import org.kimwanyi.sacco.repository.UserRepository;
import org.kimwanyi.sacco.service.LoanService;
import org.kimwanyi.sacco.service.NotificationService;
import org.kimwanyi.sacco.util.TransactionManager;
import org.kimwanyi.sacco.validation.LoanValidator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class LoanServiceImpl implements LoanService {

    private static final BigDecimal MONTHLY_INTEREST_RATE = new BigDecimal("0.05"); // 5% per month

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final LoanValidator loanValidator;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            MemberRepository memberRepository,
            UserRepository userRepository,
            LoanValidator loanValidator
    ) {
        this(loanRepository, loanRepaymentRepository, memberRepository, userRepository, loanValidator, null, null);
    }

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            MemberRepository memberRepository,
            UserRepository userRepository,
            LoanValidator loanValidator,
            AuditService auditService
    ) {
        this(loanRepository, loanRepaymentRepository, memberRepository, userRepository, loanValidator, auditService, null);
    }

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            MemberRepository memberRepository,
            UserRepository userRepository,
            LoanValidator loanValidator,
            AuditService auditService,
            NotificationService notificationService
    ) {
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.loanValidator = loanValidator;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Override
    public LoanResponse applyForLoan(LoanApplicationRequest request) {
        return TransactionManager.execute(session -> {
            Member member = memberRepository.findById(session, request.getMemberId())
                    .orElseThrow(() -> new ValidationException("Member not found with ID: " + request.getMemberId()));

            loanValidator.validateApplication(session, request, member);

            BigDecimal principal = request.getPrincipalAmount();
            int months = request.getTermInMonths();

            // Total Interest = Principal * 5% per month * Term in Months
            BigDecimal totalInterest = principal
                    .multiply(MONTHLY_INTEREST_RATE)
                    .multiply(BigDecimal.valueOf(months))
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal totalPayable = principal.add(totalInterest);

            Loan loan = new Loan();
            loan.setMember(member);
            loan.setPrincipalAmount(principal);
            loan.setInterestRate(MONTHLY_INTEREST_RATE);
            loan.setTermInMonths(months);
            loan.setTotalInterest(totalInterest);
            loan.setTotalAmountPayable(totalPayable);
            loan.setRemainingBalance(totalPayable);
            loan.setStatus(LoanStatus.PENDING);
            loan.setPurpose(request.getPurpose() != null ? request.getPurpose().trim() : null);

            Loan savedLoan = loanRepository.save(session, loan);

            if (auditService != null) {
                auditService.logSuccess(
                        member.getId(),
                        AuditAction.LOAN_APPLICATION,
                        "Loan",
                        savedLoan.getId(),
                        String.format("Loan application submitted for member ID %d: Principal %s, Term %d months",
                                member.getId(), principal.toPlainString(), months)
                );
            }

            return toResponse(savedLoan);
        });
    }

    @Override
    public LoanResponse approveOrRejectLoan(LoanApprovalRequest request) {
        if (request == null || request.getLoanId() == null) {
            throw new ValidationException("Loan ID is required for approval.");
        }
        if (request.getApproverUserId() == null) {
            throw new ValidationException("Approver User ID is required.");
        }

        return TransactionManager.execute(session -> {
            Loan loan = loanRepository.findById(session, request.getLoanId())
                    .orElseThrow(() -> new ValidationException("Loan not found with ID: " + request.getLoanId()));

            User approverUser = userRepository.findById(session, request.getApproverUserId())
                    .orElseThrow(() -> new ValidationException("Approver user not found with ID: " + request.getApproverUserId()));

            loanValidator.validateApproval(session, loan, approverUser);

            AuditAction auditAction;
            if (request.isApproved()) {
                loan.setStatus(LoanStatus.APPROVED);
                loan.setApprovedByUserId(approverUser.getId());
                loan.setApprovedAt(LocalDateTime.now());
                if (request.getRemarks() != null) {
                    loan.setRemarks(request.getRemarks().trim());
                }
                auditAction = AuditAction.APPROVE_LOAN;
            } else {
                loan.setStatus(LoanStatus.REJECTED);
                loan.setApprovedByUserId(approverUser.getId());
                loan.setApprovedAt(LocalDateTime.now());
                loan.setRemarks(request.getRemarks() != null ? request.getRemarks().trim() : "Loan application rejected.");
                auditAction = AuditAction.REJECT_LOAN;
            }

            Loan updatedLoan = loanRepository.update(session, loan);

            if (auditService != null) {
                auditService.logSuccess(
                        approverUser.getId(),
                        auditAction,
                        "Loan",
                        updatedLoan.getId(),
                        String.format("Loan ID %d %s by user ID %d", updatedLoan.getId(), updatedLoan.getStatus(), approverUser.getId())
                );
            }

            return toResponse(updatedLoan);
        });
    }

    @Override
    public LoanResponse disburseLoan(Long loanId, Long officerUserId) {
        if (loanId == null) {
            throw new ValidationException("Loan ID is required for disbursement.");
        }

        return TransactionManager.execute(session -> {
            Loan loan = loanRepository.findById(session, loanId)
                    .orElseThrow(() -> new ValidationException("Loan not found with ID: " + loanId));

            if (loan.getStatus() != LoanStatus.APPROVED) {
                throw new ValidationException("Only APPROVED loans can be disbursed. Current status: " + loan.getStatus());
            }

            loan.setStatus(LoanStatus.ACTIVE);
            loan.setDisbursedAt(LocalDateTime.now());

            Loan updatedLoan = loanRepository.update(session, loan);

            if (auditService != null) {
                auditService.logSuccess(
                        officerUserId,
                        AuditAction.LOAN_DISBURSEMENT,
                        "Loan",
                        updatedLoan.getId(),
                        String.format("Loan ID %d disbursed. Status changed to ACTIVE", updatedLoan.getId())
                );
            }

            return toResponse(updatedLoan);
        });
    }

    @Override
    public LoanResponse repayLoan(LoanPaymentRequest request) {
        return TransactionManager.execute(session -> {
            Loan loan = loanRepository.findById(session, request.getLoanId())
                    .orElseThrow(() -> new ValidationException("Loan not found with ID: " + request.getLoanId()));

            loanValidator.validatePayment(session, request, loan);

            BigDecimal paymentAmount = request.getAmount();
            ApprovalStatus status = request.isRequiresApproval() ? ApprovalStatus.PENDING : ApprovalStatus.APPROVED;

            if (status == ApprovalStatus.APPROVED) {
                BigDecimal newBalance = loan.getRemainingBalance().subtract(paymentAmount).setScale(2, RoundingMode.HALF_UP);
                loan.setRemainingBalance(newBalance);

                if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    loan.setStatus(LoanStatus.COMPLETED);
                }
            }

            LoanRepayment repayment = new LoanRepayment(
                    loan,
                    paymentAmount,
                    request.getReferenceNumber().trim(),
                    request.getRemarks() != null ? request.getRemarks().trim() : null,
                    status
            );

            loan.addRepayment(repayment);
            loanRepaymentRepository.save(session, repayment);

            Loan updatedLoan = loanRepository.update(session, loan);

            if (notificationService != null && status == ApprovalStatus.PENDING) {
                try {
                    SendNotificationRequest notifReq = new SendNotificationRequest();
                    notifReq.setUserId(100L);
                    notifReq.setTitle("Pending Loan Repayment Approval Request");
                    notifReq.setMessage(String.format("New loan repayment of UGX %s for Loan #L-%d requires cashier approval. Ref: %s",
                            paymentAmount.toPlainString(), loan.getId(), request.getReferenceNumber()));
                    notifReq.setType(NotificationType.LOAN_REPAYMENT);
                    notifReq.setChannel(NotificationChannel.SYSTEM);
                    notificationService.sendNotification(notifReq);
                } catch (Exception ignored) {}
            }

            if (auditService != null) {
                auditService.logSuccess(
                        loan.getMember() != null ? loan.getMember().getId() : null,
                        AuditAction.LOAN_REPAYMENT,
                        "LoanRepayment",
                        repayment.getId(),
                        String.format("Repayment of %s recorded (%s) for Loan ID %d.",
                                paymentAmount.toPlainString(), status, loan.getId())
                );
            }

            return toResponse(updatedLoan);
        });
    }

    @Override
    public LoanResponse approveRepayment(Long repaymentId, Long cashierUserId) {
        if (repaymentId == null) {
            throw new ValidationException("Repayment ID is required for approval.");
        }
        return TransactionManager.execute(session -> {
            LoanRepayment repayment = loanRepaymentRepository.findById(session, repaymentId)
                    .orElseThrow(() -> new ValidationException("Repayment not found with ID: " + repaymentId));

            if (repayment.getApprovalStatus() != ApprovalStatus.PENDING) {
                throw new ValidationException("Repayment is not pending approval.");
            }

            repayment.setApprovalStatus(ApprovalStatus.APPROVED);
            repayment.setApprovedByUserId(cashierUserId);
            repayment.setApprovedAt(LocalDateTime.now());

            Loan loan = repayment.getLoan();
            BigDecimal newBalance = loan.getRemainingBalance().subtract(repayment.getAmountPaid()).setScale(2, RoundingMode.HALF_UP);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) newBalance = BigDecimal.ZERO;
            loan.setRemainingBalance(newBalance);

            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                loan.setStatus(LoanStatus.COMPLETED);
            }

            loanRepaymentRepository.update(session, repayment);
            Loan updatedLoan = loanRepository.update(session, loan);

            if (notificationService != null && loan.getMember() != null) {
                try {
                    SendNotificationRequest notifReq = new SendNotificationRequest();
                    notifReq.setMemberId(loan.getMember().getId());
                    notifReq.setTitle("Loan Repayment Approved");
                    notifReq.setMessage(String.format("Your loan repayment of UGX %s (Ref: %s) for Loan #L-%d was approved. Remaining balance: UGX %s",
                            repayment.getAmountPaid().toPlainString(), repayment.getReferenceNumber(), loan.getId(), newBalance.toPlainString()));
                    notifReq.setType(NotificationType.LOAN_REPAYMENT);
                    notifReq.setChannel(NotificationChannel.BOTH);
                    notificationService.sendNotification(notifReq);
                } catch (Exception ignored) {}
            }

            return toResponse(updatedLoan);
        });
    }

    @Override
    public LoanResponse rejectRepayment(Long repaymentId, Long cashierUserId, String reason) {
        if (repaymentId == null) {
            throw new ValidationException("Repayment ID is required for rejection.");
        }
        return TransactionManager.execute(session -> {
            LoanRepayment repayment = loanRepaymentRepository.findById(session, repaymentId)
                    .orElseThrow(() -> new ValidationException("Repayment not found with ID: " + repaymentId));

            if (repayment.getApprovalStatus() != ApprovalStatus.PENDING) {
                throw new ValidationException("Repayment is not pending approval.");
            }

            repayment.setApprovalStatus(ApprovalStatus.REJECTED);
            repayment.setApprovedByUserId(cashierUserId);
            repayment.setApprovedAt(LocalDateTime.now());
            repayment.setRejectionReason(reason != null ? reason.trim() : "Rejected by cashier");

            loanRepaymentRepository.update(session, repayment);
            Loan loan = repayment.getLoan();

            if (notificationService != null && loan.getMember() != null) {
                try {
                    SendNotificationRequest notifReq = new SendNotificationRequest();
                    notifReq.setMemberId(loan.getMember().getId());
                    notifReq.setTitle("Loan Repayment Rejected");
                    notifReq.setMessage(String.format("Your loan repayment of UGX %s (Ref: %s) was rejected. Reason: %s",
                            repayment.getAmountPaid().toPlainString(), repayment.getReferenceNumber(), repayment.getRejectionReason()));
                    notifReq.setType(NotificationType.LOAN_REPAYMENT);
                    notifReq.setChannel(NotificationChannel.BOTH);
                    notificationService.sendNotification(notifReq);
                } catch (Exception ignored) {}
            }

            return toResponse(loan);
        });
    }

    @Override
    public List<LoanResponse.RepaymentDto> getPendingRepayments() {
        return TransactionManager.execute(session -> {
            List<LoanRepayment> repayments = session.createQuery(
                    "FROM LoanRepayment lr JOIN FETCH lr.loan l JOIN FETCH l.member m WHERE lr.approvalStatus = :status ORDER BY lr.paymentDate DESC",
                    LoanRepayment.class)
                    .setParameter("status", ApprovalStatus.PENDING)
                    .getResultList();

            return repayments.stream().map(r -> {
                LoanResponse.RepaymentDto dto = new LoanResponse.RepaymentDto();
                dto.setId(r.getId());
                dto.setAmountPaid(r.getAmountPaid());
                dto.setPaymentDate(r.getPaymentDate());
                dto.setReferenceNumber(r.getReferenceNumber());
                dto.setRemarks(r.getRemarks());
                dto.setApprovalStatus(r.getApprovalStatus() != null ? r.getApprovalStatus() : ApprovalStatus.APPROVED);
                dto.setRejectionReason(r.getRejectionReason());
                return dto;
            }).collect(Collectors.toList());
        });
    }

    @Override
    public LoanResponse getLoanById(Long loanId) {
        return TransactionManager.execute(session -> {
            Loan loan = loanRepository.findById(session, loanId)
                    .orElseThrow(() -> new ValidationException("Loan not found with ID: " + loanId));
            return toResponse(loan);
        });
    }

    @Override
    public List<LoanResponse> getLoansByMember(Long memberId) {
        return TransactionManager.execute(session -> {
            return loanRepository.findByMemberId(session, memberId).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public List<LoanResponse> getLoansByStatus(LoanStatus status) {
        return TransactionManager.execute(session -> {
            return loanRepository.findByStatus(session, status).stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        });
    }

    private LoanResponse toResponse(Loan loan) {
        LoanResponse response = new LoanResponse();
        response.setId(loan.getId());
        if (loan.getMember() != null) {
            response.setMemberId(loan.getMember().getId());
            response.setMembershipNumber(loan.getMember().getMembershipNumber());
            String firstName = loan.getMember().getFirstName() != null ? loan.getMember().getFirstName() : "";
            String lastName = loan.getMember().getLastName() != null ? loan.getMember().getLastName() : "";
            response.setMemberName((firstName + " " + lastName).trim());
        }
        response.setPrincipalAmount(loan.getPrincipalAmount());
        response.setInterestRate(loan.getInterestRate() != null ? loan.getInterestRate().multiply(new java.math.BigDecimal("100")).setScale(1, java.math.RoundingMode.HALF_UP) : java.math.BigDecimal.ZERO);
        response.setTermInMonths(loan.getTermInMonths());
        response.setTotalInterest(loan.getTotalInterest());
        response.setTotalAmountPayable(loan.getTotalAmountPayable());
        response.setRemainingBalance(loan.getRemainingBalance());
        response.setStatus(loan.getStatus());
        response.setPurpose(loan.getPurpose());
        response.setApprovedByUserId(loan.getApprovedByUserId());
        response.setApprovedAt(loan.getApprovedAt());
        response.setDisbursedAt(loan.getDisbursedAt());
        response.setRemarks(loan.getRemarks());

        if (loan.getRepayments() != null) {
            List<LoanResponse.RepaymentDto> repaymentDtos = loan.getRepayments().stream().map(r -> {
                LoanResponse.RepaymentDto dto = new LoanResponse.RepaymentDto();
                dto.setId(r.getId());
                dto.setAmountPaid(r.getAmountPaid());
                dto.setPaymentDate(r.getPaymentDate());
                dto.setReferenceNumber(r.getReferenceNumber());
                dto.setRemarks(r.getRemarks());
                dto.setApprovalStatus(r.getApprovalStatus() != null ? r.getApprovalStatus() : ApprovalStatus.APPROVED);
                dto.setRejectionReason(r.getRejectionReason());
                return dto;
            }).collect(Collectors.toList());
            response.setRecentRepayments(repaymentDtos);
        }

        return response;
    }
}
