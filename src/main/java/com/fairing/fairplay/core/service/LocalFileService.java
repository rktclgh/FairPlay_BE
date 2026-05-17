package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.dto.FileUploadResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileService {

    @Value("${app.base-url:https://fair-play.ink}")
    private String baseUrl;

    @Value("${app.upload.path:${user.home}/fairplay-uploads}")
    private String uploadBasePath;

    // 파일 임시 저장
    public FileUploadResponseDto uploadTemp(MultipartFile file) {
        String ext = Optional.ofNullable(file.getOriginalFilename())
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf('.')))
                .orElse("");
        String uuid = UUID.randomUUID().toString();
        String key = "uploads/tmp" + LocalDate.now() + "/" + uuid + ext;
        
        try {
            try (InputStream inputStream = file.getInputStream()) {
                writeNewFile(key, inputStream);
            }
        } catch (IOException e) {
            log.error("로컬 파일 업로드 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다: " + e.getMessage());
        }

        log.info("Temporary file uploaded successfully - Key: {}, Original: {}, Size: {}",
                key, file.getOriginalFilename(), file.getSize());

        // 미리보기용 URL (상대 경로로 반환, 프론트엔드에서 baseUrl 추가)
        String downloadUrl = "/api/uploads/download?key=" + URLEncoder.encode(key, StandardCharsets.UTF_8);

        // 이미지 여부
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");

        return new FileUploadResponseDto(key, downloadUrl, file.getOriginalFilename(), file.getContentType(), isImage);
    }

    // 파일 저장
    public String moveToPermanent(String key, String destPrefix) {
        if (key == null || key.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 유효하지 않습니다.");
        }
        
        String ext = key.contains(".") ? key.substring(key.lastIndexOf('.')) : "";
        String uuid = UUID.randomUUID().toString();
        String destKey = "uploads/" + normalizeRelativeKey(destPrefix) + "/" + uuid + ext;

        try {
            moveFile(key, destKey);
            log.info("Successfully moved file from {} to {}", key, destKey);
            return destKey;

        } catch (NoSuchFileException e) {
            throw new CustomException(HttpStatus.NOT_FOUND, "영구 저장으로 이동할 임시 파일을 찾을 수 없습니다: " + key);
        } catch (IOException e) {
            log.error("Error moving file from {} to {}: {}", key, destKey, e.getMessage(), e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 이동 중 오류가 발생했습니다.");
        }
    }

    // 파일 다운로드
    public void downloadFile(String key, HttpServletResponse response) throws IOException {
        try {
            String contentType = java.net.URLConnection.guessContentTypeFromName(key);
            response.setContentType(contentType != null ? contentType : "application/octet-stream");
            
            String fileName = key != null && key.contains("/") ? 
                key.substring(key.lastIndexOf('/') + 1) : 
                (key != null ? key : "download");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");

            try (InputStream inputStream = openFileForRead(key)) {
                IOUtils.copy(inputStream, response.getOutputStream());
            }
            response.flushBuffer();
        } catch (NoSuchFileException e) {
            log.error("로컬 파일 다운로드 실패. 파일을 찾을 수 없음. Key: {}", key);
            throw new CustomException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        } catch (IOException e) {
            log.error("로컬 파일 다운로드 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 다운로드 중 오류가 발생했습니다.");
        }
    }

    public String getPublicUrl(String key) {
        // baseUrl이 이미 /uploads로 끝나는 경우 중복 방지
        String normalizedBaseUrl = baseUrl.endsWith("/uploads") ? baseUrl : baseUrl + "/uploads";
        return normalizedBaseUrl + "/" + key;
    }

    /**
     * Static 파일 서빙을 위한 URL 생성
     */
    public String getCdnUrl(String key) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        
        String cleanKey = key.startsWith("/") ? key.substring(1) : key;
        // baseUrl이 이미 /uploads로 끝나는 경우 중복 방지
        String normalizedBaseUrl = baseUrl.endsWith("/uploads") ? baseUrl : baseUrl + "/uploads";
        return normalizedBaseUrl + "/" + cleanKey;
    }

    public String getStaticKeyFromPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return null;
        }
        
        try {
            java.net.URI uri = java.net.URI.create(publicUrl.trim());
            String path = uri.getPath();
            
            if (path != null && path.startsWith("/uploads/")) {
                String key = path.substring("/uploads/".length());
                return java.net.URLDecoder.decode(key, java.nio.charset.StandardCharsets.UTF_8);
            }
            
            return null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // 파일 삭제
    public void deleteFile(String key) {
        try {
            if (deleteFileIfExists(key)) {
                log.info("로컬 파일 삭제 성공. Key: {}", key);
            } else {
                log.warn("삭제하려는 파일이 존재하지 않음. Key: {}", key);
            }
        } catch (IOException e) {
            log.error("로컬 파일 삭제 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다.");
        }
    }

    /**
     * 로컬 파일 시스템에서 파일 존재 여부 확인
     */
    public boolean fileExists(String key) {
        try {
            return fileExistsSecurely(key);
        } catch (NoSuchFileException e) {
            return false;
        } catch (IOException e) {
            log.error("로컬 파일 존재 확인 실패. Key: {}", key, e);
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "파일 존재 확인 중 오류가 발생했습니다.");
        }
    }

    private InputStream openFileForRead(String key) throws IOException {
        List<Path> segments = keySegments(key);
        try (SecureDirectoryStream<Path> secureRoot = openSecureUploadRoot()) {
            SecurePath securePath = openSecureParent(secureRoot, segments, false);
            try (securePath) {
                BasicFileAttributes attrs = readAttributes(securePath.parent(), securePath.fileName());
                if (attrs == null) {
                    throw new NoSuchFileException(key);
                }
                rejectUnsafeFinalFile(attrs);
                SeekableByteChannel channel = securePath.parent().newByteChannel(
                        securePath.fileName(),
                        Set.of(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS));
                return Channels.newInputStream(channel);
            }
        }
    }

    private void writeNewFile(String key, InputStream inputStream) throws IOException {
        List<Path> segments = keySegments(key);
        try (SecureDirectoryStream<Path> secureRoot = openSecureUploadRoot()) {
            SecurePath securePath = openSecureParent(secureRoot, segments, true);
            try (securePath;
                 SeekableByteChannel outputChannel = securePath.parent().newByteChannel(
                         securePath.fileName(),
                         Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS));
                 OutputStream outputStream = Channels.newOutputStream(outputChannel)) {
                IOUtils.copy(inputStream, outputStream);
            }
        }
    }

    private void moveFile(String sourceKey, String destKey) throws IOException {
        List<Path> sourceSegments = keySegments(sourceKey);
        List<Path> destSegments = keySegments(destKey);

        try (SecureDirectoryStream<Path> secureRoot = openSecureUploadRoot()) {
            SecurePath source = openSecureParent(secureRoot, sourceSegments, false);
            try (source; SecureDirectoryStream<Path> secureSecondRoot = openSecureUploadRoot()) {
                SecurePath dest = openSecureParent(secureSecondRoot, destSegments, true);
                try (dest) {
                    BasicFileAttributes sourceAttrs = readAttributes(source.parent(), source.fileName());
                    if (sourceAttrs == null) {
                        throw new NoSuchFileException(sourceKey);
                    }
                    rejectUnsafeFinalFile(sourceAttrs);
                    BasicFileAttributes destAttrs = readAttributes(dest.parent(), dest.fileName());
                    if (destAttrs != null) {
                        if (destAttrs.isDirectory()) {
                            throw new IOException("대상 경로가 디렉터리입니다: " + destKey);
                        }
                        dest.parent().deleteFile(dest.fileName());
                    }
                    source.parent().move(source.fileName(), dest.parent(), dest.fileName());
                }
            }
        }
    }

    private boolean deleteFileIfExists(String key) throws IOException {
        List<Path> segments = keySegments(key);
        try (SecureDirectoryStream<Path> secureRoot = openSecureUploadRoot()) {
            SecurePath securePath = openSecureParent(secureRoot, segments, false);
            try (securePath) {
                BasicFileAttributes attrs = readAttributes(securePath.parent(), securePath.fileName());
                if (attrs == null) {
                    return false;
                }
                rejectUnsafeFinalFile(attrs);
                securePath.parent().deleteFile(securePath.fileName());
                return true;
            }
        }
    }

    private boolean fileExistsSecurely(String key) throws IOException {
        List<Path> segments = keySegments(key);
        try (SecureDirectoryStream<Path> secureRoot = openSecureUploadRoot()) {
            SecurePath securePath = openSecureParent(secureRoot, segments, false);
            try (securePath) {
                BasicFileAttributes attrs = readAttributes(securePath.parent(), securePath.fileName());
                if (attrs == null) {
                    return false;
                }
                rejectUnsafeFinalFile(attrs);
                return true;
            }
        }
    }

    private DirectoryStream<Path> openUploadRoot() throws IOException {
        Path basePath = uploadRootPath();
        Files.createDirectories(basePath);
        return Files.newDirectoryStream(basePath);
    }

    private SecureDirectoryStream<Path> openSecureUploadRoot() throws IOException {
        DirectoryStream<Path> root = openUploadRoot();
        if (root instanceof SecureDirectoryStream<Path> secureRoot) {
            return secureRoot;
        }

        root.close();
        throw new IOException("SecureDirectoryStream is required for local file I/O");
    }

    private SecurePath openSecureParent(
            SecureDirectoryStream<Path> root,
            List<Path> segments,
            boolean createDirectories
    ) throws IOException {
        SecureDirectoryStream<Path> current = root;
        Path currentPath = uploadRootPath();
        boolean closeCurrent = false;

        for (int i = 0; i < segments.size() - 1; i++) {
            Path segment = segments.get(i);
            BasicFileAttributes attrs = readAttributes(current, segment);
            if (attrs == null) {
                if (!createDirectories) {
                    throw new NoSuchFileException(segment.toString());
                }
                try {
                    Files.createDirectory(currentPath.resolve(segment));
                } catch (java.nio.file.FileAlreadyExistsException ignored) {
                    // Created concurrently; validate and open it below through the secure stream.
                }
                attrs = readAttributes(current, segment);
            }
            rejectUnsafeDirectory(attrs);

            SecureDirectoryStream<Path> next = current.newDirectoryStream(segment, LinkOption.NOFOLLOW_LINKS);
            if (closeCurrent) {
                current.close();
            }
            current = next;
            closeCurrent = true;
            currentPath = currentPath.resolve(segment);
        }

        return new SecurePath(current, segments.get(segments.size() - 1), closeCurrent);
    }

    private BasicFileAttributes readAttributes(SecureDirectoryStream<Path> directory, Path name) throws IOException {
        try {
            BasicFileAttributeView view = directory.getFileAttributeView(
                    name,
                    BasicFileAttributeView.class,
                    LinkOption.NOFOLLOW_LINKS);
            return view.readAttributes();
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    private void rejectUnsafeDirectory(BasicFileAttributes attrs) {
        if (attrs.isSymbolicLink() || !attrs.isDirectory()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 허용된 저장소 범위를 벗어났습니다.");
        }
    }

    private void rejectUnsafeFinalFile(BasicFileAttributes attrs) {
        if (attrs.isSymbolicLink() || !attrs.isRegularFile()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 허용된 저장소 범위를 벗어났습니다.");
        }
    }

    private String normalizeRelativeKey(String key) {
        if (key == null || key.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 유효하지 않습니다.");
        }

        String normalizedSeparators = key.replace('\\', '/');
        Path relativePath = Paths.get(normalizedSeparators).normalize();

        if (relativePath.isAbsolute() || relativePath.startsWith("..") || relativePath.toString().equals("..")) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 허용된 저장소 범위를 벗어났습니다.");
        }

        return relativePath.toString().replace(File.separatorChar, '/');
    }

    private List<Path> keySegments(String key) {
        Path relativePath = Paths.get(normalizeRelativeKey(key));
        List<Path> segments = new ArrayList<>();
        for (Path segment : relativePath) {
            segments.add(segment);
        }
        if (segments.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "파일 키가 유효하지 않습니다.");
        }
        return segments;
    }

    private Path uploadRootPath() {
        return Paths.get(uploadBasePath).toAbsolutePath().normalize();
    }

    boolean isSecureDirectoryStreamSupported() {
        try (DirectoryStream<Path> root = openUploadRoot()) {
            return root instanceof SecureDirectoryStream<Path>;
        } catch (IOException e) {
            return false;
        }
    }

    private record SecurePath(SecureDirectoryStream<Path> parent, Path fileName, boolean closeParent) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (closeParent) {
                parent.close();
            }
        }
    }
}
