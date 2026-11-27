package com.hyeon.enjoyyourtrip.domain.hotplace.exception;

import com.hyeon.enjoyyourtrip.global.exception.InvalidValueException;

/** 이미지가 아니거나 해상도가 너무 큰 사진 → 400 */
public class InvalidPhotoException extends InvalidValueException {

    public InvalidPhotoException(String message) {
        super("INVALID_PHOTO", message);
    }
}
