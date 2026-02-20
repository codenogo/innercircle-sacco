-- Requeue failed/pending petty-cash dead letters after deployment.
UPDATE event_dead_letter
SET
    status = 'PENDING_RETRY',
    next_retry_at = NOW(),
    updated_at = NOW(),
    error_message = NULL
WHERE event_type = 'com.innercircle.sacco.common.event.PettyCashWorkflowEvent'
  AND status IN ('FAILED', 'PENDING_RETRY');
