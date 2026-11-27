package com.hyeon.enjoyyourtrip.domain.post;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.hyeon.enjoyyourtrip.support.ApiTestSupport;
import com.jayway.jsonpath.JsonPath;

class PostApiTest extends ApiTestSupport {

    Tokens writer;
    Tokens other;
    Tokens admin;

    @BeforeEach
    void setUp() throws Exception {
        signup("작성자", "writer@example.com");
        signup("다른회원", "other@example.com");
        createAdmin("admin@example.com");
        writer = login("writer@example.com");
        other = login("other@example.com");
        admin = login("admin@example.com");
    }

    long write(Tokens tokens, String type, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokens))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("type", type, "title", title, "content", content)))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    @Nested
    @DisplayName("작성")
    class Write {

        @Test
        @DisplayName("회원은 공유글을 쓸 수 있고 작성자 정보가 함께 내려온다")
        void sharePost() throws Exception {
            mockMvc.perform(post("/api/v1/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(writer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("type", "POST", "title", "  제주 여행 후기 ", "content", "좋았어요")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("제주 여행 후기"))
                    .andExpect(jsonPath("$.authorName").value("작성자"))
                    .andExpect(jsonPath("$.viewCount").value(0))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("공지는 관리자만 쓸 수 있다")
        void noticeIsAdminOnly() throws Exception {
            mockMvc.perform(post("/api/v1/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(writer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("type", "NOTICE", "title", "공지", "content", "내용")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));

            write(admin, "NOTICE", "공지", "내용");
        }

        @Test
        @DisplayName("로그인하지 않으면 401을 돌려준다")
        void requiresLogin() throws Exception {
            mockMvc.perform(post("/api/v1/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("type", "POST", "title", "제목", "content", "내용")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("제목이 비었거나 80자를 넘으면 400을 돌려준다")
        void validation() throws Exception {
            mockMvc.perform(post("/api/v1/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(writer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("type", "POST", "title", " ", "content", "내용")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("title"));
            mockMvc.perform(post("/api/v1/posts")
                            .header(HttpHeaders.AUTHORIZATION, bearer(writer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("type", "POST", "title", "가".repeat(81), "content", "내용")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("목록·상세")
    class Read {

        @Test
        @DisplayName("로그인 없이 종류별 최신순 목록을 보고, 제목·내용·작성자로 검색한다")
        void searchWithoutLogin() throws Exception {
            write(writer, "POST", "부산 맛집", "돼지국밥");
            write(other, "POST", "강릉 바다", "커피거리");
            write(admin, "NOTICE", "점검 안내", "서버 점검");

            mockMvc.perform(get("/api/v1/posts").param("type", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[0].title").value("강릉 바다"))
                    .andExpect(jsonPath("$.content[0].content").doesNotExist());
            mockMvc.perform(get("/api/v1/posts").param("type", "POST").param("key", "content").param("word", "국밥"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("부산 맛집"));
            mockMvc.perform(get("/api/v1/posts").param("type", "POST").param("key", "author").param("word", "다른"))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].authorName").value("다른회원"));
            mockMvc.perform(get("/api/v1/posts").param("type", "NOTICE").param("key", "title").param("word", "점검"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("게시판 종류가 없으면 400을 돌려준다")
        void typeRequired() throws Exception {
            mockMvc.perform(get("/api/v1/posts"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        }

        @Test
        @DisplayName("상세를 볼 때마다 조회수가 오른다")
        void viewCount() throws Exception {
            long id = write(writer, "POST", "제목", "내용");

            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(1))
                    .andExpect(jsonPath("$.content").value("내용"));
            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(jsonPath("$.viewCount").value(2));
        }

        @Test
        @DisplayName("없는 글은 404 POST_NOT_FOUND를 돌려준다")
        void notFound() throws Exception {
            mockMvc.perform(get("/api/v1/posts/{id}", 987654321L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        }

        @Test
        @DisplayName("작성자가 탈퇴하면 작성자 이름이 '탈퇴한 회원'으로 보인다")
        void withdrawnAuthor() throws Exception {
            long memberId = signup("떠난회원", "gone@example.com");
            long id = write(login("gone@example.com"), "POST", "제목", "내용");
            mockMvc.perform(delete("/api/v1/members/{id}", memberId).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(jsonPath("$.authorName").value("탈퇴한 회원"));
        }
    }

    @Nested
    @DisplayName("수정·삭제")
    class Modify {

        @Test
        @DisplayName("작성자와 관리자는 수정할 수 있고 다른 회원은 403")
        void edit() throws Exception {
            long id = write(writer, "POST", "원래 제목", "원래 내용");

            mockMvc.perform(patch("/api/v1/posts/{id}", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(other))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("title", "남의 글", "content", "수정")))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("POST_FORBIDDEN"));
            mockMvc.perform(patch("/api/v1/posts/{id}", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(writer))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("title", "바뀐 제목", "content", "바뀐 내용")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("바뀐 제목"))
                    .andExpect(jsonPath("$.type").value("POST"));
            mockMvc.perform(patch("/api/v1/posts/{id}", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json("title", "관리자 수정", "content", "내용")))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("다른 회원은 삭제할 수 없고 작성자가 삭제하면 404가 된다")
        void deleteByOwner() throws Exception {
            long id = write(writer, "POST", "제목", "내용");

            mockMvc.perform(delete("/api/v1/posts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(other)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(delete("/api/v1/posts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(writer)))
                    .andExpect(status().isNoContent());
            mockMvc.perform(get("/api/v1/posts/{id}", id))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("관리자는 다른 회원의 글을 삭제할 수 있다")
        void deleteByAdmin() throws Exception {
            long id = write(writer, "POST", "제목", "내용");

            mockMvc.perform(delete("/api/v1/posts/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                    .andExpect(status().isNoContent());
        }
    }
}
