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
import java.net.HttpURLConnection;
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

import com.vaadin.base.devserver.PublicResourcesLiveUpdater;
import com.vaadin.base.devserver.ThemeLiveUpdater;
import com.vaadin.base.devserver.ViteHandler;
import com.vaadin.base.devserver.hotswap.Hotswapper;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccessor;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.internal.ThemeUtils;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.Theme;

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

    /**
     * The directory names that make a public resource root, as whole path
     * segments so a match cannot land in the middle of one.
     * {@code META-INF/resources} is listed first only for readability - the
     * longest match from the end wins, not the earliest in this array.
     */
    private static final String[] PUBLIC_RESOURCE_ROOTS = {
            "/META-INF/resources/", "/static/", "/public/", "/resources/",
            "/webapp/" };

    /**
     * Where Vite puts the failure in the error page it serves for a module it
     * could not transform - the JSON its own overlay renders.
     */
    private static final String VITE_ERROR_MESSAGE_KEY = "\"message\":\"";

    /**
     * What the report's lines are joined with on the way back.
     * <p>
     * A reply is one line, so the newlines cannot survive as themselves - and
     * flattening them to spaces would leave the daemon unable to tell the
     * opening line from the caret diagram under it, which is what decides which
     * parts are worth quoting. A unit separator cannot occur in the text; it
     * matches {@code AppLog.SEGMENT} in the daemon, which is the only reader of
     * this field.
     */
    private static final char REPORT_SEPARATOR = '\u001f';

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
            // The class as the application has been running it, which answers
            // for an annotation the change is taking away: a type that stops
            // being an entity was still mapped by the metamodel the application
            // started with.
            classify(first, entities, beans, uiClasses);
            byte[] bytes = readClassBytes(classesDirs, name);
            if (bytes == null) {
                return "ERR kind=missing-class-file searched="
                        + classesDirs.size() + " message=" + name;
            }
            // And the class the JVM is about to be given. A type that is only
            // now being made an entity is not one yet in the loop above, and
            // Hibernate mapped neither version: the metamodel and the schema
            // were fixed at startup. Asked of the bytes rather than of the
            // class after the redefine, because the loaded class is not a
            // reliable witness to what it has just been given - see
            // declaresEntity.
            if (declaresEntity(bytes)) {
                entities.add(simple(name));
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
        Map<String, String> frontendBefore = new HashMap<>();
        for (ClassDefinition definition : definitions) {
            before.putIfAbsent(definition.getDefinitionClass().getName(),
                    members(definition.getDefinitionClass()));
            frontendBefore.putIfAbsent(
                    definition.getDefinitionClass().getName(),
                    frontendDependencies(definition.getDefinitionClass()));
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
        Set<String> frontend = new LinkedHashSet<>();
        for (ClassDefinition definition : definitions) {
            Class<?> type = definition.getDefinitionClass();
            String previous = before.get(type.getName());
            if (previous != null && !previous.equals(members(type))) {
                structural.add(simple(type.getName()));
            }
            String previousFrontend = frontendBefore.get(type.getName());
            if (previousFrontend != null
                    && !previousFrontend.equals(frontendDependencies(type))) {
                frontend.add(simple(type.getName()));
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
                + join(uiClasses) + " frontendImports=" + join(frontend)
                + " hotswapAgent=" + hotswapAgentLoaded() + " redefineMs="
                + redefineMs + " hotswapMs=" + hotswapMs;
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
     * Gets the new CSS in front of the browser, through the same call Flow's
     * own watcher makes on save.
     * <p>
     * {@code PublicResourcesLiveUpdater.push} rather than a push of its own,
     * for the reason the theme leg calls {@code ThemeLiveUpdater.push}: the
     * update has to be keyed by the URL the browser knows the stylesheet as -
     * {@code context://} prefix included, because the debug window strips that
     * prefix unconditionally before matching what is already on the page - and
     * it has to carry the stylesheet's <em>bundled</em> content, or an
     * {@code @import} and every relative {@code url(...)} resolve against the
     * wrong base. Neither is derivable from the changed file's path, which is
     * all this leg is given: a file reached through an {@code @import} is not
     * the URL the browser has, and mapping a path back to a URL by looking for
     * a {@code /static/} segment in it guesses wrong as soon as the checkout
     * has one in its own path.
     * <p>
     * Asked once for the whole change-set rather than per file, because the
     * call re-bundles every active stylesheet and so already covers all of
     * them.
     */
    private static int pushStyleSheets(String csv) {
        VaadinService service = DevLoopRegistration.service();
        if (service == null) {
            return 0;
        }
        List<File> roots = styleSheetRoots(csv);
        if (roots.isEmpty()) {
            // No stylesheet in the change-set: an image or a font is a public
            // resource too, and has nothing to push.
            return 0;
        }
        return PublicResourcesLiveUpdater.push(service.getContext(), roots);
    }

    /**
     * The public resource roots the changed stylesheets sit under.
     * <p>
     * Passed to the push because the application knows only its own roots and
     * skips anything it can merely find on the classpath - which is every
     * reactor sibling's resource, and a sibling's stylesheet reaching the open
     * page is the whole point of having the loop span the reactor. The daemon
     * sends absolute paths, so the root is derivable here and nowhere else.
     */
    private static List<File> styleSheetRoots(String csv) {
        List<File> roots = new ArrayList<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (!trimmed.endsWith(".css")) {
                continue;
            }
            File root = publicRootOf(trimmed);
            if (root != null && !roots.contains(root)) {
                roots.add(root);
            }
        }
        return roots;
    }

    /**
     * The public resource root a file sits under, or {@code null} when it sits
     * under none.
     * <p>
     * The <em>last</em> marker wins, and it has to be a whole path segment.
     * Both matter for the same reason: {@code /resources/} occurs in
     * {@code src/main/resources} as well, so taking the first occurrence maps
     * {@code src/main/resources/META-INF/resources/app.css} to
     * {@code META-INF/resources/app.css} instead of {@code app.css} - and a
     * checkout that merely has a directory named {@code static} above it would
     * do the same to every file in the project.
     */
    // Package-private so the segment and last-match rules can be asserted
    // directly; they are the part of this leg that a wrong answer makes silent.
    static File publicRootOf(String path) {
        String normalized = path.replace('\\', '/');
        int end = -1;
        for (String marker : PUBLIC_RESOURCE_ROOTS) {
            int at = normalized.lastIndexOf(marker);
            if (at >= 0 && at + marker.length() > end) {
                // Up to and including the marker directory, without its
                // trailing separator: that directory is the root.
                end = at + marker.length() - 1;
            }
        }
        return end < 0 ? null : new File(normalized.substring(0, end));
    }

    /**
     * The theme leg. Pushes each named theme's combined stylesheet into the
     * open page.
     * <p>
     * Deduplicated by theme rather than pushed per file, because the update is
     * the whole of {@code styles.css} with its imports inlined - pushing it
     * once per changed file would send the same bytes several times over.
     * <p>
     * The combining itself is {@code ThemeLiveUpdater.push}, the same call
     * Flow's own watcher makes on save. The daemon suspends that watcher and
     * then makes the call at the moment {@code apply} decides the change goes
     * live, so which of the two ran is invisible to the browser.
     *
     * @param csv
     *            the changed theme CSS paths, comma-separated
     * @return the reply line
     */
    static String theme(String csv) {
        VaadinService service = DevLoopRegistration.service();
        if (service == null) {
            return "ERR kind=no-service message=service-not-registered";
        }
        Set<Path> folders = new LinkedHashSet<>();
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                Path parent = Paths.get(trimmed).getParent();
                Path folder = themeFolderOf(parent);
                if (folder != null) {
                    folders.add(folder);
                }
            } catch (RuntimeException e) {
                return "ERR kind=protocol message=bad-path:" + trimmed;
            }
        }
        if (folders.isEmpty()) {
            return "ERR kind=protocol message=no-paths";
        }

        long started = System.nanoTime();
        int pushed = 0;
        for (Path folder : folders) {
            if (ThemeLiveUpdater.push(folder.toFile(), service.getContext())) {
                pushed++;
            }
        }
        // A theme whose stylesheet could not be combined has already asked for
        // a reload; saying so is what stops apply claiming a silent success.
        boolean reloaded = pushed == 0 && requestBrowserReload();
        return "OK themes=" + folders.size() + " pushed=" + pushed
                + " reloaded=" + reloaded + " ms="
                + (System.nanoTime() - started) / 1_000_000;
    }

    /**
     * The theme folder a file belongs to: the directory directly under
     * {@code themes/}, however deep the file itself sits.
     */
    private static Path themeFolderOf(Path directory) {
        for (Path at = directory; at != null; at = at.getParent()) {
            Path parent = at.getParent();
            if (parent != null && parent.getFileName() != null
                    && parent.getFileName().toString().equals("themes")) {
                return at;
            }
        }
        return null;
    }

    /**
     * Asks the browser to reload, for a file the server already serves from
     * disk - {@code index.html} and theme assets - where there is nothing to
     * push and nothing to rebuild, only a stale page.
     *
     * @return the reply line
     */
    static String reload() {
        return "OK reloaded=" + requestBrowserReload();
    }

    /**
     * What the daemon needs to know about this JVM's frontend, in one
     * round-trip.
     * <p>
     * {@code frontend=} keeps its exact previous meaning and position so a
     * daemon that only reads that field is unaffected. {@code mode=} is the
     * live mode rather than the one the build recorded, which is the field that
     * decides whether a frontend edit was already applied by Vite or needs the
     * bundle rebuilt.
     *
     * @param daemonFolder
     *            the frontend folder the daemon resolved, or {@code null} if it
     *            did not say
     * @return the reply line
     */
    static String frontend(String daemonFolder) {
        VaadinService service = DevLoopRegistration.service();
        String mode = service == null ? "unknown"
                : DevLoopRegistration.modeOf(service);
        return "OK frontend=" + frontendStatus() + " mode=" + mode + " themes="
                + join(activeThemes(service)) + " agree="
                + agreesOnFolder(service, daemonFolder);
    }

    /**
     * Whether the dev server can actually compile the frontend files this
     * change touched, asked by fetching each one exactly as the browser would.
     * <p>
     * The log cannot answer this. Vite compiles a module when something
     * requests it, not when {@code apply} runs, so whether an error is written
     * while the daemon is watching depends on whether a browser happened to
     * re-fetch - and one already showing the error overlay does not. The report
     * then sits in the log from an earlier window, the new window is silent,
     * and the apply reports a clean {@code Stable} over a file the page cannot
     * load. A request is the same question with a definite answer, and it
     * answers in both directions: {@code 500} while the file is broken,
     * {@code 200} once it is fixed, so no stale verdict has to be remembered.
     * <p>
     * Only a {@code 500} is a refusal. A {@code 404} means the dev server does
     * not serve that path at all - a file outside its root - which is not this
     * change being broken, and failing an apply over one would be a worse
     * answer than the truth.
     *
     * @param csv
     *            the changed files, absolute, comma-separated: they ride in as
     *            the argument rather than as a reply field because a Windows
     *            path can contain a space
     * @return the reply line, naming the first file the dev server refused
     */
    static String frontendCheck(String csv) {
        VaadinService service = DevLoopRegistration.service();
        if (service == null) {
            return "ERR kind=no-service message=service-not-registered";
        }
        ViteHandler vite = viteHandler(service);
        if (vite == null) {
            // A dev bundle was built before the app started, so there is no
            // dev server to ask and nothing compiles on demand.
            return "OK checked=0 refused=0";
        }
        Path root = frontendRoot(service);
        if (root == null) {
            return "OK checked=0 refused=0";
        }
        int checked = 0;
        for (String value : csv.split(",")) {
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String relative = relativeName(root, trimmed);
            if (relative == null) {
                continue;
            }
            checked++;
            String refusal = refusalFor(vite,
                    vite.getPathToVaadin() + "/" + relative);
            if (refusal != null) {
                return "OK checked=" + checked + " refused=1 file="
                        + oneLine(relative) + " message=" + oneLine(refusal);
            }
        }
        return "OK checked=" + checked + " refused=0";
    }

    private static Path frontendRoot(VaadinService service) {
        try {
            return service.getDeploymentConfiguration().getFrontendFolder()
                    .toPath().toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * The file's path under the frontend folder, with forward slashes, or
     * {@code null} for one that does not live there - the dev server's root is
     * that folder, so nothing else has a URL on it.
     */
    private static String relativeName(Path root, String file) {
        try {
            Path path = Paths.get(file).toAbsolutePath().normalize();
            if (!path.startsWith(root) || path.equals(root)) {
                return null;
            }
            return root.relativize(path).toString().replace(File.separatorChar,
                    '/');
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * What the dev server said about one module, or {@code null} if it served
     * it. A connection that cannot be made is not a refusal either: a dev
     * server that is unreachable is what {@link #frontendStatus()} answers, and
     * failing an apply here would report the same thing twice.
     */
    private static String refusalFor(ViteHandler vite, String url) {
        try {
            HttpURLConnection connection = vite.prepareConnection(url, "GET");
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_INTERNAL_ERROR) {
                return null;
            }
            return viteErrorMessage(errorBody(connection), url);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static String errorBody(HttpURLConnection connection) {
        try (java.io.InputStream in = connection.getErrorStream()) {
            return in == null ? ""
                    : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * The diagnosis out of the dev server's error page.
     * <p>
     * Vite answers a module it could not transform with an HTML page carrying
     * the failure as a JSON object for its own overlay to render, so the
     * {@code message} field in it is the same text Vite logs. Read best-effort
     * and never load-bearing: the refusal is the verdict, and a page whose
     * shape has moved still fails the apply - it just says so less precisely.
     */
    private static String viteErrorMessage(String body, String url) {
        int at = body.indexOf(VITE_ERROR_MESSAGE_KEY);
        if (at < 0) {
            return "the dev server could not compile " + url;
        }
        StringBuilder text = new StringBuilder();
        for (int i = at + VITE_ERROR_MESSAGE_KEY.length(); i < body
                .length(); i++) {
            char c = body.charAt(i);
            if (c == '\\' && i + 1 < body.length()) {
                // A line break becomes the separator the daemon splits the
                // report on; a tab is only ever indentation inside one.
                char next = body.charAt(++i);
                if (next == 'n') {
                    text.append(REPORT_SEPARATOR);
                } else {
                    text.append(next == 't' ? ' ' : next);
                }
            } else if (c == '"') {
                break;
            } else {
                text.append(c);
            }
        }
        String message = text.toString().trim();
        return message.isEmpty() ? "the dev server could not compile " + url
                : message;
    }

    /** The dev server, or {@code null} when the app runs off a dev bundle. */
    private static ViteHandler viteHandler(VaadinService service) {
        return DevModeHandlerManager.getDevModeHandler(service)
                .filter(ViteHandler.class::isInstance)
                .map(ViteHandler.class::cast).orElse(null);
    }

    private static Set<String> activeThemes(VaadinService service) {
        if (service == null) {
            return Set.of();
        }
        try {
            return new LinkedHashSet<>(
                    ThemeUtils.getActiveThemes(service.getContext()));
        } catch (RuntimeException e) {
            return Set.of();
        }
    }

    /**
     * Whether the daemon and the app mean the same directory. A mismatch is not
     * an error here - the daemon logs it and carries on with its own answer -
     * but it is the first thing to look at when an edit is not seen.
     */
    private static String agreesOnFolder(VaadinService service,
            String daemonFolder) {
        if (service == null || daemonFolder == null || daemonFolder.isEmpty()) {
            return "?";
        }
        try {
            Path app = service.getDeploymentConfiguration().getFrontendFolder()
                    .toPath().toAbsolutePath().normalize();
            return String.valueOf(app.equals(
                    Paths.get(daemonFolder).toAbsolutePath().normalize()));
        } catch (RuntimeException e) {
            return "?";
        }
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

    /**
     * Sorts one class into the sets the daemon decides on, by simple name.
     * <p>
     * Asked of the class the application is running, before the redefine
     * replaces it. What the new bytes add is a separate question, and
     * {@link #declaresEntity} is where it is asked.
     */
    private static void classify(Class<?> type, Set<String> entities,
            Set<String> beans, Set<String> uiClasses) {
        if (isEntity(type)) {
            entities.add(simple(type.getName()));
        }
        if (isSpringBean(type)) {
            beans.add(simple(type.getName()));
        }
        if (isUiRefreshed(type)) {
            uiClasses.add(simple(type.getName()));
        }
    }

    private static boolean isEntity(Class<?> type) {
        return hasAnnotation(type, "jakarta.persistence.Entity",
                "jakarta.persistence.MappedSuperclass",
                "jakarta.persistence.Embeddable");
    }

    /** The same annotations, as they are spelled in a class file. */
    private static final List<String> ENTITY_DESCRIPTORS = List.of(
            "Ljakarta/persistence/Entity;",
            "Ljakarta/persistence/MappedSuperclass;",
            "Ljakarta/persistence/Embeddable;");

    /**
     * Whether the compiled bytes carry a JPA annotation, read from the class
     * file rather than from the class once it is loaded.
     * <p>
     * The loaded class cannot answer this. {@link #isEntity} reports what the
     * application has been running with, which is the right question for an
     * annotation being taken away and the wrong one for an annotation being
     * added. Asking it again after the redefine looks like the fix and is not:
     * on a JVM with enhanced class redefinition the class is replaced rather
     * than edited in place, and the reflective view of it - annotations
     * included - is refreshed by HotswapAgent's own cache clearing, which is
     * not ordered against this reply. Measured here, that made the answer
     * depend on the timing of another thread. The bytes are what the JVM was
     * handed, they say the same thing on every JVM, and they are already in
     * hand.
     * <p>
     * A descriptor in the constant pool is not proof that the annotation is on
     * the class - it could sit on a member, or be a type the class merely
     * mentions - so this over-reports rather than under-reports. That is the
     * right way round: the cost of a false positive is a restart that was not
     * needed, and the cost of a false negative is {@code Stable} over a mapping
     * the application never had.
     */
    private static boolean declaresEntity(byte[] bytes) {
        // ISO-8859-1 maps every byte to the char of the same value, so a
        // substring search over it is an exact byte search - and a descriptor
        // is ASCII, which the class file's modified UTF-8 encodes unchanged.
        String constants = new String(bytes, StandardCharsets.ISO_8859_1);
        return ENTITY_DESCRIPTORS.stream().anyMatch(constants::contains);
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

    /**
     * The frontend imports a class declares, as one comparable string.
     * <p>
     * These annotations are read by the build, not at runtime: {@code JsModule}
     * and friends end up in {@code generated-flow-imports.js}, which
     * {@code TaskUpdateImports} writes during startup, and the browser reaches
     * them through a bundle chunk keyed by class name. So adding one to a
     * running application cannot work however cleanly the class redefines - the
     * chunk the client would load does not contain the new import. The daemon
     * escalates to a restart on this, and the restart regenerates the imports
     * and rebuilds the bundle.
     * <p>
     * Read through {@link AnnotationReader} rather than off the class directly,
     * so an import inherited from a superclass or picked up through
     * {@code @Uses} counts the same way the build counts it. Reflection is
     * re-read after a redefine - {@code Class} discards its cached annotation
     * data when {@code classRedefinedCount} moves - so calling this before and
     * after is a real comparison.
     * <p>
     * {@code @StyleSheet} is deliberately absent: those are live already, added
     * and removed by {@code StyleSheetHotswapper} without a rebuild, and
     * restarting for one would be a regression.
     */
    // Package-private so the non-Component reads can be asserted directly.
    static String frontendDependencies(Class<?> type) {
        List<String> imports = new ArrayList<>();
        if (Component.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends Component> componentType = (Class<? extends Component>) type;
            AnnotationReader.getJsModuleAnnotations(componentType).forEach(
                    annotation -> imports.add("js:" + annotation.value()));
            AnnotationReader.getJavaScriptAnnotations(componentType).forEach(
                    annotation -> imports.add("script:" + annotation.value()));
            AnnotationReader.getCssImportAnnotations(componentType).forEach(
                    annotation -> imports.add("css:" + annotation.value() + ":"
                            + annotation.id() + ":" + annotation.themeFor()));
        } else {
            // Straight off the class, because AnnotationReader only accepts a
            // Component - and none of these annotations is Component-only. The
            // build scans every class it reaches from an entry point, so a
            // JsModule on a service init listener ends up in
            // generated-flow-imports.js exactly like one on a view.
            for (JsModule annotation : type
                    .getAnnotationsByType(JsModule.class)) {
                imports.add("js:" + annotation.value());
            }
            for (JavaScript annotation : type
                    .getAnnotationsByType(JavaScript.class)) {
                imports.add("script:" + annotation.value());
            }
            for (CssImport annotation : type
                    .getAnnotationsByType(CssImport.class)) {
                imports.add("css:" + annotation.value() + ":" + annotation.id()
                        + ":" + annotation.themeFor());
            }
        }
        // These two are read off the class whatever it is. @Theme in particular
        // has to sit on the AppShellConfigurator, which is never a Component -
        // so reading it only from Components meant a theme could be added,
        // changed or removed, redefine cleanly, and be reported Stable over a
        // bundle that has no imports for it.
        for (NpmPackage npmPackage : type
                .getAnnotationsByType(NpmPackage.class)) {
            imports.add(
                    "npm:" + npmPackage.value() + "@" + npmPackage.version());
        }
        Theme theme = type.getAnnotation(Theme.class);
        if (theme != null) {
            // Variant and theme class alongside the name: all three are read
            // while the application starts, so a change to any of them is the
            // same kind of change.
            imports.add("theme:" + theme.value() + ":" + theme.variant() + ":"
                    + theme.themeClass().getName());
        }
        java.util.Collections.sort(imports);
        return String.join(";", imports);
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
