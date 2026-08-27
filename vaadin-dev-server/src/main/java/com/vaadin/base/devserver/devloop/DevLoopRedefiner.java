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
package com.vaadin.base.devserver.devloop;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.Hotswapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.VaadinService;

/**
 * The runtime leg, in-process: one atomic redefine of every changed class,
 * followed by the {@code onHotswap} call that makes Vaadin notice.
 * <p>
 * Two rules are encoded here, and both were arrived at by measurement.
 * <ul>
 * <li><b>Every loaded copy.</b> A binary name can map to more than one loaded
 * {@code Class}; redefining one leaves the copy the application instantiates
 * untouched while {@code redefineClasses} still reports success. That is the
 * silent "green apply on a stale page" failure, so all copies go into the one
 * call.</li>
 * <li><b>Report what cannot work.</b> A successful redefine is not proof the
 * change is live. Entity mappings never are, and a structural change to a
 * proxied Spring bean is actively broken, so the classes involved are reported
 * back and the daemon escalates to a restart instead of claiming success.</li>
 * </ul>
 * For internal use only. May be renamed or removed in a future release.
 */
final class DevLoopRedefiner {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DevLoopRedefiner.class);

    /**
     * Where the agent publishes the JVM's instrumentation handle. The system
     * properties table is a {@code Hashtable<Object, Object>} and can therefore
     * carry an arbitrary object, which is what makes this work regardless of
     * which class loader owns the application classes.
     */
    private static final String INSTRUMENTATION_PROPERTY = "devloop.instrumentation";

    /**
     * Where the connector reads the bytes of a class it is asked to redefine. A
     * path list, because a change can land in any in-loop module's output and a
     * {@code REDEFINE} request carries only binary names - deliberately, so the
     * wire contract says nothing about where bytes live.
     */
    private static final String CLASSES_PROPERTY = "vaadin.devloop.classes";

    private static final String[] PUBLIC_RESOURCE_ROOTS = {
            "/META-INF/resources/", "/static/", "/public/", "/resources/" };

    private DevLoopRedefiner() {
    }

    /**
     * Answers a single {@code REDEFINE a.b.C,a.b.D} request.
     *
     * @param csv
     *            the requested binary names, comma-separated
     * @return the reply line
     */
    static String redefine(String csv) {
        Instrumentation inst = instrumentation();
        if (inst == null) {
            return "ERR kind=no-agent message=Instrumentation-unavailable";
        }
        Hotswapper hotswapper = DevLoopRegistration.hotswapper().orElse(null);
        if (hotswapper == null) {
            return "ERR kind=no-hotswapper message=Hotswapper-not-registered";
        }

        List<String> requested = Arrays.stream(csv.split(",")).map(String::trim)
                .filter(name -> !name.isEmpty()).toList();
        if (requested.isEmpty()) {
            return "ERR kind=protocol message=no-classes";
        }

        List<Path> classesDirs = searchPath();

        Map<String, List<Class<?>>> loaded = new HashMap<>();
        // Collected in the same pass, because the one walk over every loaded
        // class is the expensive part and a proxy is never a requested class.
        List<Class<?>> proxies = new ArrayList<>();
        for (Class<?> candidate : inst.getAllLoadedClasses()) {
            if (requested.contains(candidate.getName())) {
                loaded.computeIfAbsent(candidate.getName(),
                        key -> new ArrayList<>()).add(candidate);
            } else if (isProxy(candidate)) {
                proxies.add(candidate);
            }
        }

        List<ClassDefinition> definitions = new ArrayList<>();
        List<String> notLoaded = new ArrayList<>();
        int duplicates = 0;
        Set<String> entities = new LinkedHashSet<>();
        Set<String> beans = new LinkedHashSet<>();
        Set<String> uiClasses = new LinkedHashSet<>();

        for (String name : requested) {
            List<Class<?>> targets = loaded.getOrDefault(name, List.of());
            if (targets.isEmpty()) {
                notLoaded.add(name);
                continue;
            }
            if (targets.size() > 1) {
                duplicates += targets.size() - 1;
            }
            Class<?> first = targets.get(0);
            if (isEntity(first)) {
                entities.add(simple(name));
            }
            if (isSpringBean(first)) {
                beans.add(simple(name));
            }
            if (isUiRefreshed(first)) {
                uiClasses.add(simple(name));
            }
            byte[] bytes = readClassBytes(classesDirs, name);
            if (bytes == null) {
                return "ERR kind=missing-class-file searched="
                        + classesDirs.size() + " message=" + name;
            }
            for (Class<?> target : targets) {
                definitions.add(new ClassDefinition(target, bytes));
            }
        }

        DevLoopHotswapper observer = DevLoopHotswapper.getActive();
        if (observer != null) {
            observer.reset();
        }

        // Member signatures before the redefine, so a structural change can
        // be named afterwards. On a stock JVM such a change is simply
        // rejected, but an enhanced-redefinition JVM accepts it - and that is
        // exactly when a Spring bean's live proxy stops matching the class.
        Map<String, String> before = new HashMap<>();
        for (ClassDefinition definition : definitions) {
            before.putIfAbsent(definition.getDefinitionClass().getName(),
                    members(definition.getDefinitionClass()));
        }

        long redefineStart = System.nanoTime();
        if (!definitions.isEmpty()) {
            try {
                inst.redefineClasses(
                        definitions.toArray(new ClassDefinition[0]));
            } catch (Throwable t) {
                return "ERR kind=redefine-rejected class="
                        + t.getClass().getSimpleName() + " message="
                        + oneLine(String.valueOf(t.getMessage()));
            }
        }
        long redefineMs = (System.nanoTime() - redefineStart) / 1_000_000;

        Set<String> structural = new LinkedHashSet<>();
        Set<String> proxied = new LinkedHashSet<>();
        for (ClassDefinition definition : definitions) {
            Class<?> type = definition.getDefinitionClass();
            String previous = before.get(type.getName());
            if (previous != null && !previous.equals(members(type))) {
                structural.add(simple(type.getName()));
            }
            for (Class<?> proxy : proxies) {
                if (type.isAssignableFrom(proxy)) {
                    proxied.add(simple(type.getName()));
                    break;
                }
            }
        }

        long hotswapStart = System.nanoTime();
        hotswapper.onHotswap(requested.toArray(new String[0]), Boolean.TRUE);
        long hotswapMs = (System.nanoTime() - hotswapStart) / 1_000_000;

        boolean completed = observer != null && observer.isCompleted();
        boolean pageReload = observer != null
                && observer.isPageReloadRequired();

        return "OK redefined=" + definitions.size() + " notLoaded="
                + notLoaded.size() + " dupes=" + duplicates + " completed="
                + completed + " pageReload=" + pageReload + " entities="
                + join(entities) + " beans=" + join(beans) + " proxied="
                + join(proxied) + " structural=" + join(structural) + " ui="
                + join(uiClasses) + " hotswapAgent=" + hotswapAgentLoaded()
                + " redefineMs=" + redefineMs + " hotswapMs=" + hotswapMs;
    }

    /**
     * Where to look for the bytes of a class the daemon asks for.
     * <p>
     * The order is the application's own classpath order, so a class that
     * exists twice resolves to the copy the JVM resolved. The single relative
     * default keeps a hand-started application working exactly as it did.
     */
    private static List<Path> searchPath() {
        String configured = System.getProperty(CLASSES_PROPERTY,
                "target/classes");
        List<Path> dirs = new ArrayList<>();
        for (String value : configured.split(File.pathSeparator)) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                dirs.add(Paths.get(trimmed));
            }
        }
        return dirs;
    }

    /**
     * The first copy on the search path wins, which is what the JVM did when it
     * loaded the class. A second copy is reported rather than fixed: two
     * modules producing the same class is a build problem, and the only thing
     * worse than having it is having it silently.
     */
    private static byte[] readClassBytes(List<Path> dirs, String name) {
        String relative = name.replace('.', '/') + ".class";
        byte[] found = null;
        for (Path dir : dirs) {
            Path file = dir.resolve(relative);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            if (found != null) {
                LOGGER.warn(
                        "{} also exists in {}; redefining the copy earlier on the classpath",
                        name, dir);
                continue;
            }
            try {
                found = Files.readAllBytes(file);
            } catch (IOException e) {
                LOGGER.debug("Could not read {}", file, e);
                return null;
            }
        }
        return found;
    }

    /**
     * The resource leg. Notifies Flow of changed resources and then gets the
     * new content in front of the browser.
     * <p>
     * Pushing the content in place is preferred over a plain browser reload,
     * which loses a race the daemon's own speed creates: static resources are
     * served with {@code Cache-Control: no-cache} plus {@code Last-Modified},
     * whose one-second granularity means a copy followed ten milliseconds later
     * by a reload revalidates to 304 and the browser keeps the previous CSS.
     * Pushing content skips HTTP entirely.
     *
     * @param csv
     *            the changed resource paths, comma-separated
     * @return the reply line
     */
    static String resources(String csv) {
        Hotswapper hotswapper = DevLoopRegistration.hotswapper().orElse(null);
        if (hotswapper == null) {
            return "ERR kind=no-hotswapper message=Hotswapper-not-registered";
        }
        List<URI> uris = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                try {
                    uris.add(Paths.get(trimmed).toUri());
                } catch (RuntimeException e) {
                    return "ERR kind=protocol message=bad-path:" + trimmed;
                }
            }
        }
        if (uris.isEmpty()) {
            return "ERR kind=protocol message=no-resources";
        }

        long started = System.nanoTime();
        URI[] none = new URI[0];
        hotswapper.onHotswap(none, uris.toArray(new URI[0]), none);

        int pushed = pushStyleSheets(csv);
        boolean reloaded = pushed == 0 && requestBrowserReload();
        return "OK resources=" + uris.size() + " pushed=" + pushed
                + " browserReload=" + reloaded + " ms="
                + (System.nanoTime() - started) / 1_000_000;
    }

    /**
     * Sends new CSS content straight to the browser for each changed
     * stylesheet, keyed by the URL the client knows it as - the file name
     * relative to the public resource root, which is what
     * {@code @StyleSheet("styles.css")} resolves to.
     */
    private static int pushStyleSheets(String csv) {
        Optional<BrowserLiveReload> liveReload = liveReload();
        if (liveReload.isEmpty()) {
            return 0;
        }
        int pushed = 0;
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.endsWith(".css")) {
                continue;
            }
            Path path = Paths.get(trimmed);
            String url = publicUrlOf(path);
            if (url == null) {
                continue;
            }
            try {
                liveReload.get().update(url,
                        Files.readString(path, StandardCharsets.UTF_8));
                pushed++;
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Could not push {}", url, e);
            }
        }
        return pushed;
    }

    /** Maps a file under a public resource root to its served URL. */
    private static String publicUrlOf(Path path) {
        String normalized = path.toString().replace('\\', '/');
        for (String root : PUBLIC_RESOURCE_ROOTS) {
            int at = normalized.indexOf(root);
            if (at >= 0) {
                return normalized.substring(at + root.length());
            }
        }
        return null;
    }

    private static boolean requestBrowserReload() {
        return liveReload().map(reload -> {
            reload.reload();
            return true;
        }).orElse(false);
    }

    private static Optional<BrowserLiveReload> liveReload() {
        VaadinService service = DevLoopRegistration.service();
        return service == null ? Optional.empty()
                : BrowserLiveReloadAccessor.getLiveReloadFromService(service);
    }

    /**
     * Whether the Vite dev server is still alive.
     * <p>
     * Not via {@code DevServerWatchDog}: that runs the other way round - the
     * JVM opens a socket so <em>Vite</em> can notice the JVM died. The dev-mode
     * handler exposes Vite's port, and connecting to an HTTP port is a harmless
     * liveness check.
     *
     * @return the frontend status, as the wire protocol words it
     */
    static String frontendStatus() {
        VaadinService service = DevLoopRegistration.service();
        if (service == null) {
            return "unknown";
        }
        Optional<DevModeHandler> handler = DevModeHandlerManager
                .getDevModeHandler(service);
        if (handler.isEmpty()) {
            return "none";
        }
        String kind = handler.get().getClass().getSimpleName();
        int port = handler.get().getPort();
        if (port <= 0) {
            // Two different states share "no port", and they need different
            // answers: the bundle-building handler never gets one, while a dev
            // server that has not finished booting will. Conflating them would
            // tell an agent to wait for something that is never coming.
            return kind.contains("Bundle") ? "no-dev-server(" + kind + ")"
                    : "starting(" + kind + ")";
        }
        try (Socket probe = new Socket()) {
            probe.connect(new InetSocketAddress(
                    InetAddress.getLoopbackAddress(), port), 750);
            return "up:" + port;
        } catch (IOException e) {
            return "down:" + port;
        }
    }

    /**
     * What the daemon needs to know about this JVM's reload capabilities.
     *
     * @return the reply line
     */
    static String info() {
        Instrumentation inst = instrumentation();
        return "OK instrumentation=" + (inst != null) + " redefineSupported="
                + (inst != null && inst.isRedefineClassesSupported())
                + " hotswapAgent=" + hotswapAgentLoaded() + " hotswapper="
                + DevLoopRegistration.hotswapper().isPresent()
                + " enhancedRedefinition=" + enhancedRedefinition()
                + " frontend=" + frontendStatus();
    }

    private static boolean hotswapAgentLoaded() {
        try {
            Class.forName("org.hotswap.agent.HotswapAgent", false,
                    ClassLoader.getSystemClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Enhanced redefinition is a JVM feature, not a HotswapAgent one, so it is
     * detected separately - the two can be present independently.
     */
    private static boolean enhancedRedefinition() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(
                        arg -> arg.contains("AllowEnhancedClassRedefinition"));
    }

    /**
     * Whether Flow will visibly refresh anything on account of this class.
     * <p>
     * {@code onHotswap} refreshes what Flow owns: component instances and the
     * route registry. A class that is neither - a formatter, a mapper, a plain
     * helper called from a renderer - is redefined and live, and yet nothing
     * re-renders, because a Grid's cell content was rendered on the server and
     * pushed to the browser once. So the connector says which redefined classes
     * Flow will act on, and the daemon reports the rest as live but not yet
     * visible rather than simply stable.
     */
    private static boolean isUiRefreshed(Class<?> type) {
        if (Component.class.isAssignableFrom(type)) {
            return true;
        }
        // A route target is refreshed through the registry, and a layout or a
        // service init listener changes what Flow builds, so all of them make
        // onHotswap do visible work.
        return hasAnnotation(type, "com.vaadin.flow.router.Route",
                "com.vaadin.flow.router.RouteAlias",
                "com.vaadin.flow.router.Layout");
    }

    private static boolean isEntity(Class<?> type) {
        return hasAnnotation(type, "jakarta.persistence.Entity",
                "jakarta.persistence.MappedSuperclass",
                "jakarta.persistence.Embeddable");
    }

    /**
     * Whether this loaded class is a proxy generated over some other type.
     * <p>
     * This is the check that catches what {@link #isSpringBean} cannot: not
     * every Spring-managed type carries an annotation. A Spring Data repository
     * is a bare interface registered by {@code @EnableJpaRepositories}, and its
     * live bean is a JDK proxy built from that interface's members as they were
     * when the context started. Adding a method to the interface therefore adds
     * it to nothing that runs - and for a repository the method never even
     * becomes a query, since derivation happened at startup too. Asking Spring
     * would need Spring on the classpath; the generated proxy is evidence
     * enough and is already loaded in this JVM.
     */
    private static boolean isProxy(Class<?> type) {
        if (Proxy.isProxyClass(type)) {
            return true;
        }
        // Subclass proxies, which is what Spring generates for a class-based
        // bean - @Transactional and friends.
        String name = type.getName();
        return name.contains("$$SpringCGLIB$$")
                || name.contains("$$EnhancerBySpringCGLIB$$")
                || name.contains("$$EnhancerByCGLIB$$");
    }

    private static boolean isSpringBean(Class<?> type) {
        return hasAnnotation(type, "org.springframework.stereotype.Component",
                "org.springframework.stereotype.Service",
                "org.springframework.stereotype.Repository",
                "org.springframework.stereotype.Controller",
                "org.springframework.transaction.annotation.Transactional");
    }

    /**
     * Name-based so the connector needs no compile-time dependency on Spring or
     * JPA; also checks one level of meta-annotation, which is how
     * {@code @Service} and friends are composed.
     */
    private static boolean hasAnnotation(Class<?> type, String... wanted) {
        Set<String> names = new LinkedHashSet<>();
        for (java.lang.annotation.Annotation annotation : type
                .getAnnotations()) {
            names.add(annotation.annotationType().getName());
            for (java.lang.annotation.Annotation meta : annotation
                    .annotationType().getAnnotations()) {
                names.add(meta.annotationType().getName());
            }
        }
        for (String candidate : wanted) {
            if (names.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static Instrumentation instrumentation() {
        Object value = System.getProperties().get(INSTRUMENTATION_PROPERTY);
        return value instanceof Instrumentation inst ? inst : null;
    }

    /**
     * A stable fingerprint of a class's declared members, used to tell a
     * method-body change from one that alters the class's shape.
     */
    private static String members(Class<?> type) {
        List<String> signatures = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            signatures.add("m:" + method.getName()
                    + Arrays.toString(method.getParameterTypes()));
        }
        for (Field field : type.getDeclaredFields()) {
            signatures.add(
                    "f:" + field.getName() + ":" + field.getType().getName());
        }
        java.util.Collections.sort(signatures);
        return String.join(";", signatures);
    }

    private static String simple(String binaryName) {
        return binaryName.substring(binaryName.lastIndexOf('.') + 1);
    }

    private static String join(Set<String> values) {
        return values.isEmpty() ? "-" : String.join("|", values);
    }

    private static String oneLine(String value) {
        return value == null ? "null" : value.replaceAll("\\s+", " ").trim();
    }
}
