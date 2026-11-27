package com.hyeon.enjoyyourtrip.domain.tour;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.hyeon.enjoyyourtrip.domain.tour.exception.TourApiUnavailableException;
import com.hyeon.enjoyyourtrip.domain.tour.service.TourPlaceClient;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;
import com.hyeon.enjoyyourtrip.support.ApiTestSupport;

/**
 * 관광지 API. 외부 TourAPI는 가짜(TourPlaceClient mock)로 바꿔 서비스 규칙만 검증한다.
 * 모든 요청을 토큰 없이 보내 로그인 없이 쓸 수 있는지도 함께 확인한다.
 */
class TourApiTest extends ApiTestSupport {

    @MockitoBean
    TourPlaceClient tourPlaceClient;

    static PlaceInfo place(String id, String image) {
        return new PlaceInfo(id, "장소" + id, "39", "12", "제주특별자치도", image, "", "", 33.5, 126.5);
    }

    @Test
    @DisplayName("지역 목록 17개를 돌려준다")
    void regions() throws Exception {
        mockMvc.perform(get("/api/v1/tours/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(17)))
                .andExpect(jsonPath("$[0].name").value("서울"));
    }

    @Test
    @DisplayName("지역만 고르면 그 지역 대표 좌표 반경 20km를 조회한다")
    void searchByRegion() throws Exception {
        given(tourPlaceClient.findNearby(33.4996, 126.5312, 20_000, "12")).willReturn(List.of(place("1", "img")));

        mockMvc.perform(get("/api/v1/tours/places").param("areaCode", "39").param("contentTypeId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"))
                .andExpect(jsonPath("$[0].lat").value(33.5));
    }

    @Test
    @DisplayName("키워드가 있으면 키워드 검색을 하고 'all' 지역은 조건에서 뺀다")
    void searchByKeyword() throws Exception {
        given(tourPlaceClient.searchByKeyword("경복궁", null, null)).willReturn(List.of(place("2", "")));

        mockMvc.perform(get("/api/v1/tours/places").param("keyword", " 경복궁 ").param("areaCode", "all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("2"));
    }

    @Test
    @DisplayName("지역도 키워드도 없으면 외부 호출 없이 빈 목록")
    void searchWithoutCondition() throws Exception {
        mockMvc.perform(get("/api/v1/tours/places"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        verifyNoInteractions(tourPlaceClient);
    }

    @Test
    @DisplayName("검색어가 30자를 넘으면 400")
    void keywordTooLong() throws Exception {
        mockMvc.perform(get("/api/v1/tours/places").param("keyword", "가".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("keyword"));
    }

    @Test
    @DisplayName("주변 검색 반경은 최대 20km로 맞추고, 좌표가 범위를 벗어나면 400")
    void nearby() throws Exception {
        given(tourPlaceClient.findNearby(anyDouble(), anyDouble(), anyInt(), any())).willReturn(List.of());

        mockMvc.perform(get("/api/v1/tours/places/nearby")
                        .param("lat", "37.5").param("lng", "127.0").param("radius", "50000"))
                .andExpect(status().isOk());
        verify(tourPlaceClient).findNearby(37.5, 127.0, 20_000, null);

        mockMvc.perform(get("/api/v1/tours/places/nearby").param("lat", "999").param("lng", "127").param("radius", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("lat"));
    }

    @Test
    @DisplayName("상세가 없으면 404 PLACE_NOT_FOUND")
    void detail() throws Exception {
        given(tourPlaceClient.findDetail("126508")).willReturn(Optional.of(place("126508", "img")));
        given(tourPlaceClient.findDetail("1")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/tours/places/{id}", "126508"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("장소126508"));
        mockMvc.perform(get("/api/v1/tours/places/{id}", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("추천은 사진 있는 관광지만 최대 12곳")
    void featured() throws Exception {
        List<PlaceInfo> places = IntStream.range(0, 20)
                .mapToObj(i -> place(String.valueOf(i), i % 4 == 0 ? "" : "img" + i))
                .toList();
        given(tourPlaceClient.findByArea(null, "12")).willReturn(places);

        mockMvc.perform(get("/api/v1/tours/places/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[*].image", everyItem(not(""))));
    }

    @Test
    @DisplayName("TourAPI 장애는 502 TOUR_API_UNAVAILABLE")
    void externalFailure() throws Exception {
        given(tourPlaceClient.searchByKeyword("바다", null, null))
                .willThrow(new TourApiUnavailableException("SERVICE_KEY_IS_NOT_REGISTERED_ERROR", null));

        mockMvc.perform(get("/api/v1/tours/places").param("keyword", "바다"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("TOUR_API_UNAVAILABLE"));
    }
}
