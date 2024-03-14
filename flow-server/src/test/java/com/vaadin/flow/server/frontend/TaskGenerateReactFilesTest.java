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
import java.nio.charset.StandardCharsets;
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
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ExecutionFailedException;
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
    }

    @Test
    public void routesContainImport_serverSideRoutes_noExceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
                        import {serverSideRoutes} from "Frontend/generated/flow/Flow";
                        import {protectRoutes} from "@hilla/react-auth";
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

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();
    }

    @Test
    public void routesContainImport_buildRoutes_noExceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
                        import { buildRoutes } from "Frontend/generated/flow/Flow";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));

                        export const routes: RouteObject[] = buildRoutes(
                          [
                            { path: '/', element: <HelloWorldView />, handle: { title: 'Hello World', rolesAllowed: ['USER'] } },
                            { path: '/about', element: <AboutView />, handle: { title: 'About' } }
                          ]);

                        export default createBrowserRouter(routes);
                """;

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();
    }

    @Test
    public void routesContainMultipleFlowImports_noExceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
                        import { tea, buildRoutes, serverSideRoutes, coffee } from "Frontend/generated/flow/Flow";
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

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();
    }

    @Test
    public void routesMissingImport_noBuildOrServerSideRoutes_exceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
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

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        Exception exception = Assert.assertThrows(
                ExecutionFailedException.class, () -> task.execute());
        Assert.assertEquals(String.format(TaskGenerateReactFiles.NO_IMPORT,
                routesTsx.getPath()), exception.getMessage());
    }

    @Test
    public void routesMissingImport_expectionThrown() throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
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

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        Exception exception = Assert.assertThrows(
                ExecutionFailedException.class, () -> task.execute());
        Assert.assertEquals(String.format(TaskGenerateReactFiles.NO_IMPORT,
                routesTsx.getPath()), exception.getMessage());
    }

    @Test
    public void routesContainsRoutesExport_noExceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
                        import {serverSideRoutes} from "Frontend/generated/flow/Flow";
                        import {protectRoutes} from "@hilla/react-auth";
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

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();
    }

    @Test
    public void missingImport_noServerRoutesDefined_noExceptionThrown()
            throws IOException, ExecutionFailedException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
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
    public void routesexportMissing_expectionThrown() throws IOException {
        String content = """
                        import HelloWorldView from 'Frontend/views/helloworld/HelloWorldView.js';
                        import MainLayout from 'Frontend/views/MainLayout.js';
                        import { lazy } from 'react';
                        import { createBrowserRouter, RouteObject } from 'react-router-dom';
                        import {serverSideRoutes} from "Frontend/generated/flow/Flow";
                        import {protectRoutes} from "@hilla/react-auth";
                        import LoginView from "Frontend/views/LoginView";

                        const AboutView = lazy(async () => import('Frontend/views/about/AboutView.js'));

                        const routes: RouteObject[] = protectRoutes([
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

        FileUtils.write(routesTsx, content, StandardCharsets.UTF_8);

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        Exception exception = Assert.assertThrows(
                ExecutionFailedException.class, () -> task.execute());
        Assert.assertEquals(TaskGenerateReactFiles.MISSING_ROUTES_EXPORT,
                exception.getMessage());
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

    @Tag("div")
    @Route("test")
    private class TestRoute extends Component {
    }
}