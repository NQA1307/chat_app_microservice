ALTER TABLE friendships ADD COLUMN IF NOT EXISTS action_user_id UUID;

CREATE INDEX IF NOT EXISTS idx_friendships_blocked_action_user
ON friendships(action_user_id, status);

CREATE INDEX IF NOT EXISTS idx_friendships_requester_receiver
ON friendships(requester_id, receiver_id);
