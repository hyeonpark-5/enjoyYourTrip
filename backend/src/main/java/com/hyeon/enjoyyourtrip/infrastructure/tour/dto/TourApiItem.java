package com.hyeon.enjoyyourtrip.infrastructure.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * TourAPI 응답의 item 한 건 (response.body.items.item[]). 필드명은 TourAPI 그대로이며 값은 모두 문자열로 온다.
 * 좌표: mapx = 경도(lng), mapy = 위도(lat)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiItem(
        String contentid,
        String contenttypeid,
        String title,
        String addr1,
        String addr2,
        String areacode,
        String firstimage,
        String tel,
        String overview,
        String mapx,
        String mapy) {
}
