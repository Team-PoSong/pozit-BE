ALTER TABLE members
    DROP CONSTRAINT IF EXISTS members_provider_check,
    ADD CONSTRAINT members_provider_check CHECK (provider IN ('KAKAO', 'APPLE', 'GOOGLE'));
