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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

/**
 * Utility class for bundle operations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.1
 */
public final class BundleUtils {

    /**
     * The key of the stats.json entry that lists the identifiers of the
     * functions the bundle registers for JavaScript definitions, event data and
     * event filters.
     */
    public static final String JS_DEFINITION_FUNCTION_IDS_STATS_KEY = "jsDefinitionFunctionIds";

    /**
     * The key of the stats.json entry that maps the identifier of each function
     * the bundle leaves out, because a module its JavaScript imports could not
     * be found when the bundle was built, to that module as it is declared.
     */
    public static final String JS_DEFINITION_UNRESOLVED_IMPORTS_STATS_KEY = "jsDefinitionUnresolvedImports";

    /**
     * The key of the same map in
     * {@link FrontendUtils#JS_DEFINITION_IDS_FILE_NAME}, which a development
     * server reads, as it is written again while the application runs.
     */
    public static final String JS_DEFINITION_UNRESOLVED_IMPORTS_KEY = "unresolvedImports";

    /**
     * The classes that {@link #checkJsFunctionsBundled(Class, Supplier)} has
     * checked, so that each is reported once rather than on every use.
     */
    private static final Set<String> checkedJsFunctionDeclarers = ConcurrentHashMap
            .newKeySet();

    private BundleUtils() {
        // Util methods only
    }

    /**
     * Cached stats.json content to avoid repeated file I/O and parsing.
     * Volatile ensures visibility across threads. The stats.json is a classpath
     * resource that cannot change at runtime, making it safe to cache
     * indefinitely.
     */
    private static volatile ObjectNode cachedStatsJson = null;

    /**
     * The functions the production bundle leaves out, with the stats they were
     * read from, which can not change at runtime.
     */
    private record BundleUnresolvedImports(JsonNode stats,
            Map<String, String> unresolvedImports) {
    }

    private static volatile BundleUnresolvedImports cachedBundleUnresolvedImports = null;

    /**
     * The functions the development build leaves out, as read from the file
     * that lists them, with what tells whether the file was written since.
     */
    private record UnresolvedImportsFile(File file, FileTime modified,
            long size, Map<String, String> unresolvedImports) {
    }

    private static volatile UnresolvedImportsFile cachedUnresolvedImportsFile = null;

    /**
     * Loads stats.json from the classpath (from the production bundle) and
     * returns the "bundleImports" part of it.
     *
     * @return the bundle imports as a set
     */
    public static Set<String> loadBundleImports() {
        JsonNode statsJson = loadStatsJson();
        if (!statsJson.has("bundleImports")) {
            return null;
        }

        Set<String> bundledImports = new HashSet<>();
        JsonNode bundleImports = statsJson.get("bundleImports");
        for (int i = 0; i < bundleImports.size(); i++) {
            String jsImport = bundleImports.get(i).textValue();
            bundledImports.add(jsImport);
            bundledImports.add(jsImport.replace("/theme/lumo/", "/src/"));
            bundledImports.add(jsImport.replace("/theme/material/", "/src/"));
            bundledImports.add(jsImport.replaceFirst(
                    "^Frontend/generated/jar-resources/theme/(lumo|material)/",
                    "./src/"));
            bundledImports.add(jsImport
                    .replaceFirst("^Frontend/generated/jar-resources/", "./"));
            bundledImports.add(jsImport
                    .replaceFirst("^Frontend/generated/jar-resources/", ""));
            bundledImports.add(jsImport.replaceFirst("^Frontend/", "./"));
            bundledImports.add(jsImport.replaceFirst("^Frontend/", ""));
        }
        return Collections.unmodifiableSet(bundledImports);
    }

    /**
     * Loads stats.json from the classpath (from the production bundle). The
     * result is cached after first load since stats.json is immutable at
     * runtime.
     *
     * @return the stats json as a json object
     */
    static ObjectNode loadStatsJson() {
        ObjectNode cached = cachedStatsJson;
        if (cached != null) {
            return cached;
        }

        try (InputStream stats = BundleUtils.class.getClassLoader()
                .getResourceAsStream("META-INF/VAADIN/config/stats.json")) {
            if (stats == null) {
                cached = JacksonUtils.createObjectNode();
            } else {
                try {
                    cached = JacksonUtils
                            .readTree(StringUtil.toUTF8String(stats));
                } catch (IOException e) {
                    getLogger().warn(
                            "Unable to parse META-INF/VAADIN/config/stats.json",
                            e);
                    cached = JacksonUtils.createObjectNode();
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        cachedStatsJson = cached;
        return cached;
    }

    /**
     * Gets the id for a chunk defined by the given class.
     *
     * @param componentClass
     *            the class
     * @return an identifier for the component class that does not reveal the
     *         name of the component class
     */
    public static String getChunkId(Class<? extends Component> componentClass) {
        return getChunkId(componentClass.getName());
    }

    /**
     * Gets the id for a chunk defined by the given class.
     *
     * @param className
     *            the name of the class
     * @return an identifier for the component class that does not reveal the
     *         name of the component class
     */
    public static String getChunkId(String className) {
        return StringUtil.getHash(className, StandardCharsets.UTF_8);
    }

    /**
     * Calculates a hash for bundle JavaScript chunk containing given string
     * lines.
     *
     * @param chunkLines
     *            content of the chunk, collection of string lines
     * @return chunk's hash
     */
    public static String getChunkHash(List<String> chunkLines) {
        List<String> sortedChunkLines = new ArrayList<>(chunkLines);
        Collections.sort(sortedChunkLines);
        return StringUtil.getHash(String.join(";", sortedChunkLines),
                StandardCharsets.UTF_8);
    }

    /**
     * Returns whether the application uses pre-compiled production bundle or a
     * custom bundle.
     *
     * @return <code>true</code> in case of pre-compiled bundle,
     *         <code>false</code> otherwise
     */
    public static boolean isPreCompiledProductionBundle() {
        JsonNode stats = loadStatsJson();
        return stats.has("pre-compiled");
    }

    /**
     * Logs an error when the production bundle does not carry a function that
     * the given class declares JavaScript for, the way a missing
     * {@code @JsModule} of a component is reported. The browser can not run
     * such a function, and in production mode it does not log why, so the
     * server says which class is missing and how to get it into the bundle.
     * <p>
     * Checks a class once, and only in production mode with a bundle whose
     * stats list the functions it registers. A function that the bundle leaves
     * out because a module it imports could not be found is not reported here:
     * the class is reached, and calling the function says what is missing.
     * <p>
     * Calls that a {@code @JsStringFallback} interface stands in for are not
     * checked: the build always keeps those interfaces in the bundle, as they
     * are found by the string an add-on sends rather than through a class
     * reference, so naming them in {@code @Uses} would not help.
     *
     * @param declaringType
     *            the JavaScript definition or event type that declares the
     *            functions, not <code>null</code>
     * @param functionIds
     *            the identifiers of the functions that the class declares,
     *            asked for only when the class is checked, not
     *            <code>null</code>
     */
    public static void checkJsFunctionsBundled(Class<?> declaringType,
            Supplier<Collection<String>> functionIds) {
        VaadinService service = VaadinService.getCurrent();
        // A service mocked for a test may have no configuration
        DeploymentConfiguration configuration = service == null ? null
                : service.getDeploymentConfiguration();
        if (configuration == null || !configuration.isProductionMode()
                || !checkedJsFunctionDeclarers.add(declaringType.getName())) {
            return;
        }
        JsonNode bundled = loadStatsJson()
                .get(JS_DEFINITION_FUNCTION_IDS_STATS_KEY);
        if (bundled == null || !bundled.isArray()) {
            // Built before the stats listed the functions, so there is nothing
            // to tell a missing function by
            return;
        }
        Set<String> bundledIds = new HashSet<>();
        bundled.forEach(id -> bundledIds.add(id.asString()));
        // A function left out because a module it imports could not be
        // found is not missing for want of a reference to the class, and
        // calling it says why on its own
        bundledIds.addAll(getBundleUnresolvedImports().keySet());
        if (!bundledIds.containsAll(functionIds.get())) {
            String name = declaringType.getSimpleName();
            getLogger().error("{} declares JavaScript that was not included"
                    + " when creating the production bundle, so using it"
                    + " fails in the browser. Check that a class the"
                    + " application uses refers to {} and that it is not used"
                    + " only through reflection. If needed add a @Uses({}.class)"
                    + " to a class that the application uses.",
                    declaringType.getName(), name, name);
        }
    }

    /**
     * Gets the functions that the frontend build left out because a module that
     * their JavaScript imports with {@code @JsDefinitionImport} could not be
     * found, so that calling one of them can fail on the server with the reason
     * rather than in the browser.
     * <p>
     * In production mode they are read from the stats of the bundle. In
     * development mode they are read from the file the build writes next to the
     * generated JavaScript, which is written again when a JavaScript definition
     * is hotswapped, so the answer follows it.
     *
     * @return the module as it is declared by the identifier of each function
     *         that was left out, empty when nothing was or when there is no
     *         running service to tell the mode by
     */
    public static Map<String, String> getUnresolvedJsImports() {
        VaadinService service = VaadinService.getCurrent();
        // A service mocked for a test may have no configuration
        DeploymentConfiguration configuration = service == null ? null
                : service.getDeploymentConfiguration();
        if (configuration == null) {
            return Map.of();
        }
        if (configuration.isProductionMode()) {
            return getBundleUnresolvedImports();
        }
        File frontendFolder = configuration.getFrontendFolder();
        if (frontendFolder == null) {
            return Map.of();
        }
        return readUnresolvedImports(new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITION_IDS_FILE_NAME));
    }

    private static Map<String, String> getBundleUnresolvedImports() {
        JsonNode stats = loadStatsJson();
        BundleUnresolvedImports cached = cachedBundleUnresolvedImports;
        if (cached == null || cached.stats() != stats) {
            // A bundle built before the stats listed them has none to tell
            cached = new BundleUnresolvedImports(stats,
                    parseUnresolvedJsImports(stats
                            .get(JS_DEFINITION_UNRESOLVED_IMPORTS_STATS_KEY)));
            cachedBundleUnresolvedImports = cached;
        }
        return cached.unresolvedImports();
    }

    private static Map<String, String> readUnresolvedImports(File file) {
        UnresolvedImportsFile cached = cachedUnresolvedImportsFile;
        try {
            if (!file.isFile()) {
                return Map.of();
            }
            FileTime modified = Files.getLastModifiedTime(file.toPath());
            long size = Files.size(file.toPath());
            if (cached != null && cached.file().equals(file)
                    && cached.modified().equals(modified)
                    && cached.size() == size) {
                return cached.unresolvedImports();
            }
            Map<String, String> unresolvedImports = parseUnresolvedJsImports(
                    JacksonUtils
                            .readTree(Files.readString(file.toPath(),
                                    StandardCharsets.UTF_8))
                            .get(JS_DEFINITION_UNRESOLVED_IMPORTS_KEY));
            cachedUnresolvedImportsFile = new UnresolvedImportsFile(file,
                    modified, size, unresolvedImports);
            return unresolvedImports;
        } catch (IOException | RuntimeException e) {
            // Being written while it is read, or written by another version:
            // a call then fails in the browser rather than on the server
            getLogger().debug("Could not read {}", file, e);
            return Map.of();
        }
    }

    /**
     * Reads the functions left out of a bundle because a module they import
     * could not be found, as the stats or the file next to the generated
     * JavaScript list them.
     *
     * @param node
     *            the value of
     *            {@link #JS_DEFINITION_UNRESOLVED_IMPORTS_STATS_KEY} or
     *            {@link #JS_DEFINITION_UNRESOLVED_IMPORTS_KEY}, or
     *            <code>null</code> when there is none
     * @return the module as it is declared by the identifier of each function,
     *         empty when the value lists none or is not an object
     */
    public static Map<String, String> parseUnresolvedJsImports(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> unresolvedImports = new HashMap<>();
        node.properties().forEach(entry -> unresolvedImports.put(entry.getKey(),
                entry.getValue().asString()));
        return Map.copyOf(unresolvedImports);
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleUtils.class);
    }

}
