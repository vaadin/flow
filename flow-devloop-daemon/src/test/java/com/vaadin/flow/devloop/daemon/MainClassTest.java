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
package com.vaadin.flow.devloop.daemon;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The application class is discovered rather than configured: the one thing a
 * developer should never have to tell the dev loop is the name of their own
 * application class.
 */
class MainClassTest {

    /**
     * A stand-in for Spring's annotation, so the fixtures can be annotated with
     * it without this dependency-free module gaining a Spring dependency. Only
     * the descriptor in the constant pool is what discovery reads, and that is
     * the same either way.
     */
    private static final String SPRING_BOOT_APPLICATION_STUB = """
            package org.springframework.boot.autoconfigure;
            public @interface SpringBootApplication { }
            """;

    @TempDir
    private Path app;

    private final Launch.Log log = text -> {
    };

    @AfterEach
    void clearOverride() {
        System.clearProperty("vaadin.dev.mainClass");
    }

    @Test
    void override_winsOverEverything() throws IOException {
        System.setProperty("vaadin.dev.mainClass", "com.example.Chosen");
        compile("""
                package app;
                public class Other { public static void main(String[] a) { } }
                """);

        assertEquals(Optional.of("com.example.Chosen"),
                MainClass.discover(module(), log));
    }

    @Test
    void springBootApplication_winsOverAPlainMainMethod() throws IOException {
        compile(SPRING_BOOT_APPLICATION_STUB, """
                package app;
                public class Helper { public static void main(String[] a) { } }
                """, """
                package app;
                @org.springframework.boot.autoconfigure.SpringBootApplication
                public class Application {
                    public static void main(String[] a) { }
                }
                """);

        assertEquals(Optional.of("app.Application"),
                MainClass.discover(module(), log));
    }

    @Test
    void mainMethod_isTheFallback() throws IOException {
        compile("""
                package app;
                public class Plain { }
                """,
                """
                        package app;
                        public class Launcher { public static void main(String[] a) { } }
                        """);

        assertEquals(Optional.of("app.Launcher"),
                MainClass.discover(module(), log));
    }

    @Test
    void instanceMainMethod_isNotAnEntryPoint() throws IOException {
        compile("""
                package app;
                public class NotIt { public void main(String[] a) { } }
                """);

        assertTrue(MainClass.discover(module(), log).isEmpty());
    }

    @Test
    void packagedJar_startClassBeatsTheLauncherMainClass() throws IOException {
        // In a Spring Boot fat jar Main-Class is the launcher, and handing that
        // to a -cp launch would start nothing.
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("Main-Class",
                "org.springframework.boot.loader.launch.JarLauncher");
        attributes.putValue("Start-Class", "com.example.Application");
        jar(manifest);

        assertEquals(Optional.of("com.example.Application"),
                MainClass.discover(module(), log));
    }

    @Test
    void nothingToLaunch_isEmptyRatherThanAGuess() {
        assertTrue(MainClass.discover(module(), log).isEmpty());
    }

    private Reactor.Module module() {
        return Reactor.Module.of(app, "app");
    }

    private void jar(Manifest manifest) throws IOException {
        Path target = app.resolve("target");
        Files.createDirectories(target);
        try (OutputStream out = Files
                .newOutputStream(target.resolve("app-1.0.jar"));
                JarOutputStream jar = new JarOutputStream(out, manifest)) {
            // The manifest is the whole point; the jar needs no entries.
        }
    }

    /** Compiles fixture sources into the module's own output directory. */
    private void compile(String... sources) throws IOException {
        Path sourceRoot = app.resolve("src").resolve("main").resolve("java");
        List<String> arguments = new ArrayList<>(List.of("-d",
                app.resolve("target").resolve("classes").toString(),
                "-nowarn"));
        for (String source : sources) {
            Path file = sourceRoot.resolve(packageOf(source).replace('.', '/'))
                    .resolve(typeNameOf(source) + ".java");
            Files.createDirectories(file.getParent());
            Files.writeString(file, source);
            arguments.add(file.toString());
        }
        Files.createDirectories(app.resolve("target").resolve("classes"));
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertEquals(0,
                compiler.run(null, null, null,
                        arguments.toArray(new String[0])),
                "test fixture did not compile");
    }

    private static String packageOf(String source) {
        return source.lines().filter(line -> line.startsWith("package "))
                .findFirst().orElseThrow().substring("package ".length())
                .replace(";", "").trim();
    }

    private static String typeNameOf(String source) {
        for (String keyword : List.of("@interface ", "class ", "interface ")) {
            int at = source.indexOf(keyword);
            if (at >= 0) {
                return source.substring(at + keyword.length()).trim()
                        .split("[^A-Za-z0-9_$]")[0];
            }
        }
        throw new IllegalArgumentException("no type declared in " + source);
    }
}
