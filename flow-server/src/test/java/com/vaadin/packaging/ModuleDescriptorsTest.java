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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
            .compile("provides\\s+([\\w.]+)\\s+with([^;]+);");

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

    private static final Pattern COMMENT = Pattern
            .compile("//[^\\n]*|/\\*.*?\\*/", Pattern.DOTALL);

    @Test
    void moduleDescriptors_exportAllPackagesProvideAllServicesAndUseLoadedServices()
            throws IOException {
        List<Path> descriptors = findDescriptors();
        Set<String> names = descriptors.stream()
                .map(ModuleDescriptorsTest::moduleName)
                .collect(Collectors.toCollection(TreeSet::new));
        assertTrue(
                names.containsAll(Set.of("flow-data", "flow-dnd",
                        "flow-html-components", "flow-lit-template",
                        "flow-push", "flow-react", "flow-server")),
                "Module descriptors not found: " + names);
        assertFalse(names.contains("test-jpms"),
                "Test applications are not checked");

        List<String> errors = new ArrayList<>();
        for (Path descriptor : descriptors) {
            Path sources = descriptor.getParent();
            errors.addAll(check(moduleName(descriptor),
                    Files.readString(descriptor), packages(sources),
                    services(moduleOf(descriptor)), loadedServices(sources)));
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }

    @Test
    void flowServer_servicesAndLoadedServicesFound() throws IOException {
        Path module = Path.of(".");
        assertEquals(Set.of("com.vaadin.experimental.FeatureFlagProvider",
                "com.vaadin.flow.di.Instantiator",
                "com.vaadin.flow.server.VaadinServiceInitListener",
                "com.vaadin.flow.server.communication.PushConnectionFactory",
                "com.vaadin.flow.server.startup.NavigationTargetFilter"),
                loadedServices(module.resolve("src/main/java")));
        Map<String, Set<String>> services = services(module);
        assertEquals(
                Set.of("com.vaadin.experimental.FeatureFlagProvider",
                        "jakarta.servlet.ServletContainerInitializer"),
                services.keySet());
        assertTrue(services.get("com.vaadin.experimental.FeatureFlagProvider")
                .contains("com.vaadin.experimental.CoreFeatureFlagProvider"));
    }

    @Test
    void check_reportsMissingExportProviderAndUses() {
        String descriptor = """
                module a {
                    exports a.one;
                    provides a.Service with a.First, // a comment
                            /* another */ a.Second;
                    uses a.Used;
                }
                """;
        assertEquals(List.of(),
                check("a", descriptor, Set.of("a.one"),
                        Map.of("a.Service", Set.of("a.First", "a.Second")),
                        Set.of("a.Used")));
        assertEquals(List.of("a: package a.two is not exported",
                "a: provider a.Other of a.OtherService is not provided",
                "a: provider a.Third of a.Service is not provided",
                "a: service a.Loaded is loaded but not declared with uses"),
                check("a", descriptor, Set.of("a.one", "a.two"),
                        new TreeMap<>(Map.of("a.Service",
                                Set.of("a.First", "a.Second", "a.Third"),
                                "a.OtherService", Set.of("a.Other"))),
                        Set.of("a.Used", "a.Loaded")));
    }

    @Test
    void loadedServiceTypes_qualifiedThroughImportsOrPackage() {
        String source = """
                package a.b;

                import c.d.Imported;

                class Loader {
                    void load() {
                        ServiceLoader.load(Imported.class);
                        ServiceLoader
                                .load(
                                        Local.class, null);
                        ServiceLoader.load(Imported.Nested.class);
                        ServiceLoader.load(e.f.Qualified.class);
                    }
                }
                """;
        assertEquals(Set.of("c.d.Imported", "a.b.Local", "c.d.Imported.Nested",
                "e.f.Qualified"), loadedServices(source));
    }

    /**
     * Lists what the module descriptor misses, given what its module contains.
     */
    private static List<String> check(String name, String descriptor,
            Set<String> packages, Map<String, Set<String>> services,
            Set<String> loadedServices) {
        String content = COMMENT.matcher(descriptor).replaceAll(" ");
        List<String> errors = new ArrayList<>();

        Set<String> missingExports = new TreeSet<>(packages);
        missingExports.removeAll(matches(EXPORTS, content));
        missingExports.forEach(pkg -> errors
                .add(name + ": package " + pkg + " is not exported"));

        Map<String, Set<String>> provided = provides(content);
        services.forEach((service, providers) -> {
            Set<String> missing = new TreeSet<>(providers);
            missing.removeAll(provided.getOrDefault(service, Set.of()));
            missing.forEach(provider -> errors.add(name + ": provider "
                    + provider + " of " + service + " is not provided"));
        });

        Set<String> missingUses = new TreeSet<>(loadedServices);
        missingUses.removeAll(matches(USES, content));
        missingUses.forEach(service -> errors.add(name + ": service " + service
                + " is loaded but not declared with uses"));
        return errors;
    }

    private static Path moduleOf(Path descriptor) {
        return descriptor.getParent().getParent().getParent().getParent();
    }

    private static String moduleName(Path descriptor) {
        return moduleOf(descriptor).toAbsolutePath().normalize().getFileName()
                .toString();
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

    private static Set<String> loadedServices(Path sources) throws IOException {
        Set<String> result = new TreeSet<>();
        try (Stream<Path> files = javaFiles(sources)) {
            for (Path file : files.toList()) {
                result.addAll(loadedServices(Files.readString(file)));
            }
        }
        return result;
    }

    /**
     * Finds the service types a source file loads with a class literal. A
     * service type passed in a variable, as by {@code Lookup}, is registered
     * with {@code Module.addUses} where it is loaded instead.
     */
    private static Set<String> loadedServices(String source) {
        Set<String> result = new TreeSet<>();
        for (String type : matches(SERVICE_LOADER_LOAD, source)) {
            result.add(qualify(type, source));
        }
        return result;
    }

    private static String qualify(String type, String source) {
        // A name that starts with a package is already qualified
        if (Character.isLowerCase(type.charAt(0))) {
            return type;
        }
        int dot = type.indexOf('.');
        String outer = dot < 0 ? type : type.substring(0, dot);
        String nested = dot < 0 ? "" : type.substring(dot);
        Matcher imported = Pattern
                .compile("^\\s*import\\s+([\\w.]+\\." + outer + ")\\s*;",
                        Pattern.MULTILINE)
                .matcher(source);
        if (imported.find()) {
            return imported.group(1) + nested;
        }
        Matcher pkg = PACKAGE.matcher(source);
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
