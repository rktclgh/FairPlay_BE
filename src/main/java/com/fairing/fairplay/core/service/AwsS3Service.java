package com.fairing.fairplay.core.service;

import com.fairing.fairplay.core.dto.FileUploadResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
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

    // 파일 임시 저장
    public FileUploadResponseDto uploadTemp(MultipartFile file) throws IOException {    // 백엔드 저장 전 임시 업로드
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf('.')))
                .orElse("");
        String uuid = UUID.randomUUID().toString();
        String key = "uploads/tmp" + LocalDate.now() + "/" + uuid + ext;

        // S3에 업로드
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );

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

        // 복사
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(key)
                .destinationBucket(bucketName)
                .destinationKey(destKey)
                .build());

        // 원본 삭제
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());

        return destKey; // DB에 저장할 URL
    }

    // 파일 다운로드
    public void downloadFile(String key, HttpServletResponse response) throws IOException {
        ResponseInputStream<GetObjectResponse> s3is = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());

        response.setContentType(s3is.response().contentType() != null ? s3is.response().contentType() : "application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(key.substring(key.lastIndexOf('/')+1), "UTF-8") + "\"");

        IOUtils.copy(s3is, response.getOutputStream());
        response.flushBuffer();
    }

    public String getPublicUrl(String key) {
        return s3Client.utilities().getUrl(builder -> builder.bucket(bucketName).key(key)).toExternalForm();
    }

    public String getS3KeyFromPublicUrl(String publicUrl) {
        String bucketUrl = "https://" + bucketName + ".s3.";
        if (publicUrl.startsWith(bucketUrl)) {
            String urlWithoutSchema = publicUrl.substring(8);
            return urlWithoutSchema.substring(urlWithoutSchema.indexOf("/") + 1);
        }
        return null;
    }

    // 파일 삭제
    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build());
    }
}
