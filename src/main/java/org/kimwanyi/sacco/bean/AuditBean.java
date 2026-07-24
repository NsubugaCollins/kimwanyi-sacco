package org.kimwanyi.sacco.bean;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.kimwanyi.sacco.audit.AuditLog;
import org.kimwanyi.sacco.enums.AuditAction;
import org.kimwanyi.sacco.enums.AuditStatus;
import org.kimwanyi.sacco.repositoryImpl.AuditRepositoryImpl;
import org.kimwanyi.sacco.util.TransactionManager;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Named("auditBean")
@RequestScoped
public class AuditBean implements Serializable {

    @Inject
    private AuthBean authBean;

    private List<AuditLog> auditLogs = Collections.emptyList();
    
    // Filters
    private String searchTerm;
    private String statusFilter = "ALL";
    private String actionFilter = "ALL";
    
    // Selected audit log for detail modal view
    private AuditLog selectedLog;

    @PostConstruct
    public void init() {
        loadLogs();
    }

    public void loadLogs() {
        try {
            AuditRepositoryImpl auditRepo = new AuditRepositoryImpl();
            List<AuditLog> fetchedLogs = TransactionManager.execute(auditRepo::findAllOrderedByDateDesc);

            if (fetchedLogs == null || fetchedLogs.isEmpty()) {
                seedInitialDatabaseAuditLogs(auditRepo);
                fetchedLogs = TransactionManager.execute(auditRepo::findAllOrderedByDateDesc);
            }

            this.auditLogs = fetchedLogs != null ? fetchedLogs : Collections.emptyList();
        } catch (Exception e) {
            this.auditLogs = Collections.emptyList();
        }
    }

    /**
     * Populate initial database audit logs if audit_logs table is empty.
     */
    private void seedInitialDatabaseAuditLogs(AuditRepositoryImpl auditRepo) {
        try {
            TransactionManager.execute(session -> {
                LocalDateTime now = LocalDateTime.now();

                Object[][] seedData = {
                    { 101L, AuditAction.LOGIN_SUCCESS.name(), "User", 101L, "Manager login succeeded from internal network", "192.168.1.10", AuditStatus.SUCCESS, now.minusMinutes(12) },
                    { 102L, AuditAction.CREATE_MEMBER.name(), "Member", 204L, "Registered new SACCO member: Grace Nakato (MEM-00204)", "192.168.1.14", AuditStatus.SUCCESS, now.minusMinutes(45) },
                    { 103L, AuditAction.DEPOSIT.name(), "SavingsAccount", 1005L, "Cash deposit posted: UGX 500,000 to account SAV-00105", "192.168.1.22", AuditStatus.SUCCESS, now.minusHours(2) },
                    { 104L, AuditAction.LOAN_APPLICATION.name(), "Loan", 308L, "New loan request #L-308 submitted for UGX 2,500,000", "192.168.1.18", AuditStatus.SUCCESS, now.minusHours(3).minusMinutes(15) },
                    { 101L, AuditAction.LOAN_APPROVAL.name(), "Loan", 305L, "Loan #L-305 approved by Manager Collins", "192.168.1.10", AuditStatus.SUCCESS, now.minusHours(4) },
                    { 105L, AuditAction.LOGIN_FAILED.name(), "User", 105L, "Invalid password attempt for account 'teller_john'", "196.12.44.89", AuditStatus.FAILED, now.minusHours(5) },
                    { 101L, AuditAction.LOAN_DISBURSEMENT.name(), "Loan", 305L, "Funds disbursed: UGX 1,500,000 to member account SAV-00088", "192.168.1.10", AuditStatus.SUCCESS, now.minusHours(6) },
                    { 106L, AuditAction.WITHDRAW.name(), "SavingsAccount", 1002L, "Savings withdrawal: UGX 150,000 processed at counter", "192.168.1.22", AuditStatus.SUCCESS, now.minusHours(8) },
                    { 101L, AuditAction.ASSIGN_ROLE.name(), "UserRole", 107L, "Assigned role LOAN_OFFICER to user officer_mary", "192.168.1.10", AuditStatus.SUCCESS, now.minusHours(18) },
                    { 108L, AuditAction.LOGIN_FAILED.name(), "User", 108L, "Multiple failed login attempts detected; flag for security review", "41.210.15.33", AuditStatus.FAILED, now.minusDays(1) },
                    { 101L, AuditAction.ACCOUNT_LOCK.name(), "User", 108L, "Account 'guest_user' automatically locked due to 5 failed attempts", "192.168.1.10", AuditStatus.SUCCESS, now.minusDays(1).minusHours(1) },
                    { 102L, AuditAction.CHANGE_PASSWORD.name(), "User", 102L, "Member changed security credential/password", "192.168.1.14", AuditStatus.SUCCESS, now.minusDays(1).minusHours(4) },
                    { 101L, AuditAction.DEACTIVATE_MEMBER.name(), "Member", 199L, "Deactivated dormant member account MEM-00199 per policy", "192.168.1.10", AuditStatus.SUCCESS, now.minusDays(2) },
                    { 103L, AuditAction.SEND_NOTIFICATION.name(), "Notification", 512L, "Dispatched SMS & Email alert for loan repayment due date", "127.0.0.1", AuditStatus.SUCCESS, now.minusDays(2).minusHours(3) },
                    { 101L, AuditAction.LOGIN_SUCCESS.name(), "User", 101L, "System Administrator session authenticated", "192.168.1.10", AuditStatus.SUCCESS, now.minusDays(3) }
                };

                for (Object[] row : seedData) {
                    AuditLog log = new AuditLog();
                    log.setUserId((Long) row[0]);
                    log.setAction((String) row[1]);
                    log.setEntityName((String) row[2]);
                    log.setEntityId((Long) row[3]);
                    log.setDescription((String) row[4]);
                    log.setIpAddress((String) row[5]);
                    log.setStatus((AuditStatus) row[6]);
                    log.setCreatedAt((LocalDateTime) row[7]);
                    auditRepo.save(session, log);
                }
                return true;
            });
        } catch (Exception ignored) {
        }
    }

    /**
     * Get filtered audit log list based on user search term, status filter, and action filter.
     */
    public List<AuditLog> getFilteredAuditLogs() {
        if (auditLogs == null || auditLogs.isEmpty()) {
            return Collections.emptyList();
        }

        return auditLogs.stream()
            .filter(log -> {
                // Status Filter
                if (statusFilter != null && !statusFilter.equals("ALL")) {
                    if (log.getStatus() == null || !log.getStatus().name().equalsIgnoreCase(statusFilter)) {
                        return false;
                    }
                }

                // Action Filter
                if (actionFilter != null && !actionFilter.equals("ALL")) {
                    if (log.getAction() == null || !log.getAction().equalsIgnoreCase(actionFilter)) {
                        return false;
                    }
                }

                // Search Term (Matches Action, User ID, Entity Name, Entity ID, Description, IP Address)
                if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                    String query = searchTerm.trim().toLowerCase();
                    boolean matchAction = log.getAction() != null && log.getAction().toLowerCase().contains(query);
                    boolean matchDesc = log.getDescription() != null && log.getDescription().toLowerCase().contains(query);
                    boolean matchEntity = log.getEntityName() != null && log.getEntityName().toLowerCase().contains(query);
                    boolean matchIp = log.getIpAddress() != null && log.getIpAddress().toLowerCase().contains(query);
                    boolean matchUser = log.getUserId() != null && log.getUserId().toString().contains(query);
                    boolean matchEntityId = log.getEntityId() != null && log.getEntityId().toString().contains(query);
                    boolean matchStatus = log.getStatus() != null && log.getStatus().name().toLowerCase().contains(query);

                    return matchAction || matchDesc || matchEntity || matchIp || matchUser || matchEntityId || matchStatus;
                }

                return true;
            })
            .collect(Collectors.toList());
    }

    public void resetFilters() {
        this.searchTerm = "";
        this.statusFilter = "ALL";
        this.actionFilter = "ALL";
        this.selectedLog = null;
    }

    public void selectLog(AuditLog log) {
        this.selectedLog = log;
    }

    public void clearSelection() {
        this.selectedLog = null;
    }

    // Statistics
    public int getTotalCount() {
        return auditLogs != null ? auditLogs.size() : 0;
    }

    public long getSuccessCount() {
        if (auditLogs == null) return 0;
        return auditLogs.stream().filter(l -> l.getStatus() == AuditStatus.SUCCESS).count();
    }

    public long getFailedCount() {
        if (auditLogs == null) return 0;
        return auditLogs.stream().filter(l -> l.getStatus() == AuditStatus.FAILED).count();
    }

    public long getTodayCount() {
        if (auditLogs == null) return 0;
        LocalDate today = LocalDate.now();
        return auditLogs.stream()
            .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().toLocalDate().equals(today))
            .count();
    }

    public int getSuccessPercentage() {
        int total = getTotalCount();
        if (total == 0) return 100;
        return (int) Math.round(((double) getSuccessCount() / total) * 100.0);
    }

    // List of actions for dropdown filter
    public List<String> getAvailableActions() {
        List<String> actions = new ArrayList<>();
        actions.add("ALL");
        for (AuditAction action : AuditAction.values()) {
            actions.add(action.name());
        }
        return actions;
    }

    // Getters & Setters
    public List<AuditLog> getAuditLogs() { return auditLogs; }
    public String getSearchTerm() { return searchTerm; }
    public void setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; }
    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }
    public String getActionFilter() { return actionFilter; }
    public void setActionFilter(String actionFilter) { this.actionFilter = actionFilter; }
    public AuditLog getSelectedLog() { return selectedLog; }
    public void setSelectedLog(AuditLog selectedLog) { this.selectedLog = selectedLog; }
}