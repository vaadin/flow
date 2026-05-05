/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.packaging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Verifies that shared signal classes are not referenced from public API code
 * outside the signals package itself. Shared signals are an internal
 * implementation detail; public API should use the base {@code Signal}
 * interface or local signal classes instead.
 */
class NoSharedSignalsInApiTest {

    private static final Set<String> IGNORED_MODULES = Set.of("flow-tests");

    private static final Pattern SHARED_IMPORT_PATTERN = Pattern
            .compile("import\\s+com\\.vaadin\\.flow\\.signals\\.shared\\.");

    @Test
    void noSharedSignalReferencesOutsideSignalsPackage() throws IOException {
        Collection<File> modules = findModules();
        File repoRoot = new File("..").getCanonicalFile();

        List<String> violations = new ArrayList<>();

        for (File module : modules) {
            File srcDir = new File(module, "src/main/java");
            if (!srcDir.exists()) {
                continue;
            }

            try (Stream<Path> paths = Files.walk(srcDir.toPath())) {
                paths.filter(p -> p.toString().endsWith(".java"))
                        .forEach(javaFile -> {
                            Path relative = srcDir.toPath()
                                    .relativize(javaFile);
                            String relativeStr = relative.toString()
                                    .replace('\\', '/');

                            // Skip files inside the signals package
                            if (relativeStr
                                    .startsWith("com/vaadin/flow/signals/")) {
                                return;
                            }

                            String moduleRelativePath = repoRoot.toPath()
                                    .relativize(javaFile.toAbsolutePath())
                                    .toString().replace('\\', '/');

                            List<String> fileViolations = findViolationsInFile(
                                    javaFile, moduleRelativePath);
                            violations.addAll(fileViolations);
                        });
            }
        }

        if (!violations.isEmpty()) {
            StringBuilder errors = new StringBuilder();
            errors.append(
                    "Shared signal classes must not be referenced from public API code.\n");
            errors.append(
                    "Use Signal, ValueSignal, ListSignal, etc. from the local signals package instead.\n\n");
            errors.append("Violations found:\n");
            violations.forEach(v -> errors.append("  ").append(v).append("\n"));
            fail(errors.toString());
        }
    }

    private List<String> findViolationsInFile(Path javaFile,
            String displayPath) {
        List<String> violations = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(javaFile);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (SHARED_IMPORT_PATTERN.matcher(line).find()) {
                    violations.add(
                            displayPath + ":" + (i + 1) + ": " + line.trim());
                }
            }
        } catch (IOException e) {
            violations.add(
                    displayPath + ": error reading file: " + e.getMessage());
        }
        return violations;
    }

    private Collection<File> findModules() throws IOException {
        Collection<File> modules = new ArrayList<>();
        collectModules(new File(".."), modules);
        return modules;
    }

    private void collectModules(File dir, Collection<File> modules) {
        if (IGNORED_MODULES.contains(dir.getName())) {
            return;
        }

        if (new File(dir, "pom.xml").exists()) {
            modules.add(dir);

            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isDirectory()) {
                        collectModules(child, modules);
                    }
                }
            }
        }
    }
}
