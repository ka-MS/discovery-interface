package com.itmsg.device42.architecture;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/** 단일 모듈에서도 소유권 경계와 정확한 Java 패키지 간 순환을 회귀 검사한다. */
class DependencyBoundaryTest {
    private static final String ROOT = "com.itmsg.device42.";
    private static final Pattern PACKAGE = Pattern.compile("^package ([^;]+);", Pattern.MULTILINE);
    private static final Pattern IMPORT = Pattern.compile("^import (?:static )?(com\\.itmsg\\.device42\\.[^;]+);", Pattern.MULTILINE);

    @Test
    void lowerLayersHaveNoReverseDependenciesAndPackagesAreAcyclic() throws Exception {
        Map<String, String> classes = new HashMap<>();
        Map<Path, String> sources = new HashMap<>();
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            for (var path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(path);
                sources.put(path, source);
                var declaration = PACKAGE.matcher(source);
                assertThat(declaration.find()).as("package: %s", path).isTrue();
                classes.put(declaration.group(1) + "." + path.getFileName().toString().replace(".java", ""), declaration.group(1));
            }
        }
        Map<String, Set<String>> graph = new HashMap<>();
        for (var entry : sources.entrySet()) {
            var declaration = PACKAGE.matcher(entry.getValue());
            declaration.find();
            String owner = declaration.group(1);
            assertThat(owner).doesNotContain(ROOT + "dto", ROOT + "enums", ROOT + "config");
            var imports = IMPORT.matcher(entry.getValue());
            while (imports.find()) {
                String imported = imports.group(1);
                assertThat(imported).as("명시적인 내부 의존: %s", entry.getKey()).doesNotEndWith(".*");
                String dependency = classes.get(imported);
                if (dependency == null) dependency = classes.get(imported.substring(0, imported.lastIndexOf('.')));
                assertThat(dependency).as("해결 가능한 내부 import %s", imported).isNotNull();
                if (owner.startsWith(ROOT + "runtime")) assertThat(dependency).startsWith(ROOT + "runtime");
                if (owner.startsWith(ROOT + "device42")) assertThat(dependency).startsWith(ROOT + "device42");
                if (owner.startsWith(ROOT + "maximo")) assertThat(dependency).startsWith(ROOT + "maximo");
                if (owner.startsWith(ROOT + "cli")) assertThat(dependency).startsWith(ROOT + "runtime");
                if (!owner.equals(dependency)) graph.computeIfAbsent(owner, ignored -> new HashSet<>()).add(dependency);
            }
        }
        for (String pkg : graph.keySet()) visit(pkg, graph, new ArrayList<>(), new HashSet<>());
    }

    private void visit(String pkg, Map<String, Set<String>> graph, ArrayList<String> stack, Set<String> done) {
        assertThat(stack).as("패키지 순환: %s -> %s", stack, pkg).doesNotContain(pkg);
        if (!done.add(pkg)) return;
        stack.add(pkg);
        for (String dependency : graph.getOrDefault(pkg, Set.of())) visit(dependency, graph, stack, done);
        stack.removeLast();
    }
}
