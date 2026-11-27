package com.hyeon.enjoyyourtrip.domain.tour.exception;

import com.hyeon.enjoyyourtrip.global.exception.ExternalServiceException;

/** TourAPI 호출 실패(네트워크, 인증키 오류, 응답 형식 오류) → 502 */
public class TourApiUnavailableException extends ExternalServiceException {

    public TourApiUnavailableException(String reason, Throwable cause) {
        super("TOUR_API_UNAVAILABLE", "관광 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요. (" + reason + ")", cause);
    }
}
