package com.hyeon.enjoyyourtrip.domain.tour.entity;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 전국 17개 지역과 대표 좌표 (JPA 엔티티가 아닌 도메인 상수). code는 TourAPI의 areaCode.
 */
@Getter
@RequiredArgsConstructor
public enum Region {
    SEOUL("1", "서울", 37.5665, 126.9780),
    INCHEON("2", "인천", 37.4563, 126.7052),
    DAEJEON("3", "대전", 36.3504, 127.3845),
    DAEGU("4", "대구", 35.8714, 128.6014),
    GWANGJU("5", "광주", 35.1595, 126.8526),
    BUSAN("6", "부산", 35.1796, 129.0756),
    ULSAN("7", "울산", 35.5384, 129.3114),
    SEJONG("8", "세종", 36.4800, 127.2890),
    GYEONGGI("31", "경기", 37.2636, 127.0286),
    GANGWON("32", "강원", 37.8228, 128.1555),
    CHUNGBUK("33", "충북", 36.6357, 127.4913),
    CHUNGNAM("34", "충남", 36.6588, 126.6728),
    GYEONGBUK("35", "경북", 36.5760, 128.5056),
    GYEONGNAM("36", "경남", 35.2383, 128.6924),
    JEONBUK("37", "전북", 35.8203, 127.1088),
    JEONNAM("38", "전남", 34.8161, 126.4629),
    JEJU("39", "제주", 33.4996, 126.5312);

    private final String code;
    private final String koreanName;
    private final double lat;
    private final double lng;

    public static Optional<Region> fromCode(String code) {
        return Arrays.stream(values()).filter(region -> region.code.equals(code)).findFirst();
    }
}
