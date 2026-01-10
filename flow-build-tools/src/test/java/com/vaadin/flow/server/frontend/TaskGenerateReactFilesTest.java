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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

public class TaskGenerateReactFilesTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    Options options;
    File routesTsx, frontend, frontendGenerated;
    ClassFinder classFinder;

    @Before
    public void setup() throws IOException {
        classFinder = Mockito.mock(ClassFinder.class);

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.singleton(TestRoute.class));
        Mockito.when(classFinder.getClassLoader())
                .thenReturn(getClass().getClassLoader());

        options = new MockOptions(classFinder, temporaryFolder.getRoot())
                .withBuildDirectory("target");
        frontend = temporaryFolder
                .newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);
        options.withFrontendDirectory(frontend);
        frontendGenerated = temporaryFolder.newFolder(
                FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR);
        options.withFrontendGeneratedFolder(frontendGenerated);
        routesTsx = new File(frontend, "routes.tsx");
    }

    @Test
    public void reactFilesAreWrittenToFrontend()
            throws ExecutionFailedException {

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();

        Assert.assertTrue("Missing ./frontend/generated/flow/Flow.tsx",
                new File(new File(frontend, FrontendUtils.GENERATED),
                        "flow/Flow.tsx").exists());
        Assert.assertTrue(
                "Missing ./frontend/" + FrontendUtils.GENERATED + "routes.tsx",
                new File(new File(frontend, FrontendUtils.GENERATED),
                        "routes.tsx").exists());
        Assert.assertFalse("Missing ./frontend/routes.tsx",
                new File(frontend, "routes.tsx").exists());
        Assert.assertTrue(
                "Missing ./frontend/" + FrontendUtils.GENERATED
                        + "layouts.json",
                new File(new File(frontend, FrontendUtils.GENERATED),
                        "layouts.json").exists());
        assertGeneratedFileExists("jsx-dev-transform/index.ts");
        assertGeneratedFileExists("jsx-dev-transform/jsx-runtime.ts");
        assertGeneratedFileExists("jsx-dev-transform/jsx-dev-runtime.ts");
    }

    private void assertGeneratedFileExists(String filename) {
        Assert.assertTrue(
                "Missing ./frontend/" + FrontendUtils.GENERATED + filename,
                new File(new File(frontend, FrontendUtils.GENERATED), filename)
                        .exists());
    }

    @Test
    public void layoutsJson_containsExpectedPaths()
            throws ExecutionFailedException, IOException {
        Mockito.when(options.getClassFinder().getAnnotatedClasses(Layout.class))
                .thenReturn(Collections.singleton(TestLayout.class));

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);
        task.execute();

        String layoutsContent = Files.readString(
                new File(options.getFrontendGeneratedFolder(), "layouts.json")
                        .toPath());

        Assert.assertEquals("[{\"path\":\"/test\"}]", layoutsContent);

    }

    @Test
    public void routesContainImportAndUsage_serverSideRoutes_noExceptionThrown()
            throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import Flow from 'Frontend/generated/flow/Flow';
                        import {protectRoutes} from "@hilla/react-auth";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));
                        export const { router, routes } = new RouterConfigurationBuilder()
                            .withReactRoutes([
                                 {
                                     element: <MainLayout />,
                                     handle: { title: 'Main' },
                                     children: [
                                         { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                                         { path: '/about', element: <AboutView />, handle: { title: 'About' } },
                                     ],
                                 },
                             ])
                            .withFallback(Flow)
                            .withReactRoutes([
                                 { path: '/login', element: <Login />, handle: { title: 'Login' } },
                             ])
                            .protect()
                            .build();
                """;

        executeTask(content);
    }

    @Test
    public void routesContainOnlyImport_serverSideRoutes_exceptionThrown()
            throws IOException {
        String content = """
                         import { serverSideRoutes } from 'Frontend/generated/flow/Flow';
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void routesContainNoImport_serverSideRoutes_exceptionThrown()
            throws IOException {
        String content = """
                         export const routes = [
                             ...serverSideRoutes
                         ] as RouteObject[];
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void routesContainMultipleFlowImports_noExceptionThrown()
            throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { createBrowserRouter, RouteObject } from 'react-router';
                        import { tea, serverSideRoutes, coffee } from "Frontend/generated/flow/Flow";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));

                        export const routes: RouteObject[] = protectRoutes([
                          {
                            element: <MainLayout />,
                            handle: { title: 'Main' },
                            children: [
                              { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                              { path: '/about', element: <AboutView />, handle: { title: 'About' } },
                              ...serverSideRoutes
                            ],
                          },
                          { path: '/login', element: <LoginView />},
                        ]);

                        export default createBrowserRouter(routes);
                """;

        executeTask(content);
    }

    @Test
    public void routesMissingImportAndUsage_noBuildOrServerSideRoutes_exceptionThrown()
            throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { createBrowserRouter, RouteObject } from 'react-router';
                        import { tea, coffee } from "Frontend/generated/flow/Flow";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));

                        export const routes: RouteObject[] = protectRoutes([
                          {
                            element: <MainLayout />,
                            handle: { title: 'Main' },
                            children: [
                              { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                              { path: '/about', element: <AboutView />, handle: { title: 'About' } }
                            ],
                          },
                          { path: '/login', element: <LoginView />},
                        ]);

                        export default createBrowserRouter(routes);
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void routesMissingImport_exceptionThrown() throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router';
                        import {protectRoutes} from "@hilla/react-auth";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));

                        export const routes: RouteObject[] = protectRoutes([
                          {
                            element: <MainLayout />,
                            handle: { title: 'Main' },
                            children: [
                              { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                              { path: '/about', element: <AboutView />, handle: { title: 'About' } }
                            ],
                          },
                          { path: '/login', element: <LoginView />},
                        ]);

                        export default createBrowserRouter(routes);
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void missingImport_noServerRoutesDefined_noExceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router';
                        import {protectRoutes} from "@hilla/react-auth";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));

                        export const routes: RouteObject[] = protectRoutes([
                          {
                            element: <MainLayout />,
                            handle: { title: 'Main' },
                            children: [
                              { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                              { path: '/about', element: <AboutView />, handle: { title: 'About' } }
                            ],
                          },
                          { path: '/login', element: <LoginView />},
                        ]);

                        export default createBrowserRouter(routes);
                """;

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        Mockito.when(classFinder.getAnnotatedClasses(Route.class))
                .thenReturn(Collections.emptySet());

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();
    }

    @Test
    public void routesExportMissing_exceptionThrown() throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import Flow from 'Frontend/generated/flow/Flow';
                        import {protectRoutes} from "@hilla/react-auth";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));
                        export const { router } = new RouterConfigurationBuilder()
                            .withReactRoutes([
                                 {
                                     element: <MainLayout />,
                                     handle: { title: 'Main' },
                                     children: [
                                         { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                                         { path: '/about', element: <AboutView />, handle: { title: 'About' } },
                                     ],
                                 },
                             ])
                            .withFallback(Flow)
                            .withReactRoutes([
                                 { path: '/login', element: <Login />, handle: { title: 'Login' } },
                             ])
                            .protect()
                            .build();
                """;

        assertTaskExecutionFails(content,
                TaskGenerateReactFiles.MISSING_ROUTES_EXPORT);
    }

    @Test
    public void withFallbackMissing_exceptionThrown() throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import Flow from 'Frontend/generated/flow/Flow';
                        import {protectRoutes} from "@hilla/react-auth";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));
                        export const { router, routes } = new RouterConfigurationBuilder()
                            .withReactRoutes([
                                 {
                                     element: <MainLayout />,
                                     handle: { title: 'Main' },
                                     children: [
                                         { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                                         { path: '/about', element: <AboutView />, handle: { title: 'About' } },
                                     ],
                                 },
                             ])
                            .withReactRoutes([
                                 { path: '/login', element: <Login />, handle: { title: 'Login' } },
                             ])
                            .protect()
                            .build();
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void withFallbackReceivesDifferentObject_exceptionThrown()
            throws IOException {
        String content = """
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import foo from 'Frontend/generated/flow/Flow';

                        export const { router, routes } = new RouterConfigurationBuilder()
                            .withFallback(Flow)
                            .build();
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void withFallbackMissesImport_exceptionThrown() throws IOException {
        String content = """
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));
                        export const { router, routes } = new RouterConfigurationBuilder()
                            .withFallback(Flow)
                            .build();
                """;

        assertTaskExecutionFails(content, String
                .format(TaskGenerateReactFiles.NO_IMPORT, routesTsx.getPath()));
    }

    @Test
    public void frontendReactFilesAreCleanedWhenReactIsDisabled()
            throws ExecutionFailedException {
        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);
        task.execute();

        options.withReact(false);
        task.execute();

        Assert.assertFalse(
                "./frontend/routes.tsx should be removed when react is disabled",
                new File(frontend, "routes.tsx").exists());
        Assert.assertFalse(
                "./frontend/generated/flow/Flow.tsx should be removed when react is disabled",
                new File(frontendGenerated,
                        TaskGenerateReactFiles.FLOW_FLOW_TSX).exists());
        Assert.assertFalse(
                "./frontend/generated/flow/ReactAdapter.tsx should be removed when react is disabled",
                new File(frontendGenerated,
                        TaskGenerateReactFiles.FLOW_REACT_ADAPTER_TSX)
                        .exists());
        Assert.assertFalse(
                "./frontend/routes.tsx.flowBackup should not be created with default content",
                new File(frontend, "routes.tsx.flowBackup").exists());
        assertGeneratedFileRemoved("jsx-dev-transform/index.ts");
        assertGeneratedFileRemoved("jsx-dev-transform/jsx-dev-runtime.ts");
        assertGeneratedFileRemoved("jsx-dev-transform/jsx-runtime.ts");
    }

    private void assertGeneratedFileRemoved(String filename) {
        Assert.assertFalse(
                "./frontend/generated/" + filename
                        + " should be removed when react is disabled",
                new File(frontendGenerated, filename).exists());
    }

    @Test
    public void frontendCustomReactFilesAreCleanedAndBackUppedWhenReactIsDisabled()
            throws ExecutionFailedException, IOException {
        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);
        task.execute();
        FileUtils.write(routesTsx, "Custom content", StandardCharsets.UTF_8);

        options.withReact(false);
        task.execute();

        Assert.assertFalse(
                "./frontend/routes.tsx should be removed when react is disabled",
                new File(frontend, "routes.tsx").exists());
        Assert.assertFalse(
                "./frontend/" + FrontendUtils.GENERATED
                        + "routes.tsx should be removed when react is disabled",
                new File(new File(frontend, FrontendUtils.GENERATED),
                        "routes.tsx").exists());
        Assert.assertTrue("./frontend/routes.tsx.flowBackup should exist",
                new File(frontend, "routes.tsx.flowBackup").exists());
    }

    @Test
    public void routesContainExport_noConst_noExceptionThrown()
            throws IOException {
        String content = """
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import Flow from 'Frontend/generated/flow/Flow';

                        const { router, originalRoutes } = new RouterConfigurationBuilder();
                           .withFallback(Flow)
                           .build();

                        const routes = originalRoutes;
                        export { router, routes }
                """;

        executeTask(content);
    }

    @Test
    public void routesContainExport_twoSingleExports_noExceptionThrown()
            throws IOException {
        String content = """
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import Flow from 'Frontend/generated/flow/Flow';

                        const { router, originalRoutes } = new RouterConfigurationBuilder();
                           .withFallback(Flow)
                           .build();

                        const routes = originalRoutes;
                        export { routes }
                        router = anotherRouter;
                        export {router}
                """;

        executeTask(content);
    }

    @Test
    public void routesContainExport_oneSingleExport_exceptionThrown()
            throws IOException {
        String content = """
                        import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                        import Flow from 'Frontend/generated/flow/Flow';

                        const { router, originalRoutes } = new RouterConfigurationBuilder();
                           .withFallback(Flow)
                           .build();

                        const routes = originalRoutes;
                        export { routes }
                """;

        assertTaskExecutionFails(content,
                String.format(TaskGenerateReactFiles.MISSING_ROUTES_EXPORT,
                        routesTsx.getPath()));
    }

    private void assertTaskExecutionFails(String content, String errorMessage)
            throws IOException {
        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        Exception exception = Assert
                .assertThrows(ExecutionFailedException.class, task::execute);
        Assert.assertEquals(errorMessage, exception.getMessage());
    }

    private void executeTask(String content) throws IOException {
        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);
        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);
        try {
            task.execute();
        } catch (ExecutionFailedException e) {
            throw new AssertionError(
                    "Expected execution to complete successfully, but exception was thrown",
                    e);
        }
    }

    @Tag("div")
    @Route("test")
    private class TestRoute extends Component {
    }

    @Tag("div")
    @Layout("/test")
    private class TestLayout extends Component implements RouterLayout {
    }
}
