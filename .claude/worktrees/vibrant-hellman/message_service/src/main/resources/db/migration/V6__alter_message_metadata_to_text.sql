ALTER TABLE messages
ALTER COLUMN metadata TYPE TEXT USING metadata::TEXT;

ALTER TABLE direct_messages
ALTER COLUMN metadata TYPE TEXT USING metadata::TEXT;
