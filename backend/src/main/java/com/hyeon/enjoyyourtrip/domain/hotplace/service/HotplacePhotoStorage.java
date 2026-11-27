package com.hyeon.enjoyyourtrip.domain.hotplace.service;

import java.util.Optional;

import org.springframework.core.io.Resource;

/**
 * 핫플레이스 사진 저장소(DIP). 구현체는 infrastructure.storage.service.LocalHotplacePhotoStorage(서버 로컬 폴더).
 * 배포 환경에서 S3 등으로 바꿀 때는 이 인터페이스의 구현체만 바꾸면 된다.
 */
public interface HotplacePhotoStorage {

    /**
     * 실제 이미지인지 확인하고 크기를 줄여 JPEG로 저장한다(메타데이터 제거).
     *
     * @return 저장된 파일명
     * @throws com.hyeon.enjoyyourtrip.domain.hotplace.exception.InvalidPhotoException 이미지가 아니거나 너무 큼
     */
    String save(byte[] photo);

    /** 파일명이 형식에 맞지 않거나(경로 조작 등) 파일이 없으면 empty */
    Optional<Resource> load(String fileName);

    /** 파일명이 null이거나 파일이 없으면 아무것도 하지 않는다. */
    void delete(String fileName);
}
