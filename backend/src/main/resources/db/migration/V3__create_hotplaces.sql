-- 핫플레이스(나만의 방문 기록). 장소 정보는 등록 시점의 TourAPI 상세를 복사해 둔다.
-- 같은 회원이 같은 장소를 같은 날짜에 두 번 등록할 수 없다.
CREATE TABLE hotplaces
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT       NOT NULL,
    place_id        VARCHAR(20)  NOT NULL,
    place_name      VARCHAR(200) NOT NULL,
    address         VARCHAR(300) NOT NULL,
    lat             DOUBLE       NOT NULL,
    lng             DOUBLE       NOT NULL,
    content_type_id VARCHAR(10)  NOT NULL,
    place_image     VARCHAR(500) NOT NULL,
    place_type      VARCHAR(30)  NOT NULL,
    visited_date    DATE         NOT NULL,
    description     VARCHAR(500) NOT NULL,
    photo_file      VARCHAR(64)  NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_hotplaces_owner_place_date UNIQUE (owner_id, place_id, visited_date),
    CONSTRAINT fk_hotplaces_owner FOREIGN KEY (owner_id) REFERENCES members (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
