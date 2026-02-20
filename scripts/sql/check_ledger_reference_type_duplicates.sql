-- Detect duplicate ledger idempotency keys before adding
-- uk_journal_entries_reference_type(reference_id, transaction_type).
SELECT
    reference_id,
    transaction_type,
    COUNT(*) AS duplicate_count,
    ARRAY_AGG(id ORDER BY created_at ASC) AS entry_ids
FROM journal_entries
GROUP BY reference_id, transaction_type
HAVING COUNT(*) > 1
ORDER BY duplicate_count DESC, reference_id;
