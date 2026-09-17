package com.itmsg.device42.architecture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MappingDocumentationTest {
    @Test
    void everyFlowQueryAndWriterHasAValidMappingReference() throws Exception {
        Set<Path> referenced = new HashSet<>();
        var javaLink = Pattern.compile("\\]\\(([^)]+\\.java)\\)");
        try (var docs = Files.walk(Path.of("docs/data-analysis/data-mapping"))) {
            for (var doc : docs.filter(p -> p.toString().endsWith(".md")).toList()) {
                var matcher = javaLink.matcher(Files.readString(doc));
                while (matcher.find()) {
                    var source = doc.getParent().resolve(matcher.group(1)).toAbsolutePath().normalize();
                    assertThat(source).as("%s -> %s", doc, matcher.group(1)).exists();
                    referenced.add(source);
                }
            }
        }
        try (var sources = Files.walk(Path.of("src/main/java/com/itmsg/device42"))) {
            for (var source : sources.filter(p -> p.getFileName().toString().matches(".*(Import|Query|Writer)\\.java")).toList()) {
                assertThat(referenced).as("매핑 문서에서 참조하는 구현 %s", source)
                        .contains(source.toAbsolutePath().normalize());
            }
        }
    }
}
