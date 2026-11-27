package com.hyeon.enjoyyourtrip.domain.hotplace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hyeon.enjoyyourtrip.domain.hotplace.repository.HotplaceRepository;
import com.hyeon.enjoyyourtrip.domain.tour.service.TourPlaceClient;
import com.hyeon.enjoyyourtrip.infrastructure.storage.config.StorageProperties;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;
import com.hyeon.enjoyyourtrip.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;

/**
 * 실제로 커밋되는 흐름에서 사진 파일이 DB와 함께 정리되는지 확인한다.
 * 트랜잭션 없이 실행되므로 만든 데이터는 끝에서 직접 지운다.
 */
class HotplacePhotoFileTest extends ApiTestSupport {

    @MockitoBean
    TourPlaceClient tourPlaceClient;

    @Autowired
    HotplaceRepository hotplaceRepository;

    @Autowired
    StorageProperties storageProperties;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("커밋 후: 사진 교체 시 이전 파일, 삭제 시 남은 파일이 지워진다")
    void filesFollowCommits() throws Exception {
        given(tourPlaceClient.findDetail("126508")).willReturn(Optional.of(new PlaceInfo(
                "126508", "경복궁", "1", "12", "서울", "", "", "", 37.579, 126.977)));
        String email = "file-" + UUID.randomUUID() + "@example.com";
        long memberId = signup("파일", email);
        try {
            Tokens tokens = login(email);
            Set<String> before = files();

            MvcResult created = mockMvc.perform(multipart("/api/v1/hotplaces")
                            .file(photo()).param("placeId", "126508").param("visitedDate", "2026-11-01")
                            .param("description", "사진 테스트").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                    .andExpect(status().isCreated())
                    .andReturn();
            long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
            Set<String> first = newFiles(before);
            assertThat(first).hasSize(1);

            mockMvc.perform(multipart(HttpMethod.PATCH, "/api/v1/hotplaces/{id}", id)
                            .file(photo()).param("placeType", "TOURIST_SPOT").param("visitedDate", "2026-11-01")
                            .param("description", "사진 교체").header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                    .andExpect(status().isOk());
            Set<String> second = newFiles(before);
            assertThat(second).hasSize(1).doesNotContainAnyElementsOf(first);

            mockMvc.perform(delete("/api/v1/hotplaces/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(tokens)))
                    .andExpect(status().isNoContent());
            assertThat(newFiles(before)).isEmpty();
        } finally {
            hotplaceRepository.deleteAll(hotplaceRepository.findByOwnerIdOrderByIdDesc(memberId));
            memberRepository.deleteById(memberId);
        }
    }

    static MockMultipartFile photo() throws IOException {
        return new MockMultipartFile("photo", "p.png", MediaType.IMAGE_PNG_VALUE, HotplaceApiTest.png(50, 50));
    }

    Set<String> newFiles(Set<String> before) throws IOException {
        Set<String> now = files();
        now.removeAll(before);
        return now;
    }

    Set<String> files() throws IOException {
        Path dir = storageProperties.hotplaceImageDir();
        if (!Files.isDirectory(dir)) {
            return new java.util.HashSet<>();
        }
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.map(path -> path.getFileName().toString()).collect(Collectors.toSet());
        }
    }
}
