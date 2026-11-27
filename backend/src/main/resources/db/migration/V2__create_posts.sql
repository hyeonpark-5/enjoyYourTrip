-- 게시글. type: NOTICE(공지, 관리자만 작성) | POST(공유게시판)
-- 작성자가 탈퇴해도(소프트 삭제) 글은 남는다.
CREATE TABLE posts
(
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    type       VARCHAR(20)   NOT NULL,
    title      VARCHAR(80)   NOT NULL,
    content    VARCHAR(2000) NOT NULL,
    author_id  BIGINT        NOT NULL,
    view_count INT           NOT NULL DEFAULT 0,
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_posts_type_id (type, id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES members (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
