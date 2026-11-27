package com.hyeon.enjoyyourtrip.domain.hotplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import com.hyeon.enjoyyourtrip.domain.hotplace.repository.HotplaceRepository;
import com.hyeon.enjoyyourtrip.domain.member.repository.MemberRepository;
import com.hyeon.enjoyyourtrip.domain.tour.service.TourPlaceClient;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;

/**
 * 실제 내장 Tomcat으로 multipart 등록(POST)·수정(PATCH)이 해석되는지 확인한다.
 * (MockMvc는 서블릿 컨테이너의 multipart 파싱을 거치지 않는다)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class HotplaceMultipartServerTest {

    @LocalServerPort
    int port;

    @MockitoBean
    TourPlaceClient tourPlaceClient;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    HotplaceRepository hotplaceRepository;

    @Test
    @DisplayName("실제 서버에서 multipart POST로 등록하고 PATCH로 사진을 바꾼다")
    void multipartOverRealServer() throws Exception {
        given(tourPlaceClient.findDetail("126508")).willReturn(Optional.of(new PlaceInfo(
                "126508", "경복궁", "1", "12", "서울", "", "", "", 37.579, 126.977)));
        RestClient client = RestClient.create("http://localhost:" + port);
        String email = "server-" + UUID.randomUUID() + "@example.com";
        Long memberId = null;
        try {
            Map<?, ?> member = client.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("name", "서버", "email", email, "password", "password1234"))
                    .retrieve().body(Map.class);
            memberId = ((Number) member.get("id")).longValue();
            String token = (String) client.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "password", "password1234"))
                    .retrieve().body(Map.class).get("accessToken");

            MultiValueMap<String, Object> create = form("placeId", "126508", "visitedDate", "2026-11-01",
                    "description", "실제 서버 등록");
            Map<?, ?> created = client.post().uri("/api/v1/hotplaces")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(create)
                    .retrieve().body(Map.class);
            assertThat(created.get("photoUrl")).isNotNull();

            MultiValueMap<String, Object> update = form("placeType", "CULTURE", "visitedDate", "2026-10-31",
                    "description", "실제 서버 수정");
            Map<?, ?> updated = client.patch().uri("/api/v1/hotplaces/{id}", created.get("id"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(update)
                    .retrieve().body(Map.class);
            assertThat(updated.get("placeType")).isEqualTo("CULTURE");
            assertThat(updated.get("description")).isEqualTo("실제 서버 수정");

            byte[] photo = client.get().uri("/api/v1/hotplaces/{id}/photo", created.get("id"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().body(byte[].class);
            assertThat(photo).isNotEmpty();

            client.delete().uri("/api/v1/hotplaces/{id}", created.get("id"))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .retrieve().toBodilessEntity();
        } finally {
            if (memberId != null) {
                hotplaceRepository.deleteAll(hotplaceRepository.findByOwnerIdOrderByIdDesc(memberId));
                memberRepository.deleteById(memberId);
            }
        }
    }

    /** 텍스트 필드 + photo 파일 파트 */
    static MultiValueMap<String, Object> form(String... keyValues) throws Exception {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            form.add(keyValues[i], keyValues[i + 1]);
        }
        form.add("photo", new ByteArrayResource(HotplaceApiTest.png(40, 30)) {
            @Override
            public String getFilename() {
                return "photo.png";
            }
        });
        return form;
    }
}
