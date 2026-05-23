SELECT id, recipient_id, type, read, source_type, source_id, body, created_at
FROM notifications
ORDER BY created_at DESC;