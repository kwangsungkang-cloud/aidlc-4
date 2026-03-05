package com.tableorder.storage.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    /**
     * 이미지 파일을 S3에 업로드하고 URL을 반환한다.
     */
    public String upload(MultipartFile file, Long storeId) {
        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String key = String.format("menus/%d/%s.%s", storeId, UUID.randomUUID(), extension);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
        } catch (IOException e) {
            log.error("S3 이미지 업로드 실패: storeId={}, key={}", storeId, key, e);
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * S3에서 이미지를 삭제한다.
     */
    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(imageUrl);
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        } catch (Exception e) {
            // S3 삭제 실패는 로그만 남기고 비즈니스 로직에 영향 주지 않음
            log.warn("S3 이미지 삭제 실패: imageUrl={}", imageUrl, e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FORMAT,
                    "지원하지 않는 이미지 형식입니다 (jpg, png, webp만 허용)");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_SIZE_EXCEEDED,
                    "이미지 크기는 5MB 이하여야 합니다");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String extractKeyFromUrl(String imageUrl) {
        // https://{bucket}.s3.amazonaws.com/{key} 형식에서 key 추출
        String prefix = String.format("https://%s.s3.amazonaws.com/", bucket);
        if (imageUrl.startsWith(prefix)) {
            return imageUrl.substring(prefix.length());
        }
        // fallback: URL의 마지막 경로 부분 사용
        return imageUrl.substring(imageUrl.indexOf("menus/"));
    }
}
