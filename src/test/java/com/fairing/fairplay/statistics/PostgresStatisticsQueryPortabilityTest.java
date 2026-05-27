package com.fairing.fairplay.statistics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresStatisticsQueryPortabilityTest {

    @Test
    void statisticsQueriesDoNotUseDialectSpecificDateTruncationTemplates() throws IOException {
        Path sourceRoot = Path.of(System.getProperty("user.dir"), "src/main/java/com/fairing/fairplay");
        List<String> bannedPatterns = List.of(
                "DATE({0})",
                "DAY({0})",
                "CEILING(DAY",
                "dateTemplate(LocalDate.class, \"DATE",
                "java.sql.Date");

        List<String> violations;
        try (var paths = Files.walk(sourceRoot)) {
            violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("/statistics/")
                            || path.toString().contains("/temp/repository/reservation/")
                            || path.toString().contains("/temp/repository/sales/"))
                    .flatMap(path -> bannedPatterns.stream()
                            .filter(pattern -> contains(path, pattern))
                            .map(pattern -> sourceRoot.relativize(path) + " contains " + pattern))
                    .toList();
        }

        assertThat(violations).isEmpty();
    }

    private static boolean contains(Path path, String pattern) {
        try {
            return Files.readString(path).contains(pattern);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }
}
