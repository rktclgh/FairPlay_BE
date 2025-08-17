package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.dto.FileUploadResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AwsS3Service {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.aws.cloudfront.domain:}")
    private String cloudfrontDomain;

    // 파일 임시 저장
    public FileUploadResponseDto uploadTemp(MultipartFile file) {    // 백엔드 저장 전 임시 업로드
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf('.')))
                .orElse("");
        String uuid = UUID.randomUUID().toString();
        String key = "uploads/tmp" + LocalDate.now() + "/" + uuid + ext;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (S3Exception e) {
            log.error("S3 파일 업로드 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 업로드에 실패했습니다: " + e.getMessage());
        } catch (IOException e) {
            log.error("파일 스트림 읽기 실패. OriginalFileName: {}", file.getOriginalFilename(), e);
            throw new CustomException(HttpStatus.BAD_REQUEST, "업로드할 파일 스트림을 읽는 중 오류가 발생했습니다.");
        }

        log.info("Temporary file uploaded successfully - Key: {}, Original: {}, Size: {}",
                key, file.getOriginalFilename(), file.getSize());

        // 미리보기용 URL
        String downloadUrl = "/api/uploads/download?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

        // 이미지 여부
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");

        return new FileUploadResponseDto(key, downloadUrl, file.getOriginalFilename(), file.getContentType(), isImage);
    }

    // 파일 저장
    public String moveToPermanent(String key, String destPrefix) {
        String ext = key.substring(key.lastIndexOf('.'));
        String uuid = UUID.randomUUID().toString();
        String destKey = "uploads/" + destPrefix + "/" + uuid + ext;

        try {
            // 먼저 원본 파일이 존재하는지 확인
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());

            // 복사
            s3Client.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(key)
                    .destinationBucket(bucketName)
                    .destinationKey(destKey)
                    .build());

            // 복사 완료 후에만 원본 삭제
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());

            log.info("Successfully moved file from {} to {}", key, destKey);
            return destKey;

        } catch (NoSuchKeyException e) {
            log.error("Source file not found: {}", key, e);
            throw new CustomException(HttpStatus.NOT_FOUND, "영구 저장으로 이동할 임시 파일을 찾을 수 없습니다: " + key);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.error("Source file not found (S3 404): {}", key, e);
                throw new CustomException(HttpStatus.NOT_FOUND, "영구 저장으로 이동할 임시 파일을 찾을 수 없습니다: " + key);
            }
            log.error("Error moving file from {} to {}: {}", key, destKey, e.getMessage(), e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 이동 중 S3 오류가 발생했습니다.");
        }
    }

    // 파일 다운로드
    public void downloadFile(String key, HttpServletResponse response) throws IOException {
        try {
            ResponseInputStream<GetObjectResponse> s3is = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());

            response.setContentType(s3is.response().contentType() != null ? s3is.response().contentType() : "application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(key.substring(key.lastIndexOf('/') + 1), "UTF-8") + "\"");

            IOUtils.copy(s3is, response.getOutputStream());
            response.flushBuffer();
        } catch (S3Exception e) {
            log.error("S3 파일 다운로드 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없거나 다운로드 중 오류가 발생했습니다.");
        }
    }

    public String getPublicUrl(String key) {
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key)).toExternalForm();
    }

    /**
     * CloudFront를 통한 CDN URL 생성
     * CloudFront 도메인이 설정되어 있으면 CloudFront URL, 없으면 직접 S3 URL 반환
     */
    public String getCdnUrl(String key) {
        if (cloudfrontDomain != null && !cloudfrontDomain.trim().isEmpty()) {
            // CloudFront 도메인이 설정된 경우
            String cleanDomain = cloudfrontDomain.trim();
            if (!cleanDomain.startsWith("http://") && !cleanDomain.startsWith("https://")) {
                cleanDomain = "https://" + cleanDomain;
            }
            if (cleanDomain.endsWith("/")) {
                cleanDomain = cleanDomain.substring(0, cleanDomain.length() - 1);
            }
            String cleanKey = key.startsWith("/") ? key : "/" + key;
            return cleanDomain + cleanKey;
        } else {
            // CloudFront가 설정되지 않은 경우 직접 S3 URL 사용
            return getPublicUrl(key);
        }
    }

    public String getS3KeyFromPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        try {
            java.net.URI uri = java.net.URI.create(publicUrl.trim());
            String host = uri.getHost();
            String path = uri.getPath(); // 쿼리/프래그먼트 제외
            if (host == null || path == null) {
                return null;
            }

            // 1) CloudFront: 설정된 도메인/경로 하위인지 확인
            if (cloudfrontDomain != null && !cloudfrontDomain.trim().isEmpty()) {
                String cfg = cloudfrontDomain.trim();
                if (!cfg.startsWith("http://") && !cfg.startsWith("https://")) {
                    cfg = "https://" + cfg;
                }
                java.net.URI cfgUri = java.net.URI.create(cfg);
                String cfgHost = cfgUri.getHost();
                String cfgPath = Optional.ofNullable(cfgUri.getPath()).orElse("");
                if (cfgHost != null
                        && host.equalsIgnoreCase(cfgHost)
                        && (cfgPath.isEmpty() || path.startsWith(cfgPath))) {
                    String rel = path.substring(cfgPath.length());
                    String key = rel.startsWith("/") ? rel.substring(1) : rel;
                    return java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            // 2) S3 virtual-hosted-style: {bucket}.s3[.-]*.amazonaws.com/{key}
            String vhPrefix1 = bucketName + ".s3.";
            String vhPrefix2 = bucketName + ".s3-"; // s3-accelerate 등
            if (host.equalsIgnoreCase(bucketName + ".s3.amazonaws.com")
                    || host.startsWith(vhPrefix1)
                    || host.startsWith(vhPrefix2)) {
                String key = path.startsWith("/") ? path.substring(1) : path;
                return java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
            }

            // 3) S3 path-style: s3[.-]*.amazonaws.com/{bucket}/{key}
            if (host.equalsIgnoreCase("s3.amazonaws.com")
                    || host.startsWith("s3.")
                    || host.startsWith("s3-")) {
                String prefix = "/" + bucketName + "/";
                if (path.startsWith(prefix)) {
                    String key = path.substring(prefix.length());
                    return java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
                }
            }

            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 파일 삭제
    public void deleteFile(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            log.info("S3 파일 삭제 성공. Key: {}", key);
        } catch (S3Exception e) {
            log.error("S3 파일 삭제 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "S3 파일 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * S3에서 파일 존재 여부 확인
     */
    public boolean fileExists(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("파일 존재 여부 확인 중 S3 오류 발생 - Key: {}, 오류: {}", key, e.getMessage());
            return false;
        }
    }
}