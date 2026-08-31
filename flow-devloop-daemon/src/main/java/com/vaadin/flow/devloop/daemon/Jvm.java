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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Which JVM runs the served application, and at which level its sources are
 * compiled.
 * <p>
 * A JBR is preferred because {@code -XX:+AllowEnhancedClassRedefinition} is a
 * JVM feature and not an agent one: without it a structural change is rejected
 * outright and the dev loop can only escalate to a restart. But preferring a
 * JBR is not the same as taking the first directory whose name starts with
 * {@code jbr} - one too old for the project cannot run the application at all,
 * which is a worse outcome than losing enhanced redefinition. So the version is
 * read rather than assumed, from each candidate's own {@code release} file, and
 * compared against what the project needs.
 */
final class Jvm {

    /**
     * The platform baseline: Flow requires Java 21, so a JVM below it cannot
     * run a served application whatever the project's own poms say.
     */
    static final int FLOOR = 21;

    /**
     * A JVM the daemon could launch the application with.
     *
     * @param feature
     *            the feature version, or {@code 0} when it could not be read -
     *            which only survives for an explicitly overridden home, since
     *            discovery drops anything that does not clear {@link #FLOOR}
     */
    record Jdk(Path home, Path binary, int feature, String version,
            boolean jbr) {

        String describe() {
            return (jbr ? "JBR " : "JDK ") + version + " at " + home;
        }
    }

    /** A required Java version, and what said so. */
    record Requirement(int feature, String source) {
    }

    /**
     * Closest above the requirement first: an exact match therefore wins, and
     * between two installs of the same feature version the newer patch does.
     */
    private static final Comparator<Jdk> CLOSEST = Comparator
            .comparingInt(Jdk::feature).thenComparing(Comparator
                    .comparing(Jdk::version, Jvm::compareVersions).reversed());

    /** The first number in a version string, and the second when it is 1.x. */
    private static final Pattern VERSION = Pattern
            .compile("(\\d+)(?:\\.(\\d+))?");

    /** One numeric segment of a version, for comparing 21.0.10 with 21.0.6. */
    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;

    /**
     * A class file's major version is its Java feature version plus this: 65 is
     * Java 21.
     */
    private static final int MAJOR_OFFSET = 44;

    private Jvm() {
    }

    /**
     * The JVM to launch the application with, and why.
     * <p>
     * Logs the decision as one line: which JVM won, what the project needed,
     * and what it cost when the answer was not a JBR. A developer who wonders
     * why a structural change restarted needs that line to be in the log
     * already, because by the time they wonder, the launch is long past.
     */
    static Jdk select(Optional<Requirement> required, Launch.Log log) {
        return select(required, candidates(), log);
    }

    /**
     * As {@link #select(Optional, Launch.Log)}, over a given candidate list,
     * which is how this is tested: the answer must not depend on what the
     * developer happens to have installed.
     */
    static Jdk select(Optional<Requirement> required, List<Jdk> candidates,
            Launch.Log log) {
        String override = System.getProperty("vaadin.dev.javaHome");
        if (override != null && !override.isBlank()) {
            Jdk forced = describe(Path.of(override));
            if (forced.feature() > 0 && forced.feature() < FLOOR) {
                log.line("WARNING: vaadin.dev.javaHome is Java "
                        + forced.feature() + " and Flow requires " + FLOOR
                        + "; the application will not start");
            }
            log.line(
                    "app JVM: " + forced.describe() + " (vaadin.dev.javaHome)");
            return forced;
        }

        int wanted = Math.max(required.map(Requirement::feature).orElse(FLOOR),
                FLOOR);
        String because = required.map(requirement -> requirement.feature()
                + " from " + requirement.source()).orElse("nothing declared");
        Optional<Jdk> jbr = candidates.stream().filter(Jdk::jbr)
                .filter(jdk -> jdk.feature() >= wanted).min(CLOSEST);
        if (jbr.isPresent()) {
            log.line("app JVM: " + jbr.get().describe() + " (needs " + wanted
                    + "; " + because + ")");
            return jbr.get();
        }

        // No JBR clears the bar. A JDK that does still runs the application; it
        // only costs enhanced redefinition, and that is worth saying out loud
        // because it changes what an apply can do for the rest of the session.
        Jdk chosen = candidates.stream().filter(jdk -> jdk.feature() >= wanted)
                .min(CLOSEST).orElseGet(Jvm::current);
        String note = chosen.jbr() ? ""
                : " - no JetBrains Runtime for Java " + wanted
                        + " installed, so a structural change will restart "
                        + "instead of hot reloading";
        if (chosen.feature() > 0 && chosen.feature() < wanted) {
            note = " - WARNING: below the Java " + wanted
                    + " this project needs" + note;
        }
        log.line("app JVM: " + chosen.describe() + " (needs " + wanted + "; "
                + because + ")" + note);
        return chosen;
    }

    /**
     * The Java version the project needs, from the poms and then from the
     * bytecode already on disk.
     * <p>
     * Two sources because they fail in opposite directions. The poms are the
     * declared intent but say nothing when the level is inherited from a parent
     * outside the checkout, which is every {@code spring-boot-starter-parent}
     * project that leaves {@code java.version} alone. The compiled classes
     * always answer, but only once the module has been built.
     */
    static Optional<Requirement> required(Reactor reactor) {
        OptionalInt fromPoms = reactor.requiredRelease();
        if (fromPoms.isPresent()) {
            return Optional
                    .of(new Requirement(fromPoms.getAsInt(), "the poms"));
        }
        OptionalInt fromClasses = compiledFeature(reactor.app().classesDir());
        return fromClasses.isPresent() ? Optional.of(
                new Requirement(fromClasses.getAsInt(), "the compiled classes"))
                : Optional.empty();
    }

    /**
     * Whether this JVM honours {@code -XX:+AllowEnhancedClassRedefinition}.
     * <p>
     * By {@code IMPLEMENTOR} in the JDK's own {@code release} file, so a JBR
     * installed anywhere is recognised and a stock JDK under a path that
     * happens to contain {@code jbr} is not. The path test remains for a home
     * with no readable {@code release} - it is what the daemon always used, and
     * a wrong answer there is no worse than before.
     */
    static boolean supportsEnhancedRedefinition(Path javaBinary) {
        Path bin = javaBinary.getParent();
        Path home = bin == null ? null : bin.getParent();
        return isJetBrains(
                home == null ? null : release(home).get("IMPLEMENTOR"),
                javaBinary);
    }

    private static boolean isJetBrains(String implementor, Path javaBinary) {
        if (implementor != null) {
            return implementor.toLowerCase(Locale.ROOT).contains("jetbrains");
        }
        return javaBinary.toString().toLowerCase(Locale.ROOT).contains("jbr");
    }

    /**
     * The feature version in a version string or a directory name:
     * {@code 1.8.0_442} is 8, {@code 21.0.5} and {@code jbr-21.0.5} are 21,
     * {@code 17-ea} is 17.
     */
    static OptionalInt featureOf(String text) {
        Matcher matcher = VERSION.matcher(text);
        if (!matcher.find()) {
            return OptionalInt.empty();
        }
        int first = Integer.parseInt(matcher.group(1));
        // 1.8 is Java 8; 1.x has not been spelled that way since Java 9.
        if (first == 1 && matcher.group(2) != null) {
            return OptionalInt.of(Integer.parseInt(matcher.group(2)));
        }
        return OptionalInt.of(first);
    }

    /** Every installed JVM that could run the application. */
    static List<Jdk> candidates() {
        return candidates(homes());
    }

    /**
     * As {@link #candidates()}, over given homes, which is how this is tested:
     * the answer must not depend on what the developer happens to have
     * installed.
     */
    static List<Jdk> candidates(List<Path> homes) {
        List<Jdk> found = new ArrayList<>();
        List<Path> seen = new ArrayList<>();
        for (Path home : homes) {
            Path real = Reactor.real(home);
            if (seen.contains(real)) {
                continue;
            }
            seen.add(real);
            Jdk jdk = describe(real);
            // Below the floor is not a candidate at all: it cannot load a class
            // file Flow compiled, so ranking it would only ever produce an
            // UnsupportedClassVersionError with a JVM the daemon chose itself.
            if (jdk.feature() >= FLOOR && Files.isRegularFile(jdk.binary())) {
                found.add(jdk);
            }
        }
        return found;
    }

    /**
     * Where JVMs are looked for: the directory JetBrains IDEs download runtimes
     * into, plus whatever the environment already points at.
     */
    static List<Path> homes() {
        List<Path> homes = new ArrayList<>();
        Path jdks = Path.of(System.getProperty("user.home"), ".jdks");
        if (Files.isDirectory(jdks)) {
            try (Stream<Path> stream = Files.list(jdks)) {
                stream.filter(Files::isDirectory).forEach(homes::add);
            } catch (IOException e) {
                // An unreadable ~/.jdks leaves the environment's JVMs, and
                // ultimately this one: never a reason to fail a launch.
                homes.clear();
            }
        }
        for (String variable : List.of("JAVA_HOME", "JDK_HOME")) {
            String value = System.getenv(variable);
            if (value != null && !value.isBlank()) {
                homes.add(Path.of(value));
            }
        }
        return homes;
    }

    /** The JVM the daemon itself runs on, which always clears the floor. */
    static Jdk current() {
        return describe(Path.of(System.getProperty("java.home")));
    }

    /**
     * What a JDK home is, read from its {@code release} file and falling back
     * to its directory name.
     */
    static Jdk describe(Path home) {
        Path binary = javaIn(home);
        Map<String, String> release = release(home);
        String version = release.get("JAVA_VERSION");
        Path name = home.getFileName();
        int feature = featureOf(
                version != null ? version : name == null ? "" : name.toString())
                .orElse(0);
        return new Jdk(home, binary, feature,
                version != null ? version : "unknown version",
                isJetBrains(release.get("IMPLEMENTOR"), binary));
    }

    /** The launcher in a JDK home, whichever platform it is for. */
    static Path javaIn(Path home) {
        Path win = home.resolve("bin").resolve("java.exe");
        return Files.isRegularFile(win) ? win
                : home.resolve("bin").resolve("java");
    }

    /**
     * The {@code KEY="value"} lines every JDK since 9 ships in
     * {@code <home>/release}; empty for a home that has none.
     */
    private static Map<String, String> release(Path home) {
        Map<String, String> values = new LinkedHashMap<>();
        Path file = home.resolve("release");
        if (!Files.isRegularFile(file)) {
            return values;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                int equals = line.indexOf('=');
                if (equals > 0) {
                    values.put(line.substring(0, equals).trim(), line
                            .substring(equals + 1).trim().replace("\"", ""));
                }
            }
        } catch (IOException | UncheckedIOException e) {
            return Map.of();
        }
        return values;
    }

    /**
     * The feature version of the bytecode already under an output directory,
     * from the first class file found.
     * <p>
     * One file is enough: a module compiles to one level, and walking the whole
     * output to confirm it would cost a directory tree on every launch.
     */
    private static OptionalInt compiledFeature(Path classesDir) {
        if (!Files.isDirectory(classesDir)) {
            return OptionalInt.empty();
        }
        try (Stream<Path> stream = Files.walk(classesDir)) {
            Optional<Path> first = stream
                    .filter(path -> path.toString().endsWith(".class"))
                    .filter(Files::isRegularFile).findFirst();
            if (first.isEmpty()) {
                return OptionalInt.empty();
            }
            byte[] header = readHeader(first.get());
            if (header.length < 8) {
                return OptionalInt.empty();
            }
            int magic = ((header[0] & 0xFF) << 24) | ((header[1] & 0xFF) << 16)
                    | ((header[2] & 0xFF) << 8) | (header[3] & 0xFF);
            if (magic != CLASS_FILE_MAGIC) {
                return OptionalInt.empty();
            }
            int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
            return major > MAJOR_OFFSET ? OptionalInt.of(major - MAJOR_OFFSET)
                    : OptionalInt.empty();
        } catch (IOException | UncheckedIOException e) {
            return OptionalInt.empty();
        }
    }

    private static byte[] readHeader(Path classFile) throws IOException {
        try (var in = Files.newInputStream(classFile)) {
            return in.readNBytes(8);
        }
    }

    /**
     * Compares version strings by their numeric segments, so 21.0.10 is above
     * 21.0.6 - which a lexicographic comparison gets backwards.
     */
    private static int compareVersions(String left, String right) {
        List<Integer> a = segments(left);
        List<Integer> b = segments(right);
        for (int i = 0; i < Math.max(a.size(), b.size()); i++) {
            int compared = Integer.compare(i < a.size() ? a.get(i) : 0,
                    i < b.size() ? b.get(i) : 0);
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private static List<Integer> segments(String version) {
        List<Integer> numbers = new ArrayList<>();
        Matcher matcher = NUMBER.matcher(version);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }
}
