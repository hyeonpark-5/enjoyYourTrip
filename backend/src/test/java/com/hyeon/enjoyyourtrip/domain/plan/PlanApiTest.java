package com.hyeon.enjoyyourtrip.domain.plan;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.hyeon.enjoyyourtrip.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;

class PlanApiTest extends ApiTestSupport {

    /** 서울시청 → 경복궁 직선거리 약 1.46km */
    static final String CITY_HALL = stop("1", "서울시청", 37.5665, 126.9780);
    static final String GYEONGBOKGUNG = stop("126508", "경복궁", 37.5796, 126.9770);

    Tokens owner;
    Tokens other;

    @BeforeEach
    void setUp() throws Exception {
        signup("주인", "owner@example.com");
        signup("남", "other@example.com");
        owner = login("owner@example.com");
        other = login("other@example.com");
    }

    static String stop(String placeId, String name, double lat, double lng) {
        return """
                {"placeId":"%s","name":"%s","address":"서울","lat":%s,"lng":%s,"contentTypeId":"12"}"""
                .formatted(placeId, name, lat, lng);
    }

    static String plan(String name, String start, String end, String... stops) {
        return """
                {"name":"%s","startDate":"%s","endDate":"%s","stops":[%s]}"""
                .formatted(name, start, end, String.join(",", stops));
    }

    long create(Tokens tokens, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("만들면 동선 순서와 총 직선거리가 내려오고, 경비·메모를 비우면 0과 빈 문자열")
    void createPlan() throws Exception {
        mockMvc.perform(post("/api/v1/plans")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plan(" 서울 하루 ", "2026-12-01", "2026-12-01", CITY_HALL, GYEONGBOKGUNG)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("서울 하루"))
                .andExpect(jsonPath("$.budget").value(0))
                .andExpect(jsonPath("$.notes").value(""))
                .andExpect(jsonPath("$.stops", hasSize(2)))
                .andExpect(jsonPath("$.stops[0].order").value(1))
                .andExpect(jsonPath("$.stops[1].name").value("경복궁"))
                .andExpect(jsonPath("$.distanceKm").value(1.5));
    }

    @Test
    @DisplayName("내 목록은 최신순 요약(앞 4곳 + 외 N곳)이고 남의 계획은 보이지 않는다")
    void findMine() throws Exception {
        create(owner, plan("첫 계획", "2026-12-01", "2026-12-02", CITY_HALL));
        String[] five = Arrays.stream(new String[] {"a", "b", "c", "d", "e"})
                .map(id -> stop(id, id.toUpperCase(), 37.5, 127.0))
                .toArray(String[]::new);
        create(owner, plan("긴 계획", "2026-12-03", "2026-12-04", five));
        create(other, plan("남의 계획", "2026-12-01", "2026-12-02", CITY_HALL));

        mockMvc.perform(get("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("긴 계획"))
                .andExpect(jsonPath("$[0].stopCount").value(5))
                .andExpect(jsonPath("$[0].routeSummary").value("A → B → C → D → … 외 1곳"));
    }

    @Test
    @DisplayName("도착일이 출발일보다 빠르면 400 INVALID_PLAN_PERIOD, 같은 장소가 두 번이면 400 DUPLICATE_PLAN_STOP")
    void businessRules() throws Exception {
        mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plan("거꾸로", "2026-12-05", "2026-12-01", CITY_HALL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PLAN_PERIOD"));
        mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plan("중복", "2026-12-01", "2026-12-01", GYEONGBOKGUNG, CITY_HALL, GYEONGBOKGUNG)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DUPLICATE_PLAN_STOP"));
    }

    @Test
    @DisplayName("동선이 비었거나 좌표가 국내가 아니면 400과 필드 오류")
    void validation() throws Exception {
        mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plan("빈 동선", "2026-12-01", "2026-12-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("stops"));
        mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(plan("해외", "2026-12-01", "2026-12-01", stop("x", "파리", 48.85, 2.35))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field").value(
                        org.hamcrest.Matchers.containsInAnyOrder("stops[0].lat", "stops[0].lng")));
    }

    @Test
    @DisplayName("수정하면 동선이 통째로 바뀌고, 남의 계획은 403")
    void update() throws Exception {
        long id = create(owner, plan("원래", "2026-12-01", "2026-12-01", CITY_HALL, GYEONGBOKGUNG));
        String body = """
                {"name":"바뀐 계획","budget":150000,"startDate":"2026-12-02","endDate":"2026-12-03",
                 "notes":"메모","stops":[%s,%s,%s]}""".formatted(GYEONGBOKGUNG, CITY_HALL, stop("2", "남산", 37.5512, 126.9882));

        mockMvc.perform(put("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐 계획"))
                .andExpect(jsonPath("$.budget").value(150000))
                .andExpect(jsonPath("$.stops[*].name").value(
                        org.hamcrest.Matchers.contains("경복궁", "서울시청", "남산")));
        mockMvc.perform(get("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(jsonPath("$.stops", hasSize(3)))
                .andExpect(jsonPath("$.stops[2].order").value(3));

        mockMvc.perform(put("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PLAN_FORBIDDEN"));
    }

    @Test
    @DisplayName("삭제하면 404가 되고, 남의 계획은 보거나 지울 수 없다")
    void deletePlan() throws Exception {
        long id = create(owner, plan("지울 계획", "2026-12-01", "2026-12-01", CITY_HALL));

        mockMvc.perform(get("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/plans/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    @DisplayName("로그인하지 않으면 401")
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/v1/plans")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/plans").contentType(MediaType.APPLICATION_JSON)
                        .content(plan("x", "2026-12-01", "2026-12-01", CITY_HALL)))
                .andExpect(status().isUnauthorized());
    }
}
