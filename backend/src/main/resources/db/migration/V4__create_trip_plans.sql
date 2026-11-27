-- 나만의 여행 계획. budget은 원 단위(0이면 미정).
CREATE TABLE trip_plans
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    owner_id   BIGINT       NOT NULL,
    name       VARCHAR(40)  NOT NULL,
    budget     BIGINT       NOT NULL DEFAULT 0,
    start_date DATE         NOT NULL,
    end_date   DATE         NOT NULL,
    notes      VARCHAR(800) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_trip_plans_owner (owner_id),
    CONSTRAINT fk_trip_plans_owner FOREIGN KEY (owner_id) REFERENCES members (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

-- 여행 동선(방문 순서대로). 장소 정보는 추가 시점의 관광지 정보를 복사해 둔다.
-- 수정 시 동선 전체를 지우고 다시 넣으므로 별도 id 없이 (plan_id, stop_order)로 식별한다.
CREATE TABLE plan_stops
(
    plan_id         BIGINT       NOT NULL,
    stop_order      INT          NOT NULL,
    place_id        VARCHAR(40)  NOT NULL,
    name            VARCHAR(100) NOT NULL,
    address         VARCHAR(200) NOT NULL,
    lat             DOUBLE       NOT NULL,
    lng             DOUBLE       NOT NULL,
    content_type_id VARCHAR(5)   NOT NULL,
    PRIMARY KEY (plan_id, stop_order),
    CONSTRAINT fk_plan_stops_plan FOREIGN KEY (plan_id) REFERENCES trip_plans (id) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
