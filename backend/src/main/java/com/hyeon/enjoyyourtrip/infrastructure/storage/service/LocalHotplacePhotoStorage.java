package com.hyeon.enjoyyourtrip.infrastructure.storage.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.hyeon.enjoyyourtrip.domain.hotplace.exception.InvalidPhotoException;
import com.hyeon.enjoyyourtrip.domain.hotplace.service.HotplacePhotoStorage;
import com.hyeon.enjoyyourtrip.infrastructure.storage.config.StorageProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 서버 로컬 폴더(app.storage.hotplace-image-dir)에 사진을 저장한다.
 * 확장자나 Content-Type은 믿지 않고 실제 이미지로 읽히는지 확인한 뒤, 960x720 안으로 줄여 JPEG로 다시 저장한다
 * (위치정보 등 메타데이터가 함께 제거된다). 파일명은 UUID라 추측할 수 없고 경로 조작도 막는다.
 */
@Slf4j
@Component
public class LocalHotplacePhotoStorage implements HotplacePhotoStorage {

    private static final int MAX_WIDTH = 960;
    private static final int MAX_HEIGHT = 720;
    /** 디코딩 폭탄(작은 파일, 거대한 해상도) 차단 */
    private static final long MAX_PIXELS = 40_000_000L;
    private static final float QUALITY = 0.82f;
    private static final Pattern FILE_NAME =
            Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");
    private static final String NOT_IMAGE = "이미지 파일이 아니거나 지원하지 않는 형식입니다.";

    private final Path directory;

    public LocalHotplacePhotoStorage(StorageProperties properties) {
        this.directory = properties.hotplaceImageDir().toAbsolutePath().normalize();
    }

    @Override
    public String save(byte[] photo) {
        BufferedImage resized = fit(decode(photo));
        String name = UUID.randomUUID() + ".jpg";
        Path target = directory.resolve(name);
        try {
            Files.createDirectories(directory);
            Files.write(target, toJpeg(resized));
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 저장하지 못했습니다: " + target, e);
        }
        return name;
    }

    @Override
    public Optional<Resource> load(String fileName) {
        return resolve(fileName).filter(Files::isRegularFile).map(FileSystemResource::new);
    }

    @Override
    public void delete(String fileName) {
        resolve(fileName).ifPresent(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("사진 파일을 지우지 못했습니다: {}", path, e);
            }
        });
    }

    private Optional<Path> resolve(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            return Optional.empty();
        }
        return Optional.of(directory.resolve(fileName));
    }

    private static BufferedImage decode(byte[] bytes) {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = stream == null ? null : ImageIO.getImageReaders(stream);
            if (readers == null || !readers.hasNext()) {
                throw new InvalidPhotoException(NOT_IMAGE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(stream);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new InvalidPhotoException("사진 해상도가 너무 큽니다. 더 작은 사진을 선택해 주세요.");
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new InvalidPhotoException(NOT_IMAGE);
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof InvalidPhotoException invalid) {
                throw invalid;
            }
            throw new InvalidPhotoException(NOT_IMAGE);
        }
    }

    /** 비율을 유지해 960x720 안으로 줄이고 RGB로 바꾼다(투명 배경은 흰색). */
    private static BufferedImage fit(BufferedImage source) {
        double scale = Math.min(1.0,
                Math.min((double) MAX_WIDTH / source.getWidth(), (double) MAX_HEIGHT / source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static byte[] toJpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(buffer)) {
            writer.setOutput(out);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return buffer.toByteArray();
    }
}
