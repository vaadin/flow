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

import java.io.File;
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
     * @param platform
     *            what the JVM was built for, as far as its {@code release} file
     *            says
     */
    record Jdk(Path home, Path binary, int feature, String version, boolean jbr,
            Platform platform) {

        String describe() {
            return (jbr ? "JBR " : "JDK ") + version + " at " + home;
        }
    }

    /**
     * An operating system and CPU architecture, normalized so that the
     * spellings of a {@code release} file ({@code Darwin}, {@code x86_64}) and
     * of a running JVM ({@code Mac OS X}, {@code amd64}) compare equal.
     * <p>
     * Not {@code com.vaadin.flow.internal.Platform}: the daemon must not depend
     * on flow-server (see the module's enforcer rule), and that class only
     * reads the running JVM's properties, so it cannot parse a {@code release}
     * file's {@code Darwin} or tell whether one platform runs another's
     * binaries.
     *
     * @param os
     *            the normalized operating system, or {@code null} when unknown
     * @param arch
     *            the normalized architecture, or {@code null} when unknown
     */
    record Platform(String os, String arch) {

        private static final String MACOS = "macos";

        private static final String WINDOWS = "windows";

        /** The platform the daemon itself runs on. */
        static Platform current() {
            return of(System.getProperty("os.name"),
                    System.getProperty("os.arch"));
        }

        /**
         * A platform from the raw names, either a {@code release} file's
         * {@code OS_NAME} and {@code OS_ARCH} or a JVM's {@code os.name} and
         * {@code os.arch}; a missing name stays unknown.
         */
        static Platform of(String osName, String osArch) {
            return new Platform(normalizeOs(osName), normalizeArch(osArch));
        }

        /**
         * Whether a JVM built for this platform can run on {@code host}. An
         * unknown part is taken to match: a home whose {@code release} file
         * does not say was a candidate before platforms were compared, and
         * rejecting it now would leave a working JDK unused.
         * <p>
         * Another operating system never runs. Another architecture only rules
         * a JVM out where the OS does not emulate it: macOS runs x86_64
         * binaries on Apple silicon through Rosetta 2 and Windows on Arm runs
         * x64 ones, and a daemon that is itself emulated reports the emulated
         * architecture, so there a mismatch says nothing about the hardware.
         */
        boolean runsOn(Platform host) {
            if (os != null && host.os != null && !os.equals(host.os)) {
                return false;
            }
            if (arch == null || host.arch == null || arch.equals(host.arch)) {
                return true;
            }
            String system = os != null ? os : host.os;
            return MACOS.equals(system) || WINDOWS.equals(system);
        }

        @Override
        public String toString() {
            return (os == null ? "unknown OS" : os) + " "
                    + (arch == null ? "unknown architecture" : arch);
        }

        private static String normalizeOs(String name) {
            if (name == null || name.isBlank()) {
                return null;
            }
            String lower = name.trim().toLowerCase(Locale.ROOT);
            if (lower.startsWith(WINDOWS)) {
                return WINDOWS;
            }
            if (lower.startsWith("mac") || lower.equals("darwin")) {
                return MACOS;
            }
            return lower;
        }

        private static String normalizeArch(String name) {
            if (name == null || name.isBlank()) {
                return null;
            }
            String lower = name.trim().toLowerCase(Locale.ROOT);
            return switch (lower) {
            case "amd64", "x64", "x86-64" -> "x86_64";
            case "i386", "i486", "i586", "i686", "x86_32" -> "x86";
            case "arm64" -> "aarch64";
            default -> lower;
            };
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
        return select(required, candidates(log), log);
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
            Platform host = Platform.current();
            if (!forced.platform().runsOn(host)) {
                log.line("WARNING: vaadin.dev.javaHome is built for "
                        + forced.platform() + " and this machine is " + host
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

    /**
     * Every installed JVM that could run the application, logging each one
     * skipped because it is built for another platform.
     */
    static List<Jdk> candidates(Launch.Log log) {
        return candidates(homes(), Platform.current(), log);
    }

    /**
     * As {@link #candidates(Launch.Log)}, over given homes and for a given host
     * platform, which is how this is tested: the answer must not depend on what
     * the developer happens to have installed, nor on the machine the test runs
     * on.
     */
    static List<Jdk> candidates(List<Path> homes, Platform host,
            Launch.Log log) {
        List<Jdk> found = new ArrayList<>();
        List<Path> seen = new ArrayList<>();
        for (Path home : homes) {
            Path real = Reactor.real(home);
            if (!seen.contains(real)) {
                seen.add(real);
                Jdk jdk = describe(real);
                if (canRun(jdk, host, log)) {
                    found.add(jdk);
                }
            }
        }
        return found;
    }

    /**
     * Whether a discovered JVM can run the application on {@code host}, logging
     * it when it is skipped for being built for another platform.
     */
    private static boolean canRun(Jdk jdk, Platform host, Launch.Log log) {
        // Below the floor is not a candidate at all: it cannot load a class
        // file Flow compiled, so ranking it would only ever produce an
        // UnsupportedClassVersionError with a JVM the daemon chose itself.
        if (jdk.feature() < FLOOR || !Files.isRegularFile(jdk.binary())) {
            return false;
        }
        // A home directory shared with a machine of another platform - a
        // container bind-mounting the host's - holds JVMs that cannot even be
        // executed here, and the closest version is as likely to be one of
        // those as not.
        if (!jdk.platform().runsOn(host)) {
            log.line("skipped " + jdk.describe() + ": built for "
                    + jdk.platform() + ", this machine is " + host);
            return false;
        }
        return true;
    }

    /**
     * Where JVMs are looked for: the directory JetBrains IDEs download runtimes
     * into, the one Vaadin's own tooling installs into, plus whatever the
     * environment already points at.
     */
    static List<Path> homes() {
        return homes(Path.of(System.getProperty("user.home", ".")));
    }

    /**
     * As {@link #homes()}, over a given home directory, which is how this is
     * tested: the answer must not depend on what the developer happens to have
     * installed.
     */
    static List<Path> homes(Path userHome) {
        List<Path> homes = new ArrayList<>();
        installsUnder(userHome.resolve(".jdks"), homes);
        // Where the Vaadin plugins for IntelliJ IDEA, VS Code and Eclipse put
        // the JetBrains Runtime they offer to download - so a developer who
        // took that offer has exactly the JVM this loop wants, and skipping
        // the directory would cost them enhanced class redefinition for the
        // whole session while a JBR sat on disk.
        installsUnder(userHome.resolve(".vaadin").resolve("jdk"), homes);
        for (String variable : List.of("JAVA_HOME", "JDK_HOME")) {
            String value = System.getenv(variable);
            if (value != null && !value.isBlank()) {
                homes.add(Path.of(value));
            }
        }
        return homes;
    }

    /**
     * Every install directly under a directory JVMs are installed into.
     * <p>
     * {@code listFiles} rather than {@code Files.list}, because it answers
     * {@code null} for a directory that is missing or unreadable where the
     * stream throws, and neither is a reason to fail a launch: what is left is
     * the other directory, the environment's JVMs, and ultimately this one.
     */
    private static void installsUnder(Path parent, List<Path> homes) {
        File[] installs = parent.toFile().listFiles(File::isDirectory);
        if (installs == null) {
            return;
        }
        for (File install : installs) {
            homes.add(install.toPath());
        }
    }

    /** The JVM the daemon itself runs on, which always clears the floor. */
    static Jdk current() {
        return describe(Path.of(System.getProperty("java.home")));
    }

    /**
     * What a JDK home is, read from its {@code release} file and falling back
     * to its directory name.
     * <p>
     * The path given may be the root of a macOS bundle rather than the home
     * itself; the {@code Jdk} always names the home, which is what a launch
     * needs. The version fallback still comes from the name the caller used,
     * because that is the one carrying a version - a bundle's home is called
     * {@code Home}.
     */
    static Jdk describe(Path home) {
        Path real = bundleHome(home);
        Path binary = javaIn(real);
        Map<String, String> release = release(real);
        String version = release.get("JAVA_VERSION");
        Path name = home.getFileName();
        int feature = featureOf(
                version != null ? version : name == null ? "" : name.toString())
                .orElse(0);
        return new Jdk(real, binary, feature,
                version != null ? version : "unknown version",
                isJetBrains(release.get("IMPLEMENTOR"), binary),
                Platform.of(release.get("OS_NAME"), release.get("OS_ARCH")));
    }

    /** The launcher in a JDK home, whichever platform it is for. */
    static Path javaIn(Path home) {
        Path win = home.resolve("bin").resolve("java.exe");
        return Files.isRegularFile(win) ? win
                : home.resolve("bin").resolve("java");
    }

    /**
     * The home inside a macOS bundle, or the path itself when it is already a
     * home.
     * <p>
     * A JDK installed on macOS is a bundle whose home is
     * {@code <root>/Contents/Home}, and that is the layout a JetBrains IDE
     * leaves in {@code ~/.jdks} - so without this, the runtime a developer
     * already has is the one never found: the bundle root carries neither the
     * launcher nor the {@code release} file, and a candidate with no launcher
     * is dropped without a word. Resolved for anything named as a home,
     * {@code vaadin.dev.javaHome} included, since a bundle root is what a
     * Finder window offers.
     */
    private static Path bundleHome(Path home) {
        if (Files.isRegularFile(javaIn(home))) {
            return home;
        }
        Path inside = home.resolve("Contents").resolve("Home");
        return Files.isRegularFile(javaIn(inside)) ? inside : home;
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
