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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Which class the application JVM is launched with.
 * <p>
 * Discovered rather than configured, because the one thing a developer should
 * never have to tell the dev loop is the name of their own application class.
 * The answers are tried in order of how much they are worth trusting:
 * <ol>
 * <li>{@code -Dvaadin.dev.mainClass}, for the project the heuristics get
 * wrong.</li>
 * <li>The manifest of a jar the build already produced: Spring Boot's
 * {@code Start-Class}, else a plain {@code Main-Class}. This is the build's own
 * answer, so it beats anything read out of class files.</li>
 * <li>A class in the application module's output annotated
 * {@code @SpringBootApplication}.</li>
 * <li>A class in that output with a {@code public static void main}.</li>
 * </ol>
 * The first three are {@link #namedByBuild}: answers the build states, rather
 * than infers. Only they are strong enough for {@link AppRuntime} to decide
 * this is an application with an entry point at all. Class files are read with
 * a minimal constant-pool walk rather than loaded: the daemon has neither the
 * application's classpath nor any business putting it on its own.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class MainClass {

    /** {@code @SpringBootApplication}, as it appears in a class file. */
    private static final String SPRING_BOOT_APPLICATION = "Lorg/springframework/boot/autoconfigure/SpringBootApplication;";

    private MainClass() {
    }

    /**
     * The class to launch for an application module, or empty when nothing
     * looks like an entry point.
     *
     * @param appModule
     *            the application module
     * @param log
     *            where to report which answer was used
     * @return the binary name of the class to launch
     */
    static Optional<String> discover(Reactor.Module appModule, Launch.Log log) {
        Optional<String> named = namedByBuild(appModule);
        if (named.isPresent()) {
            log.line("main class " + named.get() + " (named by the build)");
            return named;
        }
        Optional<String> withMain = firstMatching(appModule,
                classFilesOf(appModule.classesDir()), ClassFile::hasMainMethod);
        withMain.ifPresent(name -> log
                .line("main class " + name + " (public static void main)"));
        return withMain;
    }

    /**
     * The entry point the <em>build</em> names, as opposed to one merely found
     * by looking for a main method.
     * <p>
     * Separated out because it is the one unambiguous answer, and
     * {@link AppRuntime} has to distinguish the two. A WAR project may well
     * carry some unrelated {@code public static void main} - a code generator,
     * a fixture loader - and taking that for the application would launch the
     * wrong thing and report the wrong reason when it exited. A manifest
     * {@code Start-Class} or an {@code @SpringBootApplication} is a statement
     * of intent; a stray main method is not.
     *
     * @param appModule
     *            the application module
     * @return the binary name the build names, or empty when it names none
     */
    static Optional<String> namedByBuild(Reactor.Module appModule) {
        String configured = System.getProperty("vaadin.dev.mainClass");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(configured.trim());
        }
        Optional<String> fromManifest = fromPackagedJar(appModule);
        if (fromManifest.isPresent()) {
            return fromManifest;
        }
        return firstMatching(appModule, classFilesOf(appModule.classesDir()),
                file -> file.strings().contains(SPRING_BOOT_APPLICATION));
    }

    /**
     * The build's own answer, when there is a packaged jar to read it from.
     * {@code Start-Class} first: in a Spring Boot fat jar {@code Main-Class} is
     * the launcher, and handing that to a {@code -cp} launch would start
     * nothing.
     * <p>
     * An answer naming a class the compiled output no longer has is left out:
     * the jar is only as current as the last {@code mvn package}, and a class
     * moved to another package since would otherwise go on being launched after
     * it is gone. An output with no classes at all is a module not yet
     * compiled, which the jar still speaks for.
     */
    private static Optional<String> fromPackagedJar(Reactor.Module appModule) {
        Path target = appModule.dir().resolve("target");
        if (!Files.isDirectory(target)) {
            return Optional.empty();
        }
        try (Stream<Path> jars = Files.list(target)) {
            return jars.filter(path -> path.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(MainClass::mainClassIn).filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(name -> !isStale(appModule, name)).findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static boolean isStale(Reactor.Module appModule, String name) {
        return !Files.isRegularFile(appModule.classFileOf(name))
                && !classFilesOf(appModule.classesDir()).isEmpty();
    }

    private static Optional<String> mainClassIn(Path jar) {
        try (JarFile file = new JarFile(jar.toFile())) {
            Manifest manifest = file.getManifest();
            if (manifest == null) {
                return Optional.empty();
            }
            for (String attribute : List.of("Start-Class", "Main-Class")) {
                String value = manifest.getMainAttributes().getValue(attribute);
                if (value != null && !value.isBlank() && !value
                        .startsWith("org.springframework.boot.loader")) {
                    return Optional.of(value.trim());
                }
            }
        } catch (IOException | RuntimeException e) {
            // An unreadable or non-jar file is simply not the answer.
        }
        return Optional.empty();
    }

    /**
     * Class files under an output directory, shallowest first so an application
     * class in the root package of the project wins over a helper buried
     * deeper.
     */
    private static List<Path> classFilesOf(Path classesDir) {
        if (!Files.isDirectory(classesDir)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(classesDir)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".class"))
                    // Nested and synthetic classes are never entry points.
                    .filter(path -> path.getFileName() != null
                            && !path.getFileName().toString().contains("$"))
                    .sorted(Comparator.comparingInt(Path::getNameCount)
                            .thenComparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Optional<String> firstMatching(Reactor.Module module,
            List<Path> classFiles,
            java.util.function.Predicate<ClassFile> test) {
        for (Path file : classFiles) {
            Optional<ClassFile> parsed = ClassFile.read(file);
            if (parsed.isPresent() && test.test(parsed.get())) {
                return Optional.of(module.binaryNameOf(file));
            }
        }
        return Optional.empty();
    }
}
