package com.hyeon.enjoyyourtrip.domain.member.exception;

import com.hyeon.enjoyyourtrip.global.exception.ExternalServiceException;

/** Keycloak Admin API 호출 실패 → 502 */
public class IdentityProviderUnavailableException extends ExternalServiceException {

    public IdentityProviderUnavailableException(String reason, Throwable cause) {
        super("IDENTITY_PROVIDER_UNAVAILABLE", "인증 서버와 통신하지 못했습니다. 잠시 후 다시 시도해주세요. (" + reason + ")", cause);
    }
}
