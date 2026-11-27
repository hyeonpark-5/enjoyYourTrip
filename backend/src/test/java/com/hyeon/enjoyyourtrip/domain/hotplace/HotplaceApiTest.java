package com.hyeon.enjoyyourtrip.domain.hotplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.hyeon.enjoyyourtrip.domain.tour.service.TourPlaceClient;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;
import com.hyeon.enjoyyourtrip.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;

/**
 * 핫플레이스 API. 장소 정보는 가짜 TourPlaceClient가 돌려준다.
 * 테스트 트랜잭션은 롤백되므로, 새로 저장된 사진 파일도 롤백 시 지워진다(서비스의 정리 로직).
 */
class HotplaceApiTest extends ApiTestSupport {

    static final String GYEONGBOKGUNG = "126508";

    @MockitoBean
    TourPlaceClient tourPlaceClient;

    Tokens owner;
    Tokens other;

    @BeforeEach
    void setUp() throws Exception {
        given(tourPlaceClient.findDetail(GYEONGBOKGUNG)).willReturn(Optional.of(new PlaceInfo(
                GYEONGBOKGUNG, "경복궁", "1", "12", "서울특별시 종로구 사직로 161", "http://img/gbg.jpg", "", "개요", 37.579, 126.977)));
        given(tourPlaceClient.findDetail("999")).willReturn(Optional.empty());
        signup("주인", "owner@example.com");
        signup("남", "other@example.com");
        owner = login("owner@example.com");
        other = login("other@example.com");
    }

    static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    static MockMultipartHttpServletRequestBuilder createRequest(String placeId, String date, byte[] photo) {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/hotplaces");
        if (photo != null) {
            request.file(new MockMultipartFile("photo", "photo.png", MediaType.IMAGE_PNG_VALUE, photo));
        }
        request.param("placeId", placeId).param("visitedDate", date).param("description", "궁이 멋졌다");
        return request;
    }

    long create(Tokens tokens, String date, byte[] photo) throws Exception {
        MvcResult result = mockMvc.perform(createRequest(GYEONGBOKGUNG, date, photo)
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Test
    @DisplayName("관광지 정보를 복사해 등록하고, 유형을 비우면 관광지 종류(12→관광지)로 정한다")
    void create() throws Exception {
        mockMvc.perform(createRequest(GYEONGBOKGUNG, "2026-11-01", null)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placeName").value("경복궁"))
                .andExpect(jsonPath("$.lat").value(37.579))
                .andExpect(jsonPath("$.placeType").value("TOURIST_SPOT"))
                .andExpect(jsonPath("$.placeTypeLabel").value("관광지"))
                .andExpect(jsonPath("$.visitedDate").value("2026-11-01"))
                .andExpect(jsonPath("$.photoUrl").value(nullValue()));
    }

    @Test
    @DisplayName("사진은 960x720 안으로 줄인 JPEG로 저장되고 본인만 받을 수 있다")
    void photo() throws Exception {
        long id = create(owner, "2026-11-01", png(2000, 1000));

        MvcResult result = mockMvc.perform(get("/api/v1/hotplaces/{id}/photo", id)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andReturn();
        BufferedImage saved = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        assertThat(saved.getWidth()).isEqualTo(960);
        assertThat(saved.getHeight()).isEqualTo(480);

        mockMvc.perform(get("/api/v1/hotplaces/{id}/photo", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("HOTPLACE_FORBIDDEN"));
    }

    @Test
    @DisplayName("이미지가 아닌 파일은 400 INVALID_PHOTO")
    void invalidPhoto() throws Exception {
        mockMvc.perform(createRequest(GYEONGBOKGUNG, "2026-11-01", "not an image".getBytes())
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHOTO"));
    }

    @Test
    @DisplayName("같은 장소를 같은 날 두 번 등록하면 409, 미래 날짜는 400, 없는 관광지는 404")
    void rules() throws Exception {
        create(owner, "2026-11-01", null);

        mockMvc.perform(createRequest(GYEONGBOKGUNG, "2026-11-01", null).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_HOTPLACE"));
        mockMvc.perform(createRequest(GYEONGBOKGUNG, LocalDate.now().plusDays(3).toString(), null)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VISITED_DATE"));
        mockMvc.perform(createRequest("999", "2026-11-01", null).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"));
        // 다른 회원은 같은 장소·날짜로 등록할 수 있다
        create(other, "2026-11-01", null);
    }

    @Test
    @DisplayName("필수값이 없으면 400, 로그인하지 않으면 401")
    void validationAndAuth() throws Exception {
        mockMvc.perform(multipart("/api/v1/hotplaces").param("placeId", GYEONGBOKGUNG)
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mockMvc.perform(createRequest(GYEONGBOKGUNG, "2026-11-01", null))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내 목록은 최신순이고 남의 핫플레이스는 보이지 않는다")
    void findMine() throws Exception {
        create(owner, "2026-10-01", null);
        create(owner, "2026-10-02", null);
        create(other, "2026-10-03", null);

        mockMvc.perform(get("/api/v1/hotplaces").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].visitedDate").value("2026-10-02"));
    }

    @Test
    @DisplayName("수정: 유형·날짜·설명을 바꾸고 사진을 지울 수 있으며, 남의 것은 403")
    void update() throws Exception {
        long id = create(owner, "2026-11-01", png(100, 100));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/hotplaces/{id}", id)
                        .param("placeType", "CULTURE").param("visitedDate", "2026-10-31")
                        .param("description", "다시 가고 싶다").param("removePhoto", "true")
                        .header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.placeTypeLabel").value("문화시설"))
                .andExpect(jsonPath("$.visitedDate").value("2026-10-31"))
                .andExpect(jsonPath("$.photoUrl").value(nullValue()));
        mockMvc.perform(get("/api/v1/hotplaces/{id}/photo", id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/hotplaces/{id}", id)
                        .param("placeType", "ETC").param("visitedDate", "2026-10-31").param("description", "x")
                        .header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("삭제하면 404가 되고, 남의 것은 지울 수 없다")
    void deleteHotplace() throws Exception {
        long id = create(owner, "2026-11-01", null);

        mockMvc.perform(delete("/api/v1/hotplaces/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/hotplaces/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/hotplaces/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("HOTPLACE_NOT_FOUND"));
    }

    @Test
    @DisplayName("장소 유형 9개를 돌려준다")
    void placeTypes() throws Exception {
        mockMvc.perform(get("/api/v1/hotplaces/place-types").header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(9)))
                .andExpect(jsonPath("$[0].label").value("관광지"));
    }
}
