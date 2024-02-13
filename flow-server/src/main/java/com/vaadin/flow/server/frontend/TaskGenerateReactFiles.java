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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.Version;
import com.vaadin.open.App;

import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_JS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TS;
import static com.vaadin.flow.server.frontend.FrontendUtils.INDEX_TSX;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Generate default files for react-router if missing from the frontend folder.
 * <p>
 * </p>
 * The generated files are <code>App.tsx</code>, <code>Flow.tsx</code> and
 * <code>routes.tsx</code>. Where <code>Flow.tsx</code> is for communication
 * between the Flow and the router and contains the server side route target
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

    private Options options;
    protected static String NO_IMPORT = """
            Faulty configuration of serverSideRoutes.
            The server route definition is missing from the '%1$s' file

            To have working Flow routes add the following to the '%1$s' file:
            - import { serverSideRoutes } from "Frontend/generated/flow/Flow";
            - route '...serverSideRoutes' into the routes definition as shown below:

                export const routes = [
                  {
                    element: <MainLayout />,
                    handle: { title: 'Main' },
                    children: [
                      { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World' } },
                      ...serverSideRoutes
                    ],
                  },
                ] as RouteObject[];
            """;
    protected static String MISSING_ROUTES_EXPORT = """
            Routes need to be exported as 'routes' for server navigation handling.
            routes.tsx should at least contain
            'export const routes = [...serverSideRoutes] as RouteObject[];'
            but can have react routes also defined.
            """;

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
        File frontendDirectory = options.getFrontendDirectory();
        File appTsx = new File(frontendDirectory, "App.tsx");
        File flowTsx = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                "flow/Flow.tsx");
        File reactAdapterTsx = new File(
                new File(frontendDirectory, FrontendUtils.GENERATED),
                "flow/ReactAdapter.tsx");
        File routesTsx = new File(frontendDirectory, "routes.tsx");
        try {
            writeFile(flowTsx, getFileContent("Flow.tsx"));
            writeFile(reactAdapterTsx, getFileContent("ReactAdapter.tsx"));
            if (!appTsx.exists()) {
                writeFile(appTsx, getFileContent("App.tsx"));
            }

            if (!routesTsx.exists()) {
                writeFile(routesTsx, getFileContent("routes.tsx"));
            } else {
                String routesContent = FileUtils.readFileToString(routesTsx,
                        UTF_8);
                if (missingServerImport(routesContent)
                        && serverRoutesAvailable()) {
                    throw new ExecutionFailedException(
                            String.format(NO_IMPORT, routesTsx.getPath()));
                }
                if (!routesContent.contains("export const routes")) {
                    throw new ExecutionFailedException(MISSING_ROUTES_EXPORT);
                }
            }
        } catch (IOException e) {
            throw new ExecutionFailedException("Failed to read file content",
                    e);
        }
    }

    private boolean missingServerImport(String routesContent) {
        Pattern serverImport = Pattern.compile(
                "import[\\s\\S]?\\{[\\s\\S]?serverSideRoutes[\\s\\S]?\\}[\\s\\S]?from[\\s\\S]?(\"|'|`)Frontend\\/generated\\/flow\\/Flow\\1;");
        return !serverImport.matcher(routesContent).find();
    }

    private boolean serverRoutesAvailable() {
        return !options.getClassFinder().getAnnotatedClasses(Route.class)
                .isEmpty();
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
        try (InputStream indexTsStream = getClass()
                .getResourceAsStream(fileName)) {
            indexTemplate = IOUtils.toString(indexTsStream, UTF_8);
        }
        return indexTemplate;
    }
}
