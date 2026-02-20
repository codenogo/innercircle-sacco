-- Remove duplicate journal entries by (reference_id, transaction_type),
-- keeping the earliest created row per key.
WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY reference_id, transaction_type
            ORDER BY created_at ASC, id ASC
        ) AS rn
    FROM journal_entries
),
to_delete AS (
    SELECT id
    FROM ranked
    WHERE rn > 1
)
DELETE FROM journal_entries
WHERE id IN (SELECT id FROM to_delete);
