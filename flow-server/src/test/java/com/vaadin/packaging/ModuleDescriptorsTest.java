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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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
 * every provider in {@code META-INF/services} is declared with
 * {@code provides}, as the module path ignores those files, and every service
 * type loaded with {@code ServiceLoader.load(Type.class, ...)} is declared with
 * {@code uses}, as loading an undeclared service fails on the module path.
 */
class ModuleDescriptorsTest {

    private static final Pattern EXPORTS = Pattern
            .compile("^\\s*exports\\s+([\\w.]+)", Pattern.MULTILINE);

    private static final Pattern PROVIDES = Pattern
            .compile("provides\\s+([\\w.]+)\\s+with\\s+([\\w.,\\s]+);");

    private static final Pattern USES = Pattern
            .compile("^\\s*uses\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    private static final Pattern SERVICE_LOADER_LOAD = Pattern
            .compile("ServiceLoader\\s*\\.\\s*load\\(\\s*([\\w.]+)\\.class");

    private static final Pattern PACKAGE = Pattern
            .compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

    /**
     * Directories that hold no published Flow module: build output, and the
     * test applications, which are modules of their own and export nothing.
     */
    private static final Set<String> SKIPPED_DIRECTORIES = Set.of("target",
            "node_modules", "flow-tests");

    @Test
    void moduleDescriptors_exportAllPackagesProvideAllServicesAndUseLoadedServices()
            throws IOException {
        List<Path> descriptors = findDescriptors();
        assertFalse(descriptors.isEmpty(), "No module descriptors found");

        List<String> errors = new ArrayList<>();
        for (Path descriptor : descriptors) {
            Path sources = descriptor.getParent();
            Path module = sources.getParent().getParent().getParent();
            String name = module.normalize().getFileName().toString();
            String content = Files.readString(descriptor);

            Set<String> missingExports = packages(sources);
            missingExports.removeAll(matches(EXPORTS, content));
            missingExports.forEach(pkg -> errors
                    .add(name + ": package " + pkg + " is not exported"));

            Map<String, Set<String>> provided = provides(content);
            services(module).forEach((service, providers) -> {
                Set<String> missing = new TreeSet<>(providers);
                missing.removeAll(provided.getOrDefault(service, Set.of()));
                missing.forEach(provider -> errors.add(name + ": provider "
                        + provider + " of " + service + " is not provided"));
            });

            Set<String> missingUses = loadedServices(sources);
            missingUses.removeAll(matches(USES, content));
            missingUses.forEach(service -> errors.add(name + ": service "
                    + service + " is loaded but not declared with uses"));
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    private static List<Path> findDescriptors() throws IOException {
        List<Path> descriptors = new ArrayList<>();
        Files.walkFileTree(Path.of(".."), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                    BasicFileAttributes attrs) {
                Path fileName = dir.getFileName();
                if (fileName == null) {
                    return FileVisitResult.CONTINUE;
                }
                String name = fileName.toString();
                if (name.equals("src")) {
                    Path descriptor = dir.resolve("main/java/module-info.java");
                    if (Files.exists(descriptor)) {
                        descriptors.add(descriptor);
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
                if ((name.startsWith(".") && !name.equals(".."))
                        || SKIPPED_DIRECTORIES.contains(name)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        descriptors.sort(null);
        return descriptors;
    }

    private static Set<String> packages(Path sources) throws IOException {
        try (Stream<Path> files = javaFiles(sources)) {
            return files
                    .map(file -> sources.relativize(file.getParent()).toString()
                            .replace(file.getFileSystem().getSeparator(), "."))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    private static Map<String, Set<String>> services(Path module)
            throws IOException {
        Path services = module.resolve("src/main/resources/META-INF/services");
        Map<String, Set<String>> result = new TreeMap<>();
        if (!Files.isDirectory(services)) {
            return result;
        }
        try (Stream<Path> files = Files.list(services)) {
            for (Path file : files.toList()) {
                Set<String> providers = Files.readAllLines(file).stream()
                        .map(line -> line.replaceFirst("#.*", "").trim())
                        .filter(line -> !line.isEmpty())
                        .collect(Collectors.toCollection(TreeSet::new));
                result.put(file.getFileName().toString(), providers);
            }
        }
        return result;
    }

    private static Map<String, Set<String>> provides(String content) {
        Map<String, Set<String>> result = new TreeMap<>();
        Matcher matcher = PROVIDES.matcher(content);
        while (matcher.find()) {
            result.computeIfAbsent(matcher.group(1), key -> new TreeSet<>())
                    .addAll(Arrays.stream(matcher.group(2).split(","))
                            .map(String::trim).toList());
        }
        return result;
    }

    /**
     * Finds the service types loaded with a class literal. A service type
     * passed in a variable, as by {@code Lookup}, is registered with
     * {@code Module.addUses} where it is loaded instead.
     */
    private static Set<String> loadedServices(Path sources) throws IOException {
        Set<String> result = new TreeSet<>();
        try (Stream<Path> files = javaFiles(sources)) {
            for (Path file : files.toList()) {
                String content = Files.readString(file);
                for (String type : matches(SERVICE_LOADER_LOAD, content)) {
                    result.add(qualify(type, content));
                }
            }
        }
        return result;
    }

    private static String qualify(String type, String content) {
        if (type.contains(".")) {
            return type;
        }
        Matcher imported = Pattern
                .compile("^\\s*import\\s+([\\w.]+\\." + type + ")\\s*;",
                        Pattern.MULTILINE)
                .matcher(content);
        if (imported.find()) {
            return imported.group(1);
        }
        Matcher pkg = PACKAGE.matcher(content);
        return pkg.find() ? pkg.group(1) + "." + type : type;
    }

    private static Stream<Path> javaFiles(Path sources) throws IOException {
        return Files.walk(sources)
                .filter(file -> file.toString().endsWith(".java")
                        && !file.endsWith("module-info.java"));
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
