package com.hyeon.enjoyyourtrip.infrastructure.tour.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.tour-api.* — 주소·타임아웃은 spring.http.serviceclient.tour.* 에 있다.
 *
 * @param serviceKey 공공데이터포털 "디코딩" 서비스 키 (.env의 TOUR_API_KEY)
 * @param mobileApp  TourAPI 필수 파라미터 MobileApp (서비스명)
 */
@ConfigurationProperties(prefix = "app.tour-api")
public record TourApiProperties(String serviceKey, String mobileApp) {

    public boolean hasServiceKey() {
        return serviceKey != null && !serviceKey.isBlank();
    }
}
