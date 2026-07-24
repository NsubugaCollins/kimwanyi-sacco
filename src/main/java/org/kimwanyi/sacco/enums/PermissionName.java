package org.kimwanyi.sacco.enums;

public enum PermissionName {
    //use managemenet
    CREATE_USER, UPDATE_USED, DEACTIVATE_USER, ASSIGN_ROLE,
    //savings
    CREATE_DEPOSIT,PROCESS_WITHDRAW,VIEW_SAVINGS,
    //loans
    CREATE_LOAN_APPLICATION,APPROVE_LOAN,REJECT_LOAN,PROCESS_LOAN_PAYMENT,
    //reports
    VIEW_REPORTS,EXPORT_REPORTS
}
