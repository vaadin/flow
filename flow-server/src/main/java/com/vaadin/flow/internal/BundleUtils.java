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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.VaadinService;

/**
 * Utility class for bundle operations.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.1
 */
public final class BundleUtils {

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
     * The key of the stats.json entry that lists the identifiers of the
     * functions the bundle registers for JavaScript definitions, event data and
     * event filters.
     */
    public static final String JS_DEFINITION_FUNCTION_IDS_STATS_KEY = "jsDefinitionFunctionIds";

    /**
     * The classes that {@link #checkJsFunctionsBundled(Class, Supplier)} has
     * checked, so that each is reported once rather than on every use.
     */
    private static final Set<String> checkedJsFunctionDeclarers = ConcurrentHashMap
            .newKeySet();

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
     * stats list the functions it registers.
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
        if (service == null
                || !service.getDeploymentConfiguration().isProductionMode()
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

    private static Logger getLogger() {
        return LoggerFactory.getLogger(BundleUtils.class);
    }

}
