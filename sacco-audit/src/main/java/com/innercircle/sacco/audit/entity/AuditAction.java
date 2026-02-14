package com.innercircle.sacco.audit.entity;

/**
 * Enumeration of audit actions tracked in the system.
 * Represents all significant operations that should be captured in the audit trail.
 */
public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    APPROVE,
    REJECT,
    DISBURSE,
    SUSPEND,
    REACTIVATE,
    LOGIN,
    LOGOUT,
    CONFIG_CHANGE,
    REPAID,
    REJECTED,
    APPROVED
}
