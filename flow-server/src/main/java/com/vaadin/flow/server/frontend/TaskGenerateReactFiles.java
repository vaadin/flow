/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Version;

import static com.vaadin.flow.server.frontend.FileIOUtils.compareIgnoringIndentationEOLAndWhiteSpace;
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
public class TaskGenerateReactFiles
        extends AbstractFileGeneratorFallibleCommand {

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

                import { createBrowserRouter, RouteObject } from 'react-router';
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
            'export { router } = ...
            // Some other code here
            export { routes } = ...'
            OR
            'export const routes = [...serverSideRoutes] as RouteObject[];'
            """;

    private static final String FLOW_TSX = "Flow.tsx";
    private static final String VAADIN_REACT_TSX = "vaadin-react.tsx";
    private static final String REACT_ADAPTER_TEMPLATE = "ReactAdapter.template";
    private static final String REACT_ADAPTER_TSX = "ReactAdapter.tsx";
    private static final String LAYOUTS_JSON = "layouts.json";
    static final String FLOW_FLOW_TSX = "flow/" + FLOW_TSX;
    static final String FLOW_REACT_ADAPTER_TSX = "flow/" + REACT_ADAPTER_TSX;
    static final String JSX_TRANSFORM_INDEX = "jsx-dev-transform/index.ts";
    static final String JSX_TRANSFORM_RUNTIME = "jsx-dev-transform/jsx-runtime.ts";
    static final String JSX_TRANSFORM_DEV_RUNTIME = "jsx-dev-transform/jsx-dev-runtime.ts";
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

    // matches "export const { router, routes }" or "export { router, routes }"
    // or "export { router } export { routes }"
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
        File vaadinReactTsx = new File(frontendGeneratedFolder,
                VAADIN_REACT_TSX);
        File reactAdapterTsx = new File(frontendGeneratedFolder,
                FLOW_REACT_ADAPTER_TSX);
        File routesTsx = new File(frontendDirectory, FrontendUtils.ROUTES_TSX);
        File frontendGeneratedFolderRoutesTsx = new File(
                frontendGeneratedFolder, FrontendUtils.ROUTES_TSX);
        try {
            writeFile(flowTsx, getFileContent(FLOW_TSX));
            writeFile(new File(frontendGeneratedFolder, JSX_TRANSFORM_INDEX),
                    getFileContent(JSX_TRANSFORM_INDEX));
            writeFile(
                    new File(frontendGeneratedFolder,
                            JSX_TRANSFORM_DEV_RUNTIME),
                    getFileContent(JSX_TRANSFORM_DEV_RUNTIME));
            writeFile(new File(frontendGeneratedFolder, JSX_TRANSFORM_RUNTIME),
                    getFileContent(JSX_TRANSFORM_RUNTIME));
            writeFile(vaadinReactTsx,
                    getVaadinReactTsContent(routesTsx.exists()));
            writeLayoutsJson(
                    options.getClassFinder().getAnnotatedClasses(Layout.class));
            if (fileAvailable(REACT_ADAPTER_TEMPLATE)) {
                String reactAdapterContent = getFileContent(
                        REACT_ADAPTER_TEMPLATE);
                reactAdapterContent = reactAdapterContent.replace(
                        "{{VAADIN_VERSION}}", Version.getFullVersion());
                writeFile(reactAdapterTsx, reactAdapterContent);
            }

            boolean isHillaUsed = FrontendUtils.isHillaUsed(frontendDirectory,
                    options.getClassFinder());
            writeFile(frontendGeneratedFolderRoutesTsx,
                    getFileContent(isHillaUsed ? FrontendUtils.ROUTES_TSX
                            : FrontendUtils.ROUTES_FLOW_TSX));

            if (routesTsx.exists()) {
                track(routesTsx);
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

    /**
     * Writes the `layout.json` file in the frontend generated folder.
     * <p>
     * </p>
     *
     * @param options
     *            the task options
     * @param layoutsClasses
     *            {@link Layout} annotated classes.
     */
    public static void writeLayouts(Options options,
            Collection<Class<?>> layoutsClasses) {
        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);
        try {
            task.writeLayoutsJson(layoutsClasses);
        } catch (ExecutionFailedException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (e.getCause() instanceof IOException ioEx) {
                throw new UncheckedIOException(ioEx);
            }
            throw new RuntimeException(e.getCause());
        }
    }

    private void writeLayoutsJson(Collection<Class<?>> layoutClasses)
            throws ExecutionFailedException {
        writeFile(new File(options.getFrontendGeneratedFolder(), LAYOUTS_JSON),
                layoutsContent(layoutClasses));

    }

    private String layoutsContent(Collection<Class<?>> layoutClasses) {
        ArrayNode availableLayouts = JacksonUtils.createArrayNode();
        for (Class<?> layout : layoutClasses) {
            if (layout.isAnnotationPresent(Layout.class)) {
                ObjectNode layoutObject = JacksonUtils.createObjectNode();
                layoutObject.put("path",
                        layout.getAnnotation(Layout.class).value());
                availableLayouts.add(layoutObject);
            }
        }
        return availableLayouts.toString();
    }

    private void cleanup() throws ExecutionFailedException {
        try {
            File frontendDirectory = options.getFrontendDirectory();
            File frontendGeneratedFolder = options.getFrontendGeneratedFolder();
            File flowTsx = new File(frontendGeneratedFolder, FLOW_FLOW_TSX);
            File vaadinReactTsx = new File(frontendGeneratedFolder,
                    VAADIN_REACT_TSX);
            File reactAdapterTsx = new File(frontendGeneratedFolder,
                    FLOW_REACT_ADAPTER_TSX);
            File frontendGeneratedFolderRoutesTsx = new File(
                    frontendGeneratedFolder, FrontendUtils.ROUTES_TSX);
            File layoutsJson = new File(frontendGeneratedFolder, LAYOUTS_JSON);
            FileUtils.deleteQuietly(flowTsx);
            FileUtils.deleteQuietly(
                    new File(frontendGeneratedFolder, JSX_TRANSFORM_INDEX));
            FileUtils.deleteQuietly(new File(frontendGeneratedFolder,
                    JSX_TRANSFORM_DEV_RUNTIME));
            FileUtils.deleteQuietly(
                    new File(frontendGeneratedFolder, JSX_TRANSFORM_RUNTIME));
            FileUtils.deleteQuietly(layoutsJson);
            FileUtils.deleteQuietly(vaadinReactTsx);
            FileUtils.deleteQuietly(reactAdapterTsx);
            FileUtils.deleteQuietly(frontendGeneratedFolderRoutesTsx);

            File routesTsx = new File(frontendDirectory,
                    FrontendUtils.ROUTES_TSX);
            if (routesTsx.exists()) {
                String defaultRoutesContent = FileUtils
                        .readFileToString(routesTsx, UTF_8);
                if (compareIgnoringIndentationEOLAndWhiteSpace(
                        defaultRoutesContent,
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

    private String getVaadinReactTsContent(boolean frontendRoutesTsExists)
            throws IOException {
        return getFileContent(VAADIN_REACT_TSX).replace(
                ROUTES_JS_IMPORT_PATH_TOKEN,
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
            writeIfChanged(target, content);
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
