package com.fairing.fairplay.core.service;

import com.fairing.fairplay.common.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileService localFileService;
    private Path uploadRoot;

    @BeforeEach
    void setUp() throws Exception {
        uploadRoot = tempDir.resolve("uploads");
        Files.createDirectories(uploadRoot);

        localFileService = new LocalFileService();
        ReflectionTestUtils.setField(localFileService, "uploadBasePath", uploadRoot.toString());
        ReflectionTestUtils.setField(localFileService, "baseUrl", "https://fair-play.test");
    }

    @Test
    void downloadFileServesFileInsideUploadRoot() throws Exception {
        Path storedFile = uploadRoot.resolve("uploads/events/banner.txt");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "banner");

        MockHttpServletResponse response = new MockHttpServletResponse();

        if (!secureDirectoryStreamSupported()) {
            assertFailClosed(() -> localFileService.downloadFile("uploads/events/banner.txt", response));
            assertThat(response.getContentAsString()).isEmpty();
            return;
        }

        localFileService.downloadFile("uploads/events/banner.txt", response);
        assertThat(response.getContentAsString()).isEqualTo("banner");
        assertThat(response.getHeader("Content-Disposition")).contains("banner.txt");
    }

    @Test
    void uploadTempStoresFileOnlyWhenSecureDirectoryStreamIsAvailable() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "banner.txt", "text/plain", "banner".getBytes());

        if (!secureDirectoryStreamSupported()) {
            assertFailClosed(() -> localFileService.uploadTemp(file));
            try (Stream<Path> files = Files.walk(uploadRoot)) {
                assertThat(files.filter(Files::isRegularFile)).isEmpty();
            }
            return;
        }

        var response = localFileService.uploadTemp(file);

        assertThat(response.getKey()).startsWith("private/tmp/");
        assertThat(response.getKey()).doesNotStartWith("uploads/");
        assertThat(uploadRoot.resolve(response.getKey())).hasContent("banner");
    }

    @Test
    void downloadFileRejectsPathTraversalOutsideUploadRoot() throws Exception {
        Files.writeString(tempDir.resolve("secret.txt"), "outside");

        assertThatThrownBy(() -> localFileService.downloadFile("../secret.txt", new MockHttpServletResponse()))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void downloadFileRejectsFinalSymlinkEscapeOutsideUploadRoot() throws Exception {
        Path outsideFile = tempDir.resolve("secret.txt");
        Files.writeString(outsideFile, "outside");
        Path symlink = uploadRoot.resolve("uploads/secret-link.txt");
        Files.createDirectories(symlink.getParent());
        createSymlinkOrSkip(symlink, outsideFile);

        assertSecurityRejected(() -> localFileService.downloadFile("uploads/secret-link.txt", new MockHttpServletResponse()));
    }

    @Test
    void downloadFileRejectsIntermediateSymlinkEscapeOutsideUploadRoot() throws Exception {
        Path outsideDir = tempDir.resolve("outside");
        Files.createDirectories(outsideDir);
        Files.writeString(outsideDir.resolve("secret.txt"), "outside");
        Path symlinkDir = uploadRoot.resolve("uploads/link-dir");
        Files.createDirectories(symlinkDir.getParent());
        createSymlinkOrSkip(symlinkDir, outsideDir);

        assertSecurityRejected(() -> localFileService.downloadFile("uploads/link-dir/secret.txt", new MockHttpServletResponse()));
    }

    @Test
    void moveToPermanentMovesFileWithinUploadRoot() throws Exception {
        Path sourceFile = uploadRoot.resolve("private/tmp/2026-05-18/source.txt");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "staged");

        if (!secureDirectoryStreamSupported()) {
            assertFailClosed(() -> localFileService.moveToPermanent("private/tmp/2026-05-18/source.txt", "events/banners"));
            assertThat(sourceFile).exists();
            return;
        }

        String destKey = localFileService.moveToPermanent("private/tmp/2026-05-18/source.txt", "events/banners");
        assertThat(destKey).startsWith("uploads/events/banners/");
        assertThat(destKey).endsWith(".txt");
        assertThat(sourceFile).doesNotExist();
        assertThat(uploadRoot.resolve(destKey)).hasContent("staged");
    }

    @Test
    void getCdnUrlRejectsPrivateAndLegacyTempKeys() {
        assertThatThrownBy(() -> localFileService.getCdnUrl("private/tmp/2026-05-18/source.txt"))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> localFileService.getCdnUrl("uploads/tmp2026-05-18/source.txt"))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> localFileService.getCdnUrl("uploads/temp/2026-05-18/source.txt"))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void getCdnUrlAllowsPermanentUploadKey() {
        assertThat(localFileService.getCdnUrl("uploads/events/banner.txt"))
                .isEqualTo("https://fair-play.test/uploads/uploads/events/banner.txt");
    }

    @Test
    void getCdnUrlAllowsPermanentKeysWhoseNamesOnlyStartWithTempWords() {
        assertThat(localFileService.getCdnUrl("uploads/tmp-assets/banner.txt"))
                .isEqualTo("https://fair-play.test/uploads/uploads/tmp-assets/banner.txt");
        assertThat(localFileService.getCdnUrl("uploads/temporary/banner.txt"))
                .isEqualTo("https://fair-play.test/uploads/uploads/temporary/banner.txt");
    }

    @Test
    void moveToPermanentRejectsSourceOutsideUploadRoot() throws Exception {
        Files.writeString(tempDir.resolve("staged.txt"), "outside");

        assertThatThrownBy(() -> localFileService.moveToPermanent("../staged.txt", "events"))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void moveToPermanentRejectsDestinationIntermediateSymlinkEscapeOutsideUploadRoot() throws Exception {
        Path sourceFile = uploadRoot.resolve("private/tmp/2026-05-18/source.txt");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "staged");
        Path outsideDir = tempDir.resolve("outside-dest");
        Files.createDirectories(outsideDir);
        Path symlinkDir = uploadRoot.resolve("uploads/events");
        Files.createDirectories(symlinkDir.getParent());
        createSymlinkOrSkip(symlinkDir, outsideDir);

        assertSecurityRejected(() -> localFileService.moveToPermanent("private/tmp/2026-05-18/source.txt", "events/banners"));

        assertThat(sourceFile).exists();
        try (Stream<Path> outsideFiles = Files.list(outsideDir)) {
            assertThat(outsideFiles).isEmpty();
        }
    }

    @Test
    void uploadTempRejectsIntermediateSymlinkEscapeOutsideUploadRoot() throws Exception {
        Path outsideDir = tempDir.resolve("outside-upload");
        Files.createDirectories(outsideDir);
        Path symlinkDir = uploadRoot.resolve("private");
        createSymlinkOrSkip(symlinkDir, outsideDir);
        MockMultipartFile file = new MockMultipartFile("file", "banner.txt", "text/plain", "banner".getBytes());

        assertSecurityRejected(() -> localFileService.uploadTemp(file));

        try (Stream<Path> outsideFiles = Files.list(outsideDir)) {
            assertThat(outsideFiles).isEmpty();
        }
    }

    @Test
    void deleteFileDeletesOnlyWhenSecureDirectoryStreamIsAvailable() throws Exception {
        Path storedFile = uploadRoot.resolve("uploads/events/delete-me.txt");
        Files.createDirectories(storedFile.getParent());
        Files.writeString(storedFile, "delete");

        if (!secureDirectoryStreamSupported()) {
            assertFailClosed(() -> localFileService.deleteFile("uploads/events/delete-me.txt"));
            assertThat(storedFile).exists();
            return;
        }

        localFileService.deleteFile("uploads/events/delete-me.txt");

        assertThat(storedFile).doesNotExist();
    }

    @Test
    void deleteFileRejectsPathTraversalOutsideUploadRoot() throws Exception {
        Files.writeString(tempDir.resolve("delete-me.txt"), "outside");

        assertThatThrownBy(() -> localFileService.deleteFile("../delete-me.txt"))
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(tempDir.resolve("delete-me.txt")).exists();
    }

    @Test
    void deleteFileRejectsIntermediateSymlinkEscapeOutsideUploadRoot() throws Exception {
        Path outsideDir = tempDir.resolve("outside-delete");
        Files.createDirectories(outsideDir);
        Path outsideFile = outsideDir.resolve("delete-me.txt");
        Files.writeString(outsideFile, "outside");
        Path symlinkDir = uploadRoot.resolve("uploads/link-delete");
        Files.createDirectories(symlinkDir.getParent());
        createSymlinkOrSkip(symlinkDir, outsideDir);

        assertSecurityRejected(() -> localFileService.deleteFile("uploads/link-delete/delete-me.txt"));

        assertThat(outsideFile).exists();
    }

    @Test
    void fileExistsReturnsFalseOnlyForMissingFile() {
        if (!secureDirectoryStreamSupported()) {
            assertFailClosed(() -> localFileService.fileExists("uploads/events/missing.txt"));
            return;
        }

        assertThat(localFileService.fileExists("uploads/events/missing.txt")).isFalse();
    }

    @Test
    void fileExistsRejectsFinalSymlinkInsteadOfMaskingAsMissing() throws Exception {
        Path outsideFile = tempDir.resolve("secret-exists.txt");
        Files.writeString(outsideFile, "outside");
        Path symlink = uploadRoot.resolve("uploads/secret-exists-link.txt");
        Files.createDirectories(symlink.getParent());
        createSymlinkOrSkip(symlink, outsideFile);

        assertSecurityRejected(() -> localFileService.fileExists("uploads/secret-exists-link.txt"));
    }

    @Test
    void secureDirectoryStreamSupportMatchesRuntimeFileSystemProvider() throws Exception {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(uploadRoot)) {
            assertThat(localFileService.isSecureDirectoryStreamSupported())
                    .isEqualTo(directoryStream instanceof SecureDirectoryStream<Path>);
        }
    }

    private void createSymlinkOrSkip(Path link, Path target) {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | java.io.IOException | SecurityException e) {
            assumeTrue(false, "symbolic links are not available in this test environment: " + e.getMessage());
        }
    }

    private boolean secureDirectoryStreamSupported() {
        return localFileService.isSecureDirectoryStreamSupported();
    }

    private void assertSecurityRejected(ThrowingOperation operation) {
        HttpStatus expectedStatus = secureDirectoryStreamSupported()
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.INTERNAL_SERVER_ERROR;

        assertThatThrownBy(operation::run)
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(expectedStatus));
    }

    private void assertFailClosed(ThrowingOperation operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOf(CustomException.class)
                .satisfies(error -> assertThat(((CustomException) error).getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR));
    }

    @FunctionalInterface
    private interface ThrowingOperation {
        void run() throws Exception;
    }
}
