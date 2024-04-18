/*
 * Copyright 2000-2024 Vaadin Ltd.
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
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ExecutionFailedException;

import static com.vaadin.flow.server.frontend.FileIOUtils.compareIgnoringIndentationAndEOL;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate default files for react-router if missing from the frontend folder.
 * <p>
 * </p>
 * The generated files are <code>Flow.tsx</code> and <code>routes.tsx</code>.
 * Where <code>Flow.tsx</code> is for communication between the Flow and the
 * router and contains the server side route target
 * <code>serverSideRoutes</code> to be used in <code>routes.tsx</code>.
 * <p>
 * <code>Flow.tsx</code> is always written and thus updates automatically if
 * there are changes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
public class TaskGenerateReactFiles implements FallibleCommand {

    public static final String CLASS_PACKAGE = "com/vaadin/flow/server/frontend/%s";
    private Options options;
    protected static String NO_IMPORT = """
            Faulty configuration of server-side routes.
            The server route definition is missing from the '%1$s' file

            To have working Flow routes add the following to the '%1$s' file:
                import Flow from 'Frontend/generated/flow/Flow';
                import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                export const { router, routes } = new RouterConfigurationBuilder()
                    .withFallback(Flow)
                    // .withFileRoutes() or .withReactRoutes()
                    // ...
                    .build();

                OR

                import { createBrowserRouter, RouteObject } from 'react-router-dom';
                import { serverSideRoutes } from 'Frontend/generated/flow/Flow';

                function build() {
                    const routes = [...serverSideRoutes] as RouteObject[];
                    return {
                        router: createBrowserRouter(routes),
                        routes
                    };
                }
                export const { router, routes } = build();

            """;
    protected static String MISSING_ROUTES_EXPORT = """
            Routes need to be exported as 'routes' for server navigation handling.
            routes.tsx should contain
            'export const { router, routes } = new RouterConfigurationBuilder()
               // routes building
               .build();'
            OR
            'export const routes = [...serverSideRoutes] as RouteObject[];'
            """;

    private static final String FLOW_TSX = "Flow.tsx";
    private static final String REACT_ADAPTER_TSX = "ReactAdapter.tsx";
    static final String FLOW_FLOW_TSX = "flow/" + FLOW_TSX;
    static final String FLOW_REACT_ADAPTER_TSX = "flow/" + REACT_ADAPTER_TSX;
    private static final String ROUTES_JS_IMPORT_PATH_TOKEN = "%routesJsImportPath%";

    // matches setting the server-side routes from Flow.tsx:
    // import { serverSideRoutes } from "Frontend/generated/flow/Flow";
    private static final Pattern SERVER_ROUTE_PATTERN = Pattern.compile(
            "import\\s+\\{[\\s\\S]*(?:serverSideRoutes)+[\\s\\S]*\\}\\s+from\\s+(\"|'|`)Frontend\\/generated\\/flow\\/Flow(\\.js)?\\1;[\\s\\S]+\\.{3}serverSideRoutes");

    // matches setting the fallback component to RouterConfigurationBuilder,
    // e.g. Flow component from Flow.tsx:
    // import Flow from 'Frontend/generated/flow/Flow';
    // ...
    // .withFallback(Flow)
    private static final Pattern FALLBACK_COMPONENT_PATTERN = Pattern.compile(
            "import\\s+(\\w+)\\s+from\\s+(\"|'|`)Frontend\\/generated\\/flow\\/Flow(\\.js)?\\2;[\\s\\S]+withFallback\\(\\s*\\1\\s*\\)");

    private static final Pattern ROUTES_EXPORT_PATTERN = Pattern.compile(
            "export\\s+(const\\s+)?(\\{[\\s\\S]*(router[\\s\\S]*routes|routes[\\s\\S]*router)[\\s\\S]*}|routes)");

    /**
     * Create a task to generate <code>index.js</code> if necessary.
     *
     * @param options
     *            the task options
     */
    TaskGenerateReactFiles(Options options) {
        this.options = options;
    }

    @Override
    public void execute() throws ExecutionFailedException {
        if (options.isReactEnabled()) {
            doExecute();
        } else {
            cleanup();
        }
    }

    private void doExecute() throws ExecutionFailedException {
        File frontendDirectory = options.getFrontendDirectory();
        File frontendGeneratedFolder = options.getFrontendGeneratedFolder();
        File flowTsx = new File(frontendGeneratedFolder, FLOW_FLOW_TSX);
        File reactAdapterTsx = new File(frontendGeneratedFolder,
                FLOW_REACT_ADAPTER_TSX);
        File routesTsx = new File(frontendDirectory, FrontendUtils.ROUTES_TSX);
        File frontendGeneratedFolderRoutesTsx = new File(
                frontendGeneratedFolder, FrontendUtils.ROUTES_TSX);
        try {
            writeFile(flowTsx, getFlowTsxFileContent(routesTsx.exists()));
            if (fileAvailable(REACT_ADAPTER_TSX)) {
                writeFile(reactAdapterTsx, getFileContent(REACT_ADAPTER_TSX));
            }
            if (!routesTsx.exists()) {
                boolean isHillaUsed = FrontendUtils.isHillaUsed(
                        frontendDirectory, options.getClassFinder());
                writeFile(frontendGeneratedFolderRoutesTsx,
                        getFileContent(isHillaUsed ? FrontendUtils.ROUTES_TSX
                                : FrontendUtils.ROUTES_FLOW_TSX));
            } else {
                String routesContent = FileUtils.readFileToString(routesTsx,
                        UTF_8);
                routesContent = StringUtil.removeComments(routesContent);

                if (missingServerRouteImport(routesContent)
                        && serverRoutesAvailable()) {
                    throw new ExecutionFailedException(
                            String.format(NO_IMPORT, routesTsx.getPath()));
                }
                if (missingRoutesExport(routesContent)) {
                    throw new ExecutionFailedException(MISSING_ROUTES_EXPORT);
                }
            }
        } catch (IOException e) {
            throw new ExecutionFailedException("Failed to read file content",
                    e);
        }
    }

    private void cleanup() throws ExecutionFailedException {
        try {
            File frontendDirectory = options.getFrontendDirectory();
            File frontendGeneratedFolder = options.getFrontendGeneratedFolder();
            File flowTsx = new File(frontendGeneratedFolder, FLOW_FLOW_TSX);
            File reactAdapterTsx = new File(frontendGeneratedFolder,
                    FLOW_REACT_ADAPTER_TSX);
            File frontendGeneratedFolderRoutesTsx = new File(
                    frontendGeneratedFolder, FrontendUtils.ROUTES_TSX);
            FileUtils.deleteQuietly(flowTsx);
            FileUtils.deleteQuietly(reactAdapterTsx);
            FileUtils.deleteQuietly(frontendGeneratedFolderRoutesTsx);

            File routesTsx = new File(frontendDirectory,
                    FrontendUtils.ROUTES_TSX);
            if (routesTsx.exists()) {
                String defaultRoutesContent = FileUtils
                        .readFileToString(routesTsx, UTF_8);
                if (compareIgnoringIndentationAndEOL(defaultRoutesContent,
                        getFileContent(FrontendUtils.ROUTES_TSX),
                        String::equals)) {
                    routesTsx.delete();
                    log().debug("Default {} file has been removed.",
                            FrontendUtils.ROUTES_TSX);
                } else {
                    Files.copy(routesTsx.toPath(),
                            new File(frontendDirectory,
                                    FrontendUtils.ROUTES_TSX + ".flowBackup")
                                    .toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    routesTsx.delete();
                    log().warn(
                            "Custom {} file has been removed. Backup is created in {}.flowBackup file.",
                            FrontendUtils.ROUTES_TSX, FrontendUtils.ROUTES_TSX);
                }
            }
        } catch (IOException e) {
            throw new ExecutionFailedException("Failed to clean up .tsx files",
                    e);
        }
    }

    private String getFlowTsxFileContent(boolean frontendRoutesTsExists)
            throws IOException {
        return getFileContent(FLOW_TSX).replace(ROUTES_JS_IMPORT_PATH_TOKEN,
                (frontendRoutesTsExists)
                        ? FrontendUtils.FRONTEND_FOLDER_ALIAS
                                + FrontendUtils.ROUTES_JS
                        : FrontendUtils.FRONTEND_FOLDER_ALIAS
                                + FrontendUtils.GENERATED
                                + FrontendUtils.ROUTES_JS);
    }

    private boolean fileAvailable(String fileName) {
        return options.getClassFinder().getClassLoader()
                .getResource(CLASS_PACKAGE.formatted(fileName)) != null;
    }

    private boolean missingServerRouteImport(String routesContent) {
        return !FALLBACK_COMPONENT_PATTERN.matcher(routesContent).find()
                && !SERVER_ROUTE_PATTERN.matcher(routesContent).find();
    }

    private boolean serverRoutesAvailable() {
        return !options.getClassFinder().getAnnotatedClasses(Route.class)
                .isEmpty();
    }

    private static boolean missingRoutesExport(String routesContent) {
        return !ROUTES_EXPORT_PATTERN.matcher(routesContent).find();
    }

    private void writeFile(File target, String content)
            throws ExecutionFailedException {

        try {
            FileIOUtils.writeIfChanged(target, content);
        } catch (IOException exception) {
            String errorMessage = String.format("Error writing '%s'", target);
            throw new ExecutionFailedException(errorMessage, exception);
        }
    }

    protected String getFileContent(String fileName) throws IOException {
        String indexTemplate;
        try (InputStream indexTsStream = options.getClassFinder()
                .getClassLoader()
                .getResourceAsStream(CLASS_PACKAGE.formatted(fileName))) {
            indexTemplate = IOUtils.toString(indexTsStream, UTF_8);
        }
        return indexTemplate;
    }

    private Logger log() {
        return LoggerFactory.getLogger(getClass());
    }
}
