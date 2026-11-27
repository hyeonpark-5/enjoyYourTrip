package com.hyeon.enjoyyourtrip.infrastructure.storage.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.storage.* — 업로드 파일 저장 위치. 서버를 다시 빌드·배포해도 지워지지 않도록 프로젝트 밖 폴더를 쓴다.
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(Path hotplaceImageDir) {
}
