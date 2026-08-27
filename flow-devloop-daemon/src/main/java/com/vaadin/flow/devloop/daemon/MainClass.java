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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
 * Class files are read with a minimal constant-pool walk rather than loaded:
 * the daemon has neither the application's classpath nor any business putting
 * it on its own.
 */
final class MainClass {

    /** {@code @SpringBootApplication}, as it appears in a class file. */
    private static final String SPRING_BOOT_APPLICATION = "Lorg/springframework/boot/autoconfigure/SpringBootApplication;";

    private static final String MAIN_DESCRIPTOR = "([Ljava/lang/String;)V";

    private static final int ACC_PUBLIC = 0x0001;
    private static final int ACC_STATIC = 0x0008;

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
        String configured = System.getProperty("vaadin.dev.mainClass");
        if (configured != null && !configured.isBlank()) {
            return Optional.of(configured.trim());
        }
        Optional<String> fromManifest = fromPackagedJar(appModule.dir());
        if (fromManifest.isPresent()) {
            log.line("main class " + fromManifest.get()
                    + " (from the packaged jar's manifest)");
            return fromManifest;
        }
        List<Path> classFiles = classFilesOf(appModule.classesDir());
        Optional<String> springBoot = firstMatching(appModule, classFiles,
                ClassFile::isSpringBootApplication);
        if (springBoot.isPresent()) {
            log.line("main class " + springBoot.get()
                    + " (@SpringBootApplication)");
            return springBoot;
        }
        Optional<String> withMain = firstMatching(appModule, classFiles,
                ClassFile::hasMainMethod);
        withMain.ifPresent(name -> log
                .line("main class " + name + " (public static void main)"));
        return withMain;
    }

    /**
     * The build's own answer, when there is a packaged jar to read it from.
     * {@code Start-Class} first: in a Spring Boot fat jar {@code Main-Class} is
     * the launcher, and handing that to a {@code -cp} launch would start
     * nothing.
     */
    private static Optional<String> fromPackagedJar(Path appModule) {
        Path target = appModule.resolve("target");
        if (!Files.isDirectory(target)) {
            return Optional.empty();
        }
        try (Stream<Path> jars = Files.list(target)) {
            return jars.filter(path -> path.toString().endsWith(".jar"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(MainClass::mainClassIn).filter(Optional::isPresent)
                    .map(Optional::get).findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
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
                    .filter(path -> !path.getFileName().toString()
                            .contains("$"))
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

    /**
     * As much of a class file as the two questions above need: which strings
     * its constant pool holds, and which methods it declares.
     * <p>
     * Hand-parsed because the alternative is loading the class, which would
     * mean the application's classpath in the daemon JVM - the one thing this
     * module exists to avoid. Only the structure up to the method table is
     * walked; the bytecode is skipped.
     */
    private record ClassFile(List<String> strings, boolean hasMainMethod) {

        boolean isSpringBootApplication() {
            return strings.contains(SPRING_BOOT_APPLICATION);
        }

        static Optional<ClassFile> read(Path file) {
            try (InputStream in = Files.newInputStream(file);
                    DataInputStream data = new DataInputStream(in)) {
                if (data.readInt() != 0xCAFEBABE) {
                    return Optional.empty();
                }
                data.readUnsignedShort(); // minor version
                data.readUnsignedShort(); // major version
                List<String> strings = readConstantPool(data);
                data.readUnsignedShort(); // access flags
                data.readUnsignedShort(); // this class
                data.readUnsignedShort(); // super class
                skipShorts(data, data.readUnsignedShort()); // interfaces
                skipMembers(data); // fields
                return Optional.of(
                        new ClassFile(strings, hasMainMethod(data, strings)));
            } catch (IOException | RuntimeException e) {
                // A truncated or unsupported class file answers neither
                // question; the next candidate is asked instead.
                return Optional.empty();
            }
        }

        /**
         * The UTF-8 entries of the constant pool, indexed as the pool is - long
         * and double entries take two slots, which is what makes a blind walk
         * impossible and this method necessary.
         */
        private static List<String> readConstantPool(DataInputStream data)
                throws IOException {
            int count = data.readUnsignedShort();
            List<String> strings = new ArrayList<>(count);
            strings.add(""); // the pool is 1-based
            for (int index = 1; index < count; index++) {
                int tag = data.readUnsignedByte();
                switch (tag) {
                case 1 -> strings.add(data.readUTF());
                case 7, 8, 16, 19, 20 -> {
                    data.skipBytes(2);
                    strings.add("");
                }
                case 15 -> {
                    data.skipBytes(3);
                    strings.add("");
                }
                case 3, 4, 9, 10, 11, 12, 17, 18 -> {
                    data.skipBytes(4);
                    strings.add("");
                }
                case 5, 6 -> {
                    data.skipBytes(8);
                    strings.add("");
                    // A long or a double occupies the following slot as well.
                    strings.add("");
                    index++;
                }
                default -> throw new IOException(
                        "unsupported constant pool tag " + tag);
                }
            }
            return strings;
        }

        private static boolean hasMainMethod(DataInputStream data,
                List<String> strings) throws IOException {
            int methods = data.readUnsignedShort();
            for (int i = 0; i < methods; i++) {
                int flags = data.readUnsignedShort();
                String name = at(strings, data.readUnsignedShort());
                String descriptor = at(strings, data.readUnsignedShort());
                skipAttributes(data);
                if ("main".equals(name) && MAIN_DESCRIPTOR.equals(descriptor)
                        && (flags & ACC_PUBLIC) != 0
                        && (flags & ACC_STATIC) != 0) {
                    return true;
                }
            }
            return false;
        }

        private static String at(List<String> strings, int index) {
            return index >= 0 && index < strings.size() ? strings.get(index)
                    : "";
        }

        private static void skipMembers(DataInputStream data)
                throws IOException {
            int count = data.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                data.skipBytes(6); // access flags, name, descriptor
                skipAttributes(data);
            }
        }

        private static void skipAttributes(DataInputStream data)
                throws IOException {
            int count = data.readUnsignedShort();
            for (int i = 0; i < count; i++) {
                data.skipBytes(2); // name index
                int length = data.readInt();
                // skipBytes is allowed to skip fewer, and for a Code attribute
                // read from a stream it does; looping is what makes it exact.
                for (int skipped = 0; skipped < length;) {
                    int step = data.skipBytes(length - skipped);
                    if (step <= 0) {
                        throw new IOException("truncated attribute");
                    }
                    skipped += step;
                }
            }
        }
    }

    private static void skipShorts(DataInputStream data, int count)
            throws IOException {
        data.skipBytes(count * 2);
    }
}
