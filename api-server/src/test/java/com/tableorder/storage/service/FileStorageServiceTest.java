package com.tableorder.storage.service;

import com.tableorder.common.exception.BusinessException;
import com.tableorder.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    @Mock
    private S3Client s3Client;

    private void setBucket(String bucket) {
        ReflectionTestUtils.setField(fileStorageService, "bucket", bucket);
    }

    @Nested
    @DisplayName("upload - 이미지 업로드")
    class Upload {

        @Test
        @DisplayName("JPEG 이미지를 정상 업로드한다")
        void jpeg_업로드_성공() throws IOException {
            // given
            setBucket("test-bucket");
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getContentType()).willReturn("image/jpeg");
            given(file.getSize()).willReturn(1024L);
            given(file.getOriginalFilename()).willReturn("photo.jpg");
            given(file.getInputStream()).willReturn(new ByteArrayInputStream(new byte[1024]));
            given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .willReturn(PutObjectResponse.builder().build());

            // when
            String url = fileStorageService.upload(file, 1L);

            // then
            assertThat(url).startsWith("https://test-bucket.s3.amazonaws.com/menus/1/");
            assertThat(url).endsWith(".jpg");
        }

        @Test
        @DisplayName("지원하지 않는 이미지 형식이면 예외를 던진다")
        void 지원하지_않는_형식_예외() {
            // given
            setBucket("test-bucket");
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getContentType()).willReturn("image/gif");

            // when & then
            assertThatThrownBy(() -> fileStorageService.upload(file, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_IMAGE_FORMAT);
        }

        @Test
        @DisplayName("5MB 초과 이미지면 예외를 던진다")
        void 크기_초과_예외() {
            // given
            setBucket("test-bucket");
            MultipartFile file = mock(MultipartFile.class);
            given(file.isEmpty()).willReturn(false);
            given(file.getContentType()).willReturn("image/jpeg");
            given(file.getSize()).willReturn(6 * 1024 * 1024L); // 6MB

            // when & then
            assertThatThrownBy(() -> fileStorageService.upload(file, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getErrorCode())
                    .isEqualTo(ErrorCode.IMAGE_SIZE_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("delete - 이미지 삭제")
    class Delete {

        @Test
        @DisplayName("S3 이미지를 정상 삭제한다")
        void 삭제_성공() {
            // given
            setBucket("test-bucket");
            String imageUrl = "https://test-bucket.s3.amazonaws.com/menus/1/uuid.jpg";

            // when
            fileStorageService.delete(imageUrl);

            // then
            then(s3Client).should().deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("null URL이면 삭제를 건너뛴다")
        void null_url_스킵() {
            // when
            fileStorageService.delete(null);

            // then
            then(s3Client).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("빈 URL이면 삭제를 건너뛴다")
        void 빈_url_스킵() {
            // when
            fileStorageService.delete("");

            // then
            then(s3Client).shouldHaveNoInteractions();
        }
    }
}
