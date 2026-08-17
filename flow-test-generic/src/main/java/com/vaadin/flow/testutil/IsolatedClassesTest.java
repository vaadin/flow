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
package com.vaadin.flow.testutil;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static java.lang.reflect.Modifier.isAbstract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Checks that every {@link Isolated} marker in the module sits somewhere it has
 * an effect. The platform only applies the marker to classes it discovers tests
 * in, so one that lands on a helper type silently stops isolating anything, and
 * because parallelism is off by default nothing else in the build notices.
 * <p>
 * Subclass this in any module whose test classes use {@code @Isolated}, the way
 * {@link ClassesSerializableTest} is subclassed, so the module's own test
 * output directory gets scanned.
 */
public abstract class IsolatedClassesTest {

    private static final String MARKER = Isolated.class.getName().replace('.',
            '/');

    @Test
    void isolatedMarkersLandOnTestClasses()
            throws IOException, URISyntaxException {
        Path root = Path.of(getClass().getProtectionDomain().getCodeSource()
                .getLocation().toURI());

        List<String> candidates;
        try (Stream<Path> classFiles = Files.walk(root)) {
            candidates = classFiles
                    .filter(path -> path.toString().endsWith(".class"))
                    .filter(this::mentionsIsolated)
                    .map(path -> className(root, path)).sorted().toList();
        }

        Launcher launcher = LauncherFactory.create();
        List<String> unloadable = new ArrayList<>();
        List<String> isolated = new ArrayList<>();
        List<String> withoutTests = new ArrayList<>();

        for (String name : candidates) {
            Class<?> cls;
            try {
                cls = Class.forName(name, false, getClass().getClassLoader());
            } catch (ClassNotFoundException | LinkageError e) {
                // Reported rather than skipped: a class that mentions
                // @Isolated is one this check exists for, so failing to look
                // at it has to be visible
                unloadable.add(name + " (" + e + ")");
                continue;
            }
            if (!cls.isAnnotationPresent(Isolated.class)) {
                // Mentions the annotation without carrying it, for example
                // this check itself
                continue;
            }
            isolated.add(name);
            // @Isolated is @Inherited, so on an abstract class it is there for
            // the subclasses and the base itself is never discovered
            if (!isAbstract(cls.getModifiers())
                    && countTests(launcher, cls) == 0) {
                withoutTests.add(name);
            }
        }

        assertEquals(List.of(), unloadable,
                "Could not load classes that mention @Isolated, so they went "
                        + "unchecked");
        assertFalse(isolated.isEmpty(),
                () -> "Found no @Isolated class under " + root
                        + ", so this check is not looking at anything. Either "
                        + "the module stopped using the marker, in which case "
                        + "drop this test, or the scan is broken");
        assertEquals(List.of(), withoutTests,
                "@Isolated does nothing on a class the test engine finds no "
                        + "test in; move the marker to the test classes");
    }

    /**
     * Filters on the constant pool before loading anything, both to keep the
     * scan cheap and to keep {@code unloadable} down to classes that really are
     * about {@code @Isolated}.
     */
    private boolean mentionsIsolated(Path classFile) {
        try {
            return new String(Files.readAllBytes(classFile),
                    StandardCharsets.ISO_8859_1).contains(MARKER);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private long countTests(Launcher launcher, Class<?> cls) {
        return launcher
                .discover(LauncherDiscoveryRequestBuilder.request()
                        .selectors(selectClass(cls)).build())
                .countTestIdentifiers(TestIdentifier::isTest);
    }

    private String className(Path root, Path classFile) {
        String relative = root.relativize(classFile).toString();
        return relative.substring(0, relative.length() - ".class".length())
                .replace(classFile.getFileSystem().getSeparator(), ".");
    }
}
