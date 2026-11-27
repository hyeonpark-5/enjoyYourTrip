package com.hyeon.enjoyyourtrip.infrastructure.tour.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * 한국관광공사 국문 관광정보 서비스(KorService2) HTTP 호출 인터페이스.
 * serviceKey·MobileOS·MobileApp·_type 공통 파라미터는 TourApiConfig의 인터셉터가 붙인다.
 * 인증키 오류 시 JSON이 아닌 XML로 응답하므로 본문을 문자열로 받아 TourPlaceClientImpl에서 해석한다.
 * null인 @RequestParam은 쿼리에서 빠진다.
 */
@HttpExchange
public interface TourApiClient {

    @GetExchange("/areaBasedList2")
    String areaBasedList(
            @RequestParam int numOfRows,
            @RequestParam int pageNo,
            @RequestParam String arrange,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String contentTypeId);

    @GetExchange("/searchKeyword2")
    String searchKeyword(
            @RequestParam String keyword,
            @RequestParam int numOfRows,
            @RequestParam int pageNo,
            @RequestParam String arrange,
            @RequestParam(required = false) String areaCode,
            @RequestParam(required = false) String contentTypeId);

    /** mapX = 경도, mapY = 위도, radius = 미터(최대 20000) */
    @GetExchange("/locationBasedList2")
    String locationBasedList(
            @RequestParam double mapX,
            @RequestParam double mapY,
            @RequestParam int radius,
            @RequestParam int numOfRows,
            @RequestParam int pageNo,
            @RequestParam String arrange,
            @RequestParam(required = false) String contentTypeId);

    @GetExchange("/detailCommon2")
    String detailCommon(@RequestParam String contentId);
}
