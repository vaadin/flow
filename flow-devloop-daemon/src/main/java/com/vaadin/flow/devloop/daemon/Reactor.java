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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Maven reactor the served application belongs to: where its root is, which
 * modules it aggregates, and which poms decide the application's classpath.
 * <p>
 * This is a <em>structural</em> answer, read from poms with the JDK's own XML
 * parser - the daemon has no dependencies and is not going to grow Maven
 * Resolver for this. Which modules are actually in the edit loop is a different
 * question, answered in {@link Launch} against the classpath Maven itself
 * emitted, because that is the only source accounting for profiles, dependency
 * management and transitivity. The enumeration here is therefore allowed to be
 * a superset: it is used to find the root, to name the modules left out of the
 * loop, and to know which poms invalidate the classpath cache.
 * <p>
 * A project with no aggregator above it resolves to a reactor of one, which is
 * exactly the behaviour the daemon had before it knew about reactors.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class Reactor {

    /**
     * One compile domain: a module whose sources the daemon may compile and
     * whose classes it may redefine.
     * <p>
     * Nested rather than top-level on purpose: a same-package {@code Module}
     * would shadow {@link java.lang.Module} at every use site in this package,
     * and {@code Reactor.Module} reads as what it is.
     */
    record Module(String artifactId, Path dir, Path sourceDir, Path resourceDir,
            Path classesDir) {

        /**
         * The Maven defaults, which is what all but a handful of projects use.
         */
        static Module of(Path dir, String artifactId) {
            return of(dir, artifactId,
                    dir.resolve("target").resolve("classes"));
        }

        /**
         * With the output directory known from elsewhere - a directory entry on
         * the resolved classpath is authoritative even when a pom overrides
         * {@code <outputDirectory>}.
         */
        static Module of(Path dir, String artifactId, Path classesDir) {
            return new Module(artifactId, dir,
                    dir.resolve("src").resolve("main").resolve("java"),
                    dir.resolve("src").resolve("main").resolve("resources"),
                    classesDir);
        }

        /**
         * Whether this module owns a file, tested by prefix rather than by
         * relativizing: {@code relativize} throws for a path outside the module
         * - which is every path in every other module - and on Windows it
         * throws across drive letters as well.
         */
        boolean owns(Path file) {
            return file.startsWith(sourceDir) || file.startsWith(resourceDir);
        }

        /** The {@code .class} file a source compiles to. */
        Path artifactFor(Path source) {
            String relative = sourceDir.relativize(source).toString();
            return classesDir.resolve(relative.endsWith(".java")
                    ? relative.substring(0,
                            relative.length() - ".java".length()) + ".class"
                    : relative);
        }

        /** The classpath copy of a resource. */
        Path targetFor(Path resource) {
            return classesDir
                    .resolve(resourceDir.relativize(resource).toString());
        }

        /** The binary name of a class file under this module's output. */
        String binaryNameOf(Path classFile) {
            String name = classesDir.relativize(classFile).toString()
                    .replace(File.separatorChar, '.');
            return name.endsWith(".class")
                    ? name.substring(0, name.length() - ".class".length())
                    : name;
        }

        /**
         * How a module is named in output: by its directory, which is what a
         * developer navigates, rather than by an artifactId they may never have
         * read.
         */
        String name() {
            Path fileName = dir.getFileName();
            return fileName == null ? dir.toString() : fileName.toString();
        }

        boolean hasSources() {
            return Files.isDirectory(sourceDir)
                    || Files.isDirectory(resourceDir);
        }
    }

    /**
     * {@code ${property}} in a module path, which multi-variant repositories
     * use.
     */
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)\\}");

    /** How many module names {@link #describe()} spells out before counting. */
    private static final int NAMES_SHOWN = 8;

    private final Path root;
    private final Module app;
    private final List<Module> candidates;
    private final List<Path> poms;

    private Reactor(Path root, Module app, List<Module> candidates,
            List<Path> poms) {
        this.root = root;
        this.app = app;
        this.candidates = List.copyOf(candidates);
        this.poms = List.copyOf(poms);
    }

    Path root() {
        return root;
    }

    Module app() {
        return app;
    }

    /** Every module that could join the loop, the application first. */
    List<Module> candidates() {
        return candidates;
    }

    /**
     * Every pom read, aggregators included: any of them can change the
     * classpath.
     */
    List<Path> poms() {
        return poms;
    }

    boolean isMultiModule() {
        return !root.equals(app.dir());
    }

    /**
     * Finds the reactor root above an application module by verification rather
     * than by assumption: an ancestor counts only if its {@code <modules>},
     * expanded recursively, actually contains this module. The highest ancestor
     * that verifies wins, because intermediate aggregators exist and
     * {@code -am} needs the whole graph.
     * <p>
     * The {@code <parent>} chain is deliberately not consulted. Aggregation and
     * inheritance are independent in Maven, and this repository's own demo
     * application is the counterexample: it is parented by
     * {@code spring-boot-starter-parent} with an empty {@code <relativePath/>}
     * while the root pom aggregates it, so the parent chain leads out of the
     * project entirely.
     */
    static Reactor discover(Path appModule, Launch.Log log) {
        Path app = real(appModule);
        String override = System.getProperty("vaadin.dev.reactorRoot");
        if (override != null && !override.isBlank()) {
            Path forced = real(Path.of(override).toAbsolutePath());
            Optional<Reactor> built = build(forced, app, log);
            if (built.isPresent()) {
                return built.get();
            }
            log.line("vaadin.dev.reactorRoot=" + forced + " does not aggregate "
                    + app.getFileName()
                    + "; treating the application as a single module");
            return single(app);
        }
        Reactor found = null;
        for (Path ancestor = app
                .getParent(); ancestor != null; ancestor = ancestor
                        .getParent()) {
            if (!Files.isRegularFile(ancestor.resolve("pom.xml"))) {
                continue;
            }
            // No early return: a higher aggregator may aggregate this one, and
            // it
            // is the top of the chain that -pl/-am has to run against.
            Optional<Reactor> built = build(ancestor, app, log);
            if (built.isPresent()) {
                found = built.get();
            }
        }
        return found != null ? found : single(app);
    }

    /**
     * A project with no aggregator: one module, and the daemon's original
     * behaviour.
     */
    private static Reactor single(Path appModule) {
        Pom pom = Pom.read(appModule.resolve("pom.xml"));
        Module app = Module.of(appModule, pom.artifactId());
        return new Reactor(appModule, app, List.of(app),
                List.of(appModule.resolve("pom.xml")));
    }

    /**
     * Builds the reactor rooted at a candidate directory, or empty when that
     * candidate does not aggregate the application.
     */
    private static Optional<Reactor> build(Path candidateRoot, Path appModule,
            Launch.Log log) {
        Map<Path, Pom> poms = new LinkedHashMap<>();
        collect(candidateRoot, poms, log);
        if (!poms.containsKey(appModule)) {
            return Optional.empty();
        }
        Module app = Module.of(appModule, poms.get(appModule).artifactId());
        List<Module> candidates = new ArrayList<>();
        candidates.add(app);
        poms.forEach((dir, pom) -> {
            if (dir.equals(appModule) || "pom".equals(pom.packaging())) {
                return;
            }
            Module module = Module.of(dir, pom.artifactId());
            if (module.hasSources()) {
                candidates.add(module);
            }
        });
        List<Path> pomFiles = poms.keySet().stream()
                .map(dir -> dir.resolve("pom.xml")).toList();
        return Optional
                .of(new Reactor(candidateRoot, app, candidates, pomFiles));
    }

    /** Walks the aggregation graph; the visited map is also the cycle guard. */
    private static void collect(Path dir, Map<Path, Pom> into, Launch.Log log) {
        Path pomFile = dir.resolve("pom.xml");
        if (into.containsKey(dir) || !Files.isRegularFile(pomFile)) {
            return;
        }
        Pom pom = Pom.read(pomFile);
        into.put(dir, pom);
        for (String declared : pom.modules()) {
            String resolved = interpolate(declared, pom.properties());
            if (resolved == null) {
                log.line("skipping module " + declared + " in " + pomFile
                        + ": unresolved property");
                continue;
            }
            Path target = dir.resolve(resolved).normalize();
            // A <module> may name a directory or the pom file itself; Maven
            // appends pom.xml only for the former.
            Path moduleDir = Files.isRegularFile(target) ? target.getParent()
                    : target;
            if (moduleDir != null) {
                collect(real(moduleDir), into, log);
            }
        }
    }

    private static String interpolate(String value,
            Map<String, String> properties) {
        Matcher matcher = PROPERTY.matcher(value);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String replacement = properties.get(matcher.group(1));
            if (replacement == null) {
                return null;
            }
            matcher.appendReplacement(out,
                    Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * The canonical path, so two spellings of one directory - a symlinked
     * checkout, {@code ../app}, a short Windows path - compare equal. Falls
     * back to normalisation for a directory that does not exist yet.
     */
    static Path real(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }

    /**
     * Names of the candidate modules, application first, for one log line.
     * <p>
     * Truncated, because this really is one line. A project whose reactor root
     * was discovered higher up than intended - Flow's own repository aggregates
     * 149 modules - would otherwise print every one of them, burying the count
     * that is the actionable part of the message.
     */
    String describe() {
        Set<String> names = new LinkedHashSet<>();
        candidates.forEach(module -> names.add(module.name()));
        List<String> listed = List.copyOf(names);
        String shown = String.join(", ",
                listed.subList(0, Math.min(NAMES_SHOWN, listed.size())));
        if (listed.size() > NAMES_SHOWN) {
            shown += " and " + (listed.size() - NAMES_SHOWN) + " more";
        }
        return listed.size() + " module(s): " + shown;
    }

    /**
     * The Java release this project is built for, when a pom says so.
     * <p>
     * The application module first and the reactor root second, because a
     * multi-module project normally declares the level once at the top and a
     * module that overrides it means it. A pom that inherits the level from a
     * parent outside the checkout - {@code spring-boot-starter-parent} being
     * the common case - yields nothing here, and is answered from the compiled
     * bytecode instead; see {@link Jvm#required}.
     */
    OptionalInt requiredRelease() {
        OptionalInt fromApp = releaseIn(app.dir());
        return fromApp.isPresent() ? fromApp : releaseIn(root);
    }

    private static OptionalInt releaseIn(Path moduleDir) {
        Pom pom = Pom.read(moduleDir.resolve("pom.xml"));
        String declared = pom.declaredRelease();
        if (declared == null) {
            return OptionalInt.empty();
        }
        // <java.version>21</java.version> reaching maven.compiler.release as
        // ${java.version} is how every Spring Boot project spells this.
        String resolved = interpolate(declared, pom.properties());
        return resolved == null ? OptionalInt.empty() : Jvm.featureOf(resolved);
    }

    /**
     * The four things a pom is read for.
     * <p>
     * Namespace-unaware on purpose: element names are all that matter here, and
     * the POM namespace is declared inconsistently in the wild.
     */
    private record Pom(String artifactId, String packaging,
            List<String> modules, Map<String, String> properties,
            String compilerRelease) {

        /**
         * The Java release this pom declares, still uninterpolated, or
         * {@code null} when it declares none.
         * <p>
         * The compiler plugin's own configuration outranks the properties: a
         * pom setting both means the one Maven will act on. {@code target}
         * before {@code source} because it is {@code target} that decides which
         * JVM can load the result.
         */
        String declaredRelease() {
            if (compilerRelease != null && !compilerRelease.isBlank()) {
                return compilerRelease;
            }
            for (String key : List.of("maven.compiler.release",
                    "maven.compiler.target", "maven.compiler.source",
                    "java.version")) {
                String value = properties.get(key);
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
            return null;
        }

        static Pom read(Path file) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory
                        .newInstance();
                factory.setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true);
                factory.setExpandEntityReferences(false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(file.toFile());
                Element project = document.getDocumentElement();
                // Every <modules> element, top-level and inside a profile
                // alike:
                // evaluating profile activation is not something a JDK-only
                // reader can do, and a superset is harmless - it only answers
                // "is
                // my directory aggregated below this pom?", while the edit loop
                // itself is decided by the resolved classpath.
                List<String> modules = new ArrayList<>();
                for (Element element : elementsNamed(document, "modules")) {
                    for (Element module : children(element, "module")) {
                        modules.add(text(module));
                    }
                }
                Map<String, String> properties = new LinkedHashMap<>();
                for (Element element : elementsNamed(document, "properties")) {
                    for (Element property : children(element, null)) {
                        properties.put(property.getTagName(), text(property));
                    }
                }
                String artifactId = childText(project, "artifactId");
                String packaging = childText(project, "packaging");
                return new Pom(artifactId == null ? "" : artifactId,
                        packaging == null ? "jar" : packaging, modules,
                        properties, compilerRelease(document));
            } catch (Exception e) {
                // An unreadable pom is not a reason to refuse to serve the app:
                // it yields no modules, which degrades to single-module mode.
                return new Pom("", "jar", List.of(), Map.of(), null);
            }
        }

        /**
         * {@code maven-compiler-plugin}'s configured level, wherever it is
         * declared.
         * <p>
         * Every {@code <plugin>} in the document, {@code <pluginManagement>}
         * and profiles included, for the same reason {@code <modules>} is read
         * that way: a JDK-only reader cannot evaluate profile activation, and
         * one declaration of the compiler level is what these poms have.
         */
        private static String compilerRelease(Document document) {
            for (Element plugin : elementsNamed(document, "plugin")) {
                if (!"maven-compiler-plugin"
                        .equals(childText(plugin, "artifactId"))) {
                    continue;
                }
                for (Element configuration : children(plugin,
                        "configuration")) {
                    for (String name : List.of("release", "target", "source")) {
                        String value = childText(configuration, name);
                        if (value != null && !value.isBlank()) {
                            return value;
                        }
                    }
                }
            }
            return null;
        }

        private static List<Element> elementsNamed(Document document,
                String name) {
            List<Element> found = new ArrayList<>();
            NodeList nodes = document.getElementsByTagName(name);
            for (int i = 0; i < nodes.getLength(); i++) {
                found.add((Element) nodes.item(i));
            }
            return found;
        }

        /**
         * Direct children only, so a nested {@code artifactId} - every
         * dependency has one - cannot be mistaken for the project's own.
         */
        private static List<Element> children(Element parent, String name) {
            List<Element> found = new ArrayList<>();
            NodeList nodes = parent.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node instanceof Element element && (name == null
                        || name.equals(element.getTagName()))) {
                    found.add(element);
                }
            }
            return found;
        }

        private static String childText(Element parent, String name) {
            List<Element> found = children(parent, name);
            return found.isEmpty() ? null : text(found.get(0));
        }

        private static String text(Element element) {
            String content = element.getTextContent();
            return content == null ? "" : content.trim();
        }
    }
}
