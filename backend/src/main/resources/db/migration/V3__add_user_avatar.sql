ALTER TABLE users ADD COLUMN avatar_base64 TEXT;
ALTER TABLE users ADD COLUMN username_key VARCHAR(30);
UPDATE users SET username_key = LOWER(username);
ALTER TABLE users ALTER COLUMN username_key SET NOT NULL;
CREATE UNIQUE INDEX users_username_key_unique ON users(username_key);
