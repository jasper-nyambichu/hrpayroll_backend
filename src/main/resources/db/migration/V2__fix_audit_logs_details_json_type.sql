-- ============================================================
-- V2__fix_audit_logs_details_json_type.sql
-- AuditLog.detailsJson is annotated @Lob on a String, which Hibernate
-- maps to CLOB -> OID on PostgreSQL (not TEXT). Align the column type
-- to match what Hibernate's schema validator expects.
-- Table is part of the fresh baseline with no data yet, so a simple
-- drop/re-add is safe.
-- ============================================================

ALTER TABLE audit_logs DROP COLUMN details_json;
ALTER TABLE audit_logs ADD COLUMN details_json OID;