/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.ExecutionFailedException;

public class TaskGenerateReactFilesTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    Options options;
    File routesTsx, frontend;

    @Before
    public void setup() throws IOException {
        options = new Options(Mockito.mock(Lookup.class),
                temporaryFolder.getRoot()).withBuildDirectory("target");
        frontend = temporaryFolder.newFolder("frontend");
        options.withFrontendDirectory(frontend);
        routesTsx = new File(frontend, "routes.tsx");
    }

    @Test
    public void reactFilesAreWrittenToFrontend()
            throws ExecutionFailedException {

        TaskGenerateReactFiles task = new TaskGenerateReactFiles(options);

        task.execute();

        Assert.assertTrue("Missing ./frontend/App.tsx",
                new File(frontend, "App.tsx").exists());
        Assert.assertTrue("Missing ./frontend/generated/flow/Flow.tsx",
                new File(new File(frontend, FrontendUtils.GENERATED),
                        "flow/Flow.tsx").exists());
        Assert.assertTrue("Missing ./frontend/routes.tsx",
                new File(frontend, "routes.tsx").exists());
    }

    @Test
    public void routesContainsImport_noExpectionThrown()
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
}