DELETE FROM flyway_schema_history
WHERE version = '1'
  AND script = 'V1__init_notification_schema.sql';