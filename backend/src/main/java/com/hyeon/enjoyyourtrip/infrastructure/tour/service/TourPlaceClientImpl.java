package com.hyeon.enjoyyourtrip.infrastructure.tour.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import com.hyeon.enjoyyourtrip.domain.tour.exception.TourApiUnavailableException;
import com.hyeon.enjoyyourtrip.domain.tour.service.TourPlaceClient;
import com.hyeon.enjoyyourtrip.infrastructure.tour.client.TourApiClient;
import com.hyeon.enjoyyourtrip.infrastructure.tour.config.TourApiProperties;
import com.hyeon.enjoyyourtrip.infrastructure.tour.config.TourCacheConfig;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.PlaceInfo;
import com.hyeon.enjoyyourtrip.infrastructure.tour.dto.TourApiItem;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * TourAPI 실제 호출 구현체. 외부 응답(TourApiItem)을 내 도메인 DTO(PlaceInfo)로 바꾸고,
 * 외부 예외(네트워크·인증·형식 오류)를 도메인 예외(TourApiUnavailableException)로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class TourPlaceClientImpl implements TourPlaceClient {

    private static final int ROWS = 60;
    private static final int FIRST_PAGE = 1;
    /** 대표 이미지가 있는 항목을 제목순으로 */
    private static final String ARRANGE_TITLE_WITH_IMAGE = "O";
    private static final String ARRANGE_DISTANCE = "E";
    private static final Pattern XML_AUTH_MESSAGE = Pattern.compile("<returnAuthMsg>([^<]*)</returnAuthMsg>");

    /** 대한민국(마라도·독도·백령도 포함)을 넉넉히 감싸는 범위. 원본 데이터의 잘못된 좌표를 걸러낸다. */
    private static final double MIN_LAT = 32.5;
    private static final double MAX_LAT = 39.0;
    private static final double MIN_LNG = 123.5;
    private static final double MAX_LNG = 132.0;

    private final TourApiClient tourApiClient;
    private final TourApiProperties properties;
    private final JsonMapper jsonMapper;

    @Override
    @Cacheable(cacheNames = TourCacheConfig.AREA)
    public List<PlaceInfo> findByArea(String areaCode, String contentTypeId) {
        return toPlaces(call(() -> tourApiClient.areaBasedList(
                ROWS, FIRST_PAGE, ARRANGE_TITLE_WITH_IMAGE, areaCode, contentTypeId)));
    }

    @Override
    @Cacheable(cacheNames = TourCacheConfig.KEYWORD)
    public List<PlaceInfo> searchByKeyword(String keyword, String areaCode, String contentTypeId) {
        return toPlaces(call(() -> tourApiClient.searchKeyword(
                keyword, ROWS, FIRST_PAGE, ARRANGE_TITLE_WITH_IMAGE, areaCode, contentTypeId)));
    }

    @Override
    @Cacheable(cacheNames = TourCacheConfig.NEARBY)
    public List<PlaceInfo> findNearby(double lat, double lng, int radius, String contentTypeId) {
        return toPlaces(call(() -> tourApiClient.locationBasedList(
                lng, lat, radius, ROWS, FIRST_PAGE, ARRANGE_DISTANCE, contentTypeId)));
    }

    @Override
    @Cacheable(cacheNames = TourCacheConfig.DETAIL)
    public Optional<PlaceInfo> findDetail(String contentId) {
        return toPlaces(call(() -> tourApiClient.detailCommon(contentId))).stream().findFirst();
    }

    /** 호출 후 response.body.items.item 배열을 돌려준다. */
    private List<JsonNode> call(Supplier<String> request) {
        if (!properties.hasServiceKey()) {
            throw new TourApiUnavailableException("TOUR_API_KEY 미설정", null);
        }
        String body;
        try {
            body = request.get();
        } catch (RestClientException e) {
            throw new TourApiUnavailableException("통신 오류", e);
        }
        return parseItems(body);
    }

    List<JsonNode> parseItems(String body) {
        if (body == null || body.isBlank()) {
            throw new TourApiUnavailableException("빈 응답", null);
        }
        if (body.stripLeading().startsWith("<")) {
            // 인증키 오류 등은 XML로 온다: <returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR</returnAuthMsg>
            Matcher matcher = XML_AUTH_MESSAGE.matcher(body);
            throw new TourApiUnavailableException(matcher.find() ? matcher.group(1) : "XML 오류 응답", null);
        }
        JsonNode root;
        try {
            root = jsonMapper.readTree(body);
        } catch (JacksonException e) {
            throw new TourApiUnavailableException("응답 형식 오류", e);
        }
        JsonNode response = root.path("response");
        String resultCode = response.path("header").path("resultCode").asString("");
        if (!"0000".equals(resultCode)) {
            String message = response.path("header").path("resultMsg").asString(root.path("resultMsg").asString("알 수 없는 오류"));
            throw new TourApiUnavailableException(message, null);
        }
        // 결과가 없으면 items가 ""(빈 문자열), 한 건이면 item이 배열이 아닌 객체로 온다.
        JsonNode item = response.path("body").path("items").path("item");
        List<JsonNode> items = new ArrayList<>();
        if (item.isArray()) {
            item.forEach(items::add);
        } else if (item.isObject()) {
            items.add(item);
        }
        return items;
    }

    private List<PlaceInfo> toPlaces(List<JsonNode> items) {
        return items.stream()
                .map(node -> jsonMapper.treeToValue(node, TourApiItem.class))
                .map(this::toPlace)
                .flatMap(Optional::stream)
                .toList();
    }

    /** 좌표가 없거나 국내 범위 밖이면 지도에 표시할 수 없으므로 제외한다. */
    private Optional<PlaceInfo> toPlace(TourApiItem item) {
        double lat;
        double lng;
        try {
            lat = Double.parseDouble(text(item.mapy()));
            lng = Double.parseDouble(text(item.mapx()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
        if (lat < MIN_LAT || lat > MAX_LAT || lng < MIN_LNG || lng > MAX_LNG) {
            return Optional.empty();
        }
        return Optional.of(new PlaceInfo(
                text(item.contentid()),
                text(item.title()),
                text(item.areacode()),
                text(item.contenttypeid()),
                (text(item.addr1()) + " " + text(item.addr2())).trim(),
                text(item.firstimage()),
                text(item.tel()),
                stripHtml(text(item.overview())),
                lat,
                lng));
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String stripHtml(String html) {
        return html.replaceAll("(?i)<br\\s*/?>", "\n").replaceAll("<[^>]+>", "").trim();
    }
}
