package com.hyeon.enjoyyourtrip.infrastructure.tour.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import com.hyeon.enjoyyourtrip.domain.tour.exception.TourApiUnavailableException;
import com.hyeon.enjoyyourtrip.infrastructure.tour.client.TourApiClient;
import com.hyeon.enjoyyourtrip.infrastructure.tour.config.TourApiProperties;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;

import tools.jackson.databind.json.JsonMapper;

/**
 * TourAPI 응답 해석 단위 테스트. 실제 HTTP 호출 없이 응답 문자열을 바꿔 가며 검증한다.
 */
class TourPlaceClientImplTest {

    static final TourApiProperties WITH_KEY = new TourApiProperties("test-key", "EnjoyYourTrip");

    static String ok(String items) {
        return """
                {"response":{"header":{"resultCode":"0000","resultMsg":"OK"},
                 "body":{"items":%s,"numOfRows":60,"pageNo":1,"totalCount":1}}}
                """.formatted(items);
    }

    static String item(String id, String title, String mapx, String mapy) {
        return """
                {"contentid":"%s","contenttypeid":"12","title":"%s","addr1":"서울특별시 종로구","addr2":"사직로 161",
                 "areacode":"1","firstimage":"http://img/1.jpg","tel":"02-000-0000",
                 "overview":"첫 줄<br>둘째 <b>줄</b>","mapx":"%s","mapy":"%s","unknownField":"x"}
                """.formatted(id, title, mapx, mapy);
    }

    static TourPlaceClientImpl clientReturning(Supplier<String> body) {
        return new TourPlaceClientImpl(new StubTourApiClient(body), WITH_KEY, JsonMapper.builder().build());
    }

    @Test
    @DisplayName("item 배열을 PlaceInfo로 바꾸고 주소를 합치며 개요의 HTML 태그를 지운다")
    void mapsItems() {
        TourPlaceClientImpl client = clientReturning(() -> ok("{\"item\":[" + item("1", "경복궁", "126.977", "37.579") + "]}"));

        List<PlaceInfo> places = client.findByArea("1", "12");

        assertThat(places).singleElement().satisfies(place -> {
            assertThat(place.id()).isEqualTo("1");
            assertThat(place.address()).isEqualTo("서울특별시 종로구 사직로 161");
            assertThat(place.overview()).isEqualTo("첫 줄\n둘째 줄");
            assertThat(place.lat()).isEqualTo(37.579);
            assertThat(place.lng()).isEqualTo(126.977);
        });
    }

    @Test
    @DisplayName("좌표가 없거나 국내 범위를 벗어난 항목은 뺀다")
    void filtersInvalidCoordinates() {
        String items = "{\"item\":[" + item("1", "정상", "126.9", "37.5") + ","
                + item("2", "좌표없음", "", "") + ","
                + item("3", "바다한가운데", "0", "0") + "]}";

        assertThat(clientReturning(() -> ok(items)).findByArea(null, null))
                .extracting(PlaceInfo::id).containsExactly("1");
    }

    @Test
    @DisplayName("결과가 한 건이면 item이 객체로, 없으면 items가 빈 문자열로 와도 처리한다")
    void singleAndEmpty() {
        assertThat(clientReturning(() -> ok("{\"item\":" + item("7", "하나", "127.0", "37.0") + "}"))
                .findDetail("7")).map(PlaceInfo::name).contains("하나");
        assertThat(clientReturning(() -> ok("\"\"")).searchByKeyword("없음", null, null)).isEmpty();
    }

    @Test
    @DisplayName("resultCode가 0000이 아니면 resultMsg를 담아 TourApiUnavailableException")
    void resultCodeError() {
        String body = "{\"response\":{\"header\":{\"resultCode\":\"10\",\"resultMsg\":\"INVALID_REQUEST_PARAMETER_ERROR\"}}}";

        assertThatThrownBy(() -> clientReturning(() -> body).findByArea(null, null))
                .isInstanceOf(TourApiUnavailableException.class)
                .hasMessageContaining("INVALID_REQUEST_PARAMETER_ERROR");
    }

    @Test
    @DisplayName("인증키 오류 XML 응답은 returnAuthMsg를 담아 TourApiUnavailableException")
    void xmlAuthError() {
        String xml = "<OpenAPI_ServiceResponse><cmmMsgHeader><errMsg>SERVICE ERROR</errMsg>"
                + "<returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg></cmmMsgHeader></OpenAPI_ServiceResponse>";

        assertThatThrownBy(() -> clientReturning(() -> xml).findByArea(null, null))
                .isInstanceOf(TourApiUnavailableException.class)
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    @DisplayName("통신 오류와 키 미설정도 TourApiUnavailableException으로 바꾼다")
    void networkErrorAndMissingKey() {
        assertThatThrownBy(() -> clientReturning(() -> {
            throw new ResourceAccessException("timeout");
        }).findByArea(null, null))
                .isInstanceOf(TourApiUnavailableException.class)
                .hasCauseInstanceOf(ResourceAccessException.class);

        TourPlaceClientImpl noKey = new TourPlaceClientImpl(
                new StubTourApiClient(() -> ok("\"\"")), new TourApiProperties("", "app"), JsonMapper.builder().build());
        assertThatThrownBy(() -> noKey.findByArea(null, null))
                .isInstanceOf(TourApiUnavailableException.class)
                .hasMessageContaining("TOUR_API_KEY");
    }

    /** 어떤 오퍼레이션을 부르든 같은 응답을 돌려주는 가짜 클라이언트 */
    record StubTourApiClient(Supplier<String> body) implements TourApiClient {

        @Override
        public String areaBasedList(int numOfRows, int pageNo, String arrange, String areaCode, String contentTypeId) {
            return body.get();
        }

        @Override
        public String searchKeyword(String keyword, int numOfRows, int pageNo, String arrange, String areaCode,
                String contentTypeId) {
            return body.get();
        }

        @Override
        public String locationBasedList(double mapX, double mapY, int radius, int numOfRows, int pageNo,
                String arrange, String contentTypeId) {
            return body.get();
        }

        @Override
        public String detailCommon(String contentId) {
            return body.get();
        }
    }
}
