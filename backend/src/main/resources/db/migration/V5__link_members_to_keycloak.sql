-- 인증을 Keycloak으로 옮긴다. 비밀번호는 Keycloak이 관리하므로 지우고, Keycloak 사용자 id(sub)로 회원을 잇는다.
-- 기존 회원은 같은 이메일로 Keycloak에 처음 로그인할 때 keycloak_id가 채워진다.
ALTER TABLE members
    ADD COLUMN keycloak_id VARCHAR(36) NULL AFTER id,
    ADD CONSTRAINT uk_members_keycloak_id UNIQUE (keycloak_id),
    DROP COLUMN password;
