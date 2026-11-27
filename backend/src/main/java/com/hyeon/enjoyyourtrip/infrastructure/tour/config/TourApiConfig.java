package com.hyeon.enjoyyourtrip.infrastructure.tour.config;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.ImportHttpServices;
import org.springframework.web.util.UriComponentsBuilder;

import com.hyeon.enjoyyourtrip.infrastructure.tour.client.TourApiClient;

/**
 * TourApiClient 프록시 등록. 주소·타임아웃은 application.yaml의 spring.http.serviceclient.tour.* 를 따르고,
 * 모든 요청에 TourAPI 공통 파라미터(serviceKey, MobileOS, MobileApp, _type=json)를 붙인다.
 */
@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
@ImportHttpServices(group = TourApiConfig.GROUP, types = TourApiClient.class)
public class TourApiConfig {

    public static final String GROUP = "tour";

    @Bean
    public RestClientHttpServiceGroupConfigurer tourApiGroupConfigurer(TourApiProperties properties) {
        return groups -> groups.filterByName(GROUP)
                .forEachClient((group, builder) -> builder.requestInterceptor(new CommonParameterInterceptor(properties)));
    }

    /**
     * 공통 파라미터를 쿼리에 덧붙인다. 서비스 키에는 +, /, = 가 섞여 있어 직접 인코딩한 뒤
     * 이미 인코딩된 URI로 조립(build(true))한다 — 키워드 등 기존 파라미터가 이중 인코딩되지 않게 하기 위함.
     */
    static final class CommonParameterInterceptor implements ClientHttpRequestInterceptor {

        private final TourApiProperties properties;

        CommonParameterInterceptor(TourApiProperties properties) {
            this.properties = properties;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            URI uri = UriComponentsBuilder.fromUri(request.getURI())
                    .queryParam("serviceKey", encode(properties.serviceKey()))
                    .queryParam("MobileOS", "ETC")
                    .queryParam("MobileApp", encode(properties.mobileApp()))
                    .queryParam("_type", "json")
                    .build(true)
                    .toUri();
            return execution.execute(new HttpRequestWrapper(request) {
                @Override
                public URI getURI() {
                    return uri;
                }
            }, body);
        }

        private static String encode(String value) {
            return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
        }
    }
}
