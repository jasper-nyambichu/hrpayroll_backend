-- V35__audit_details_as_text.sql  (STAGED: move into src/main/resources/db/migration only after a pg_dump backup,
-- together with the AuditLog entity change below)
-- audit_logs.details_json was a PostgreSQL large object (OID, from V2): every row's JSON lived in
-- pg_largeobject and had to be streamed one by one inside a transaction, which made the audit pages slow
-- and broke reads outside a transaction. The JSON is small, so it becomes plain TEXT.
--
-- Every entry's text is copied across unchanged. The append-only trigger (V16) is paused for that one copy
-- and switched back on in this same transaction, so if anything fails the whole change rolls back with the
-- protection still in place. The large objects are then removed so they don't linger as orphans.

ALTER TABLE audit_logs ADD COLUMN details_text TEXT;

ALTER TABLE audit_logs DISABLE TRIGGER audit_logs_no_update_delete;

UPDATE audit_logs
SET details_text = convert_from(lo_get(details_json), 'UTF8')
WHERE details_json IS NOT NULL;

ALTER TABLE audit_logs ENABLE TRIGGER audit_logs_no_update_delete;

SELECT lo_unlink(details_json) FROM audit_logs WHERE details_json IS NOT NULL;

ALTER TABLE audit_logs DROP COLUMN details_json;
ALTER TABLE audit_logs RENAME COLUMN details_text TO details_json;

-- Required in the same commit, in entity/audit/AuditLog.java, replace
--     @Lob
--     private String detailsJson;
-- with
--     // Plain TEXT since V35 (it was a large object, slow to read row by row).
--     @Column(columnDefinition = "text")
--     private String detailsJson;
-- Tested 2026-11-09 on a copy of the local database: all 334 entries identical before and after (md5 of the
-- text), no large objects left, append-only trigger re-enabled and still refusing updates, audit page
-- 328 ms -> 28 ms (one request) and 2.6 s -> about 0.1 s (30 at once).
