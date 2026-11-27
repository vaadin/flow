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
import java.util.regex.Pattern;

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

    private final ClassFinder classFinder;

    private final File frontendDirectory;

    private final Logger logger;

    /**
     * Creates a resolver for the project the given options describe.
     *
     * @param options
     *            the options of the build, with the class finder that finds the
     *            frontend files of jars, not <code>null</code>
     * @param logger
     *            where to warn about a path that is missing its {@code ./}
     *            prefix, not <code>null</code>
     */
    FrontendModuleResolver(Options options, Logger logger) {
        this.classFinder = options.getClassFinder();
        this.frontendDirectory = options.getFrontendDirectory();
        this.logger = logger;
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
        int queryStart = modulePath.lastIndexOf('?');
        String query = queryStart < 0 ? "" : modulePath.substring(queryStart);
        String resolved = resolveResource(
                modulePath.substring(0, modulePath.length() - query.length()));
        if (resolved.startsWith("./")) {
            return FRONTEND_FOLDER_ALIAS
                    + STARTING_DOT_SLASH.matcher(resolved).replaceFirst("")
                    + query;
        }
        return toValidBrowserImport(resolved) + query;
    }

    private boolean hasMetaInfResource(String resource) {
        return classFinder.getResource(
                RESOURCES_FRONTEND_DEFAULT + "/" + resource) != null
                || classFinder
                        .getResource(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                                + "/" + resource) != null;
    }
}
