// entity/auth/Role.java
package com.company.hrpayroll.entity.auth;

public enum Role {
    SUPER_ADMIN,
    ADMIN,
    HR_MANAGER,
    PAYROLL_OFFICER,
    BRANCH_MANAGER,
    AGENT,
    EMPLOYEE,
    FINANCE_OFFICER,   // shown to users as "Accounts Office"
    AUDITOR,
    BACK_OFFICE        // runs the clock-in/out desktop application and records days worked and damages
}