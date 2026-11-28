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
package com.vaadin.flow.server.frontend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import static com.vaadin.flow.internal.FrontendUtils.FRONTEND_FOLDER_ALIAS;
import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;

/**
 * Resolves the path of a JavaScript or CSS module, as an annotation such as
 * {@code @JsModule} declares it, to the path the generated frontend files
 * import it by: a file of the project frontend folder, a frontend file of a
 * jar, which the build copies into the frontend folder, or an npm package.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class FrontendModuleResolver {

    private static final Pattern STARTING_DOT_SLASH = Pattern.compile("^\\./+");

    // What Vite tries after the path as it is, by default
    private static final List<String> VITE_EXTENSIONS = List.of(".mjs", ".js",
            ".mts", ".ts", ".jsx", ".tsx", ".json");

    // What vite.generated.ts maps to a folder rather than to a package
    private static final List<String> VITE_ALIASES = List
            .of(FRONTEND_FOLDER_ALIAS, "@vaadin/flow-frontend/");

    private final ClassFinder classFinder;

    private final File frontendDirectory;

    private final Logger logger;

    private final Supplier<@Nullable Set<String>> declaredPackages;

    private @Nullable Set<String> packages;

    private boolean packagesFound;

    /**
     * Creates a resolver for the project the given options describe, which
     * takes any npm package to be there.
     *
     * @param options
     *            the options of the build, with the class finder that finds the
     *            frontend files of jars, not <code>null</code>
     * @param logger
     *            where to warn about a path that is missing its {@code ./}
     *            prefix, not <code>null</code>
     */
    FrontendModuleResolver(Options options, Logger logger) {
        this(options, logger, () -> null);
    }

    /**
     * Creates a resolver for the project the given options describe.
     *
     * @param options
     *            the options of the build, with the class finder that finds the
     *            frontend files of jars, not <code>null</code>
     * @param logger
     *            where to warn about a path that is missing its {@code ./}
     *            prefix, not <code>null</code>
     * @param declaredPackages
     *            the names of the npm packages the project depends on, asked
     *            for once and only when a module is checked that is an npm
     *            package; answers <code>null</code> when they can not be told,
     *            which takes any package to be there
     */
    FrontendModuleResolver(Options options, Logger logger,
            Supplier<@Nullable Set<String>> declaredPackages) {
        this.classFinder = options.getClassFinder();
        this.frontendDirectory = options.getFrontendDirectory();
        this.logger = logger;
        this.declaredPackages = declaredPackages;
    }

    /**
     * Resolves a path that names a frontend file of a jar to where the build
     * copies that file, relative to the frontend folder. Any other path is
     * returned as it is.
     *
     * @param importPath
     *            the path as it is declared, not <code>null</code>
     * @return the path of the copied file relative to the frontend folder, or
     *         the given path
     */
    String resolveResource(String importPath) {
        String resolved = importPath;
        if (!importPath.startsWith("@")) {

            // We only should check here those paths starting with './' when all
            // flow components have the './' prefix
            String resource = STARTING_DOT_SLASH.matcher(resolved)
                    .replaceFirst("");
            if (hasMetaInfResource(resource)) {
                if (!resolved.startsWith("./")) {
                    logger.warn(
                            "Use the './' prefix for files in JAR files: '{}', please update your component.",
                            importPath);
                }
                resolved = FrontendUtils.JAR_RESOURCES_IMPORT_FRONTEND_RELATIVE
                        + resource;
            }
        }
        return resolved;
    }

    /**
     * Turns a path of a file or folder in the frontend folder into one that
     * starts with the {@value FrontendUtils#FRONTEND_FOLDER_ALIAS} alias, which
     * a generated file can import it by wherever it is. Any other path is
     * returned as it is.
     *
     * @param jsImport
     *            the path to turn into an import, not <code>null</code>
     * @return the path with the alias, or the given path
     */
    String toValidBrowserImport(String jsImport) {
        File file = new File(frontendDirectory, jsImport);
        if (file.isFile() || file.isDirectory()) {
            if (!jsImport.startsWith("./")) {
                logger.warn(
                        "Use the './' prefix for files in the '{}' folder: '{}', please update your annotations.",
                        frontendDirectory, jsImport);
            }
            return FRONTEND_FOLDER_ALIAS
                    + STARTING_DOT_SLASH.matcher(jsImport).replaceFirst("");
        }
        return jsImport;
    }

    /**
     * Resolves the path of a module to the path a generated file imports it by,
     * wherever that file is: a file of the frontend folder or of a jar, which
     * is named with a path starting with {@code ./}, is imported through the
     * {@value FrontendUtils#FRONTEND_FOLDER_ALIAS} alias, and an npm package as
     * it is. Unlike {@link #toValidBrowserImport(String)}, a path starting with
     * {@code ./} is resolved even before the file is there, since the frontend
     * files of jars are copied after some files that import them are generated.
     * A query, such as {@code ?inline}, is kept on the resolved path, but not
     * used to find the file.
     *
     * @param modulePath
     *            the path as it is declared, not <code>null</code>
     * @return the path to import the module by, not <code>null</code>
     */
    String resolveImport(String modulePath) {
        String path = stripQuery(modulePath);
        String query = modulePath.substring(path.length());
        String resolved = resolveResource(path);
        if (resolved.startsWith("./")) {
            return FRONTEND_FOLDER_ALIAS
                    + STARTING_DOT_SLASH.matcher(resolved).replaceFirst("")
                    + query;
        }
        return toValidBrowserImport(resolved) + query;
    }

    /**
     * Tells why the module of the given path can not be found, before anything
     * is installed or copied, so that a build can leave out what imports it
     * rather than fail in the frontend build, which does not say what declared
     * the import.
     * <p>
     * A path starting with {@code ./} names a frontend file of a jar, which the
     * path has to name as it is, or a file of the frontend folder, which is
     * looked for as Vite looks for it: as it is, with one of the extensions
     * Vite tries, and as an index file of a folder of that name. A query, such
     * as {@code ?inline}, is not part of the name of the file. Any other path
     * that is not a file of the frontend folder or of a jar names an npm
     * package, which has to be one that the project depends on: npm installs
     * packages after this is asked, so what is installed is not what counts.
     * Anything else, such as an alias of the frontend folder, a subpath import
     * starting with {@code #} or a relative path that leaves the frontend
     * folder, is taken to resolve, as is any package when the packages the
     * project depends on can not be told.
     *
     * @param modulePath
     *            the path as it is declared, not <code>null</code>
     * @return why the module can not be found, or <code>null</code> when it can
     *         be found or that can not be told
     */
    @Nullable
    String findUnresolvableReason(String modulePath) {
        String path = stripQuery(modulePath);
        if (path.startsWith("./")) {
            String resource = STARTING_DOT_SLASH.matcher(path).replaceFirst("");
            // A frontend file of a jar is imported from where the build
            // copies it only when the path names the file as it is, see
            // resolveResource, so Vite tries nothing else for it
            if (hasMetaInfResource(resource) || findCandidates(resource)
                    .stream().anyMatch(this::isFrontendFolderFile)) {
                return null;
            }
            return "no file '" + resource + "' is in "
                    + RESOURCES_FRONTEND_DEFAULT + " or "
                    + COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                    + " of the class path, and neither it nor a file with one of the extensions "
                    + String.join(", ", VITE_EXTENSIONS)
                    + " or an index file of a folder of that name is in the frontend folder '"
                    + frontendDirectory + "'";
        }
        // A path relative to somewhere else, a URL, a module with a scheme
        // such as virtual: or a subpath import starting with # is resolved
        // by Vite, a plugin or package.json, which can not be told from here
        if (path.startsWith(".") || path.startsWith("/") || path.startsWith("#")
                || path.contains(":")
                || VITE_ALIASES.stream().anyMatch(path::startsWith)
                || isFrontendFolderFile(path) || hasMetaInfResource(path)) {
            return null;
        }
        String packageName = getPackageName(path);
        Set<String> declared = getDeclaredPackages();
        if (declared == null || declared.contains(packageName)) {
            return null;
        }
        return "the npm package '" + packageName
                + "' is not a dependency of the project. Add it with @NpmPackage"
                + " or to the dependencies in package.json";
    }

    private @Nullable Set<String> getDeclaredPackages() {
        if (!packagesFound) {
            packages = declaredPackages.get();
            packagesFound = true;
        }
        return packages;
    }

    private static String getPackageName(String path) {
        String[] segments = path.split("/", 3);
        return path.startsWith("@") && segments.length > 1
                ? segments[0] + "/" + segments[1]
                : segments[0];
    }

    private static List<String> findCandidates(String resource) {
        List<String> candidates = new ArrayList<>();
        candidates.add(resource);
        VITE_EXTENSIONS
                .forEach(extension -> candidates.add(resource + extension));
        String folder = resource.isEmpty() || resource.endsWith("/") ? resource
                : resource + "/";
        VITE_EXTENSIONS.forEach(
                extension -> candidates.add(folder + "index" + extension));
        return candidates;
    }

    private boolean isFrontendFolderFile(String resource) {
        return !resource.isEmpty()
                && new File(frontendDirectory, resource).isFile();
    }

    /**
     * Gets the given import path without its query, such as {@code ?inline},
     * which is not part of the name of the file.
     *
     * @param importPath
     *            the path, not <code>null</code>
     * @return the path up to its first {@code ?}, or the path when it has no
     *         query
     */
    static String stripQuery(String importPath) {
        int queryStart = importPath.indexOf('?');
        return queryStart < 0 ? importPath
                : importPath.substring(0, queryStart);
    }

    private boolean hasMetaInfResource(String resource) {
        return classFinder.getResource(
                RESOURCES_FRONTEND_DEFAULT + "/" + resource) != null
                || classFinder
                        .getResource(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                                + "/" + resource) != null;
    }
}
