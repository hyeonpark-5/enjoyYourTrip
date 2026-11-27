-- 회원. MEMBER는 MySQL 8 예약어라 테이블명은 복수형을 쓴다.
-- 탈퇴는 deleted_at을 채우는 소프트 삭제이며, 탈퇴한 이메일은 재사용할 수 없다(UNIQUE 유지).
CREATE TABLE members
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    role       VARCHAR(20)  NOT NULL,
    deleted_at DATETIME(6)  NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_members_email UNIQUE (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
