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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the module descriptors of the Flow modules keep up with their
 * code: every package is exported, as the modules export all of their packages,
 * and every service in {@code META-INF/services} is declared with
 * {@code provides}, as the module path ignores those files.
 */
class ModuleDescriptorsTest {

    private static final Pattern EXPORTS = Pattern
            .compile("^\\s*exports\\s+([\\w.]+)", Pattern.MULTILINE);

    private static final Pattern PROVIDES = Pattern
            .compile("provides\\s+([\\w.]+)\\s+with", Pattern.MULTILINE);

    @Test
    void moduleDescriptors_exportAllPackagesAndProvideAllServices()
            throws IOException {
        List<Path> descriptors = findDescriptors();
        assertFalse(descriptors.isEmpty(), "No module descriptors found");

        List<String> errors = new ArrayList<>();
        for (Path descriptor : descriptors) {
            Path sources = descriptor.getParent();
            Path module = sources.getParent().getParent().getParent();
            String content = Files.readString(descriptor);

            Set<String> missingExports = packages(sources);
            missingExports.removeAll(matches(EXPORTS, content));
            missingExports.forEach(pkg -> errors.add(module.getFileName()
                    + ": package " + pkg + " is not exported"));

            Set<String> missingServices = services(module);
            missingServices.removeAll(matches(PROVIDES, content));
            missingServices.forEach(service -> errors.add(module.getFileName()
                    + ": service " + service + " is not provided"));
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    private static List<Path> findDescriptors() throws IOException {
        // The Flow modules are direct children of the repository root
        try (Stream<Path> modules = Files.list(Path.of(".."))) {
            return modules
                    .map(module -> module
                            .resolve("src/main/java/module-info.java"))
                    .filter(Files::exists).sorted().toList();
        }
    }

    private static Set<String> packages(Path sources) throws IOException {
        try (Stream<Path> files = Files.walk(sources)) {
            return files
                    .filter(file -> file.toString().endsWith(".java")
                            && !file.endsWith("module-info.java"))
                    .map(file -> sources.relativize(file.getParent()).toString()
                            .replace(file.getFileSystem().getSeparator(), "."))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> services(Path module) throws IOException {
        Path services = module.resolve("src/main/resources/META-INF/services");
        if (!Files.isDirectory(services)) {
            return new TreeSet<>();
        }
        try (Stream<Path> files = Files.list(services)) {
            return files.map(file -> file.getFileName().toString())
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Set<String> matches(Pattern pattern, String content) {
        Set<String> result = new TreeSet<>();
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
