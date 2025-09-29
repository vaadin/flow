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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import tools.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrontendUtilsTest {

    private static final String USER_HOME = "user.home";

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private static final String ROUTES_CONTENT_WITH_ONLY_SERVERSIDE_ROUTES = """
                import {serverSideRoutes} from "Frontend/generated/flow/Flow";

                export const routes = [
                  ...serverSideRoutes
                ]
            """;

    private static final String ROUTES_CONTENT_WITH_ONLY_CLIENTSIDE_ROUTES = """
                import {serverSideRoutes} from "Frontend/generated/flow/Flow";

                export const routes = [
                  {
                    path: 'hello',
                    component: 'hello-world-view',
                    title: 'Hello World',
                  },
                  {
                    path: '',
                    component: 'about-view',
                    title: 'About',
                  }
                ]
            """;

    private static final String ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_1 = """
                import {serverSideRoutes} from "Frontend/generated/flow/Flow";

                let routes = [
                  {
                    path: 'hello',
                    component: 'hello-world-view',
                    title: 'Hello World',
                  },
                  ...serverSideRoutes
                ];

                let unknownRoutes = [
                  ...serverSideRoutes
                ];
            """;

    private static final String ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_2 = """
                import {serverSideRoutes} from "Frontend/generated/flow/Flow";

                export const routes: RouteViewObject[] = [
                  {
                    path: 'hello',
                    component: 'hello-world-view',
                    title: 'Hello World',
                  },
                  ...serverSideRoutes
                ]
            """;

    private static final String ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_3 = """
                import {serverSideRoutes} from "Frontend/generated/flow/Flow";

                var myFunc = function() {
                     // this is checked too
                     export const routes = [
                      ...serverSideRoutes
                    ];
                }

                let unknownRoutes = [
                  ...serverSideRoutes
                ];

                const routes = [
                  {
                    path: 'hello',
                    component: 'hello-world-view',
                    title: 'Hello World',
                  },
                  ...serverSideRoutes
                ];
            """;

    private static final String ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_MAINLAYOUT_TSX = """
                import { serverSideRoutes } from "Frontend/generated/flow/Flow";
                import MainLayout from 'Frontend/views/MainLayout';
                import ContactsView from 'Frontend/views/ContactsView';
                import AboutView from 'Frontend/views/AboutView';
                import { RouteObject } from 'react-router';

                export const routes: RouteObject[] = [
                      {
                          element: <MainLayout />,
                          handle: { title: 'Hilla CRM' },
                          children: [
                              { path: '/', element: <ContactsView />, handle: { title: 'Contacts' } },
                              { path: '/about', element: <AboutView />, handle: { title: 'About' } },
                              ...serverSideRoutes
                          ],
                      },
                  ];
            """;

    private static final String ROUTES_CONTENT_WITH_SERVER_SIDE_ROUTES_MAINLAYOUT_TSX = """
                import { serverSideRoutes } from "Frontend/generated/flow/Flow";
                import MainLayout from 'Frontend/views/MainLayout';
                import ContactsView from 'Frontend/views/ContactsView';
                import AboutView from 'Frontend/views/AboutView';
                import { RouteObject } from 'react-router';

                export const routes: RouteObject[] = [
                      {
                          element: <MainLayout />,
                          handle: { title: 'Hilla CRM' },
                          children: [
                              ...serverSideRoutes
                          ],
                      },
                  ];
            """;

    private static final String ROUTES_CONTENT_WITH_WITH_FILE_ROUTES = """
                import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                import Flow from 'Frontend/generated/flow/Flow';
                import fileRoutes from 'Frontend/generated/file-routes';

                export const { router, routes } = new RouterConfigurationBuilder()
                    .withFileRoutes(fileRoutes)
                    .withFallback(Flow)
                    .build();
            """;

    private static final String ROUTES_CONTENT_WITH_WITH_REACT_ROUTES = """
                import { RouterConfigurationBuilder } from '@vaadin/hilla-file-router/runtime.js';
                import Flow from 'Frontend/generated/flow/Flow';

                export const { router, routes } = new RouterConfigurationBuilder()
                    .withReactRoutes([
                      {
                          element: <MainLayout />,
                          handle: { title: 'Hilla CRM' }
                      },
                    ])
                    .withFallback(Flow).build();
            """;

    private static final String HILLA_VIEW_TSX = """
                import { VerticalLayout } from "@vaadin/react-components/VerticalLayout.js";
                export default function AboutView() {
                    return (
                        <VerticalLayout theme="padding">
                            <p>This is a Hilla view</p>
                        </VerticalLayout>
                    );
                }
            """;

    @Test
    public void parseValidVersions() {
        FrontendVersion sixPointO = new FrontendVersion(6, 0);

        FrontendVersion requiredVersionTen = new FrontendVersion(10, 0);
        assertFalse(sixPointO.isEqualOrNewer(requiredVersionTen));
        assertFalse(sixPointO.isEqualOrNewer(new FrontendVersion(6, 1)));
        assertTrue(new FrontendVersion("10.0.0")
                .isEqualOrNewer(requiredVersionTen));
        assertTrue(new FrontendVersion("10.0.2")
                .isEqualOrNewer(requiredVersionTen));
        assertTrue(new FrontendVersion("10.2.0")
                .isEqualOrNewer(requiredVersionTen));
    }

    @Test
    public void validateLargerThan_passesForNewVersion() {
        FrontendUtils.validateToolVersion("test", new FrontendVersion("10.0.2"),
                new FrontendVersion(10, 0));
        FrontendUtils.validateToolVersion("test", new FrontendVersion("10.1.2"),
                new FrontendVersion(10, 0));
        FrontendUtils.validateToolVersion("test", new FrontendVersion("11.0.2"),
                new FrontendVersion(10, 0));
    }

    @Test
    public void validateLargerThan_throwsForOldVersion() {
        try {
            FrontendUtils.validateToolVersion("test",
                    new FrontendVersion(7, 5, 0), new FrontendVersion(10, 0));
            Assert.fail("No exception was thrown for old version");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains(
                    "Your installed 'test' version (7.5.0) is too old. Supported versions are 10.0+"));
        }
    }

    @Test
    public void parseValidToolVersions() throws IOException {
        Assert.assertEquals("10.11.12",
                FrontendUtils.parseVersionString("v10.11.12"));
        Assert.assertEquals("8.0.0",
                FrontendUtils.parseVersionString("v8.0.0"));
        Assert.assertEquals("8.0.0", FrontendUtils.parseVersionString("8.0.0"));
        Assert.assertEquals("6.9.0", FrontendUtils
                .parseVersionString("Aktive Codepage: 1252\n6.9.0\n"));
    }

    @Test(expected = IOException.class)
    public void parseEmptyToolVersions() throws IOException {
        FrontendUtils.parseVersionString(" \n");
    }

    @Test
    public void should_getUnixRelativePath_when_givenTwoPaths() {
        Path sourcePath = Mockito.mock(Path.class);
        Path relativePath = Mockito.mock(Path.class);
        Mockito.when(sourcePath.relativize(Mockito.any()))
                .thenReturn(relativePath);
        Mockito.when(relativePath.toString())
                .thenReturn("this\\is\\windows\\path");

        String relativeUnixPath = FrontendUtils.getUnixRelativePath(sourcePath,
                tmpDir.getRoot().toPath());
        Assert.assertEquals(
                "Should replace windows path separator with unix path separator",
                "this/is/windows/path", relativeUnixPath);
        Mockito.when(relativePath.toString()).thenReturn("this/is/unix/path");

        relativeUnixPath = FrontendUtils.getUnixRelativePath(sourcePath,
                tmpDir.getRoot().toPath());
        Assert.assertEquals(
                "Should keep the same path when it uses unix path separator",
                "this/is/unix/path", relativeUnixPath);
    }

    @Test
    public synchronized void getVaadinHomeDirectory_noVaadinFolder_folderIsCreated()
            throws IOException {
        String originalHome = System.getProperty(USER_HOME);
        File home = tmpDir.newFolder();
        System.setProperty(USER_HOME, home.getPath());
        try {
            File vaadinDir = new File(home, ".vaadin");
            if (vaadinDir.exists()) {
                FileUtils.deleteDirectory(vaadinDir);
            }
            File vaadinHomeDirectory = FrontendUtils.getVaadinHomeDirectory();
            Assert.assertTrue(vaadinHomeDirectory.exists());
            Assert.assertTrue(vaadinHomeDirectory.isDirectory());

            // access it one more time
            vaadinHomeDirectory = FrontendUtils.getVaadinHomeDirectory();
            Assert.assertEquals(".vaadin", vaadinDir.getName());
        } finally {
            System.setProperty(USER_HOME, originalHome);
        }
    }

    @Test(expected = IllegalStateException.class)
    public synchronized void getVaadinHomeDirectory_vaadinFolderIsAFile_throws()
            throws IOException {
        String originalHome = System.getProperty(USER_HOME);
        File home = tmpDir.newFolder();
        System.setProperty(USER_HOME, home.getPath());
        try {
            File vaadinDir = new File(home, ".vaadin");
            if (vaadinDir.exists()) {
                FileUtils.deleteDirectory(vaadinDir);
            }
            vaadinDir.createNewFile();
            FrontendUtils.getVaadinHomeDirectory();
        } finally {
            System.setProperty(USER_HOME, originalHome);
        }
    }

    @Test
    public void commandToString_longCommand_resultIsWrapped() {
        List<String> command = Arrays.asList("./node/node",
                "./node_modules/webpack-dev-server/bin/webpack-dev-server.js",
                "--config", "./webpack.config.js", "--port 57799",
                "--env watchDogPort=57798", "-d", "--inline=false");
        String wrappedCommand = FrontendUtils.commandToString(".", command);
        Assert.assertEquals(
                """

                        ./node/node \\\s
                            ./node_modules/webpack-dev-server/bin/webpack-dev-server.js \\\s
                            --config ./webpack.config.js --port 57799 \\\s
                            --env watchDogPort=57798 -d --inline=false\s
                        """,
                wrappedCommand);
    }

    @Test
    public void commandToString_commandContainsBaseDir_baseDirIsReplaced() {
        List<String> command = Arrays.asList("./node/node",
                "/somewhere/not/disclosable/node_modules/webpack-dev-server/bin/webpack-dev-server.js");
        String wrappedCommand = FrontendUtils
                .commandToString("/somewhere/not/disclosable", command);
        Assert.assertEquals(
                """

                        ./node/node \\\s
                            ./node_modules/webpack-dev-server/bin/webpack-dev-server.js\s
                        """,
                wrappedCommand);
    }

    @Test
    public void deleteNodeModules_nopIfNotExists() throws IOException {
        File nodeModules = new File(tmpDir.getRoot(), "node_modules");
        FrontendUtils.deleteNodeModules(nodeModules);
    }

    @Test(expected = IOException.class)
    public void deleteNodeModules_throwsIfNotNamedNodeModules()
            throws IOException {
        File myModules = new File(tmpDir.getRoot(), "my_modules");
        myModules.mkdirs();
        FrontendUtils.deleteNodeModules(myModules);
    }

    @Test
    public void deleteNodeModules_canDeleteSymlinksAndNotFollowThem()
            throws IOException {

        // Test fails on Windows due to UAC FileSystemException
        Assume.assumeFalse(FrontendUtils.isWindows());

        File externalDir = new File(tmpDir.getRoot(), "external");
        File externalLicense = new File(externalDir, "LICENSE");

        externalLicense.getParentFile().mkdirs();
        externalLicense.createNewFile();

        File nodeModules = new File(tmpDir.getRoot(), "node_modules");
        File containing = new File(nodeModules, ".pnpm/a/node_modules/dep");
        containing.mkdirs();
        File license = new File(containing, "LICENSE");
        license.createNewFile();

        File linking = new File(nodeModules, ".pnpm/b/node_modules/dep");
        linking.getParentFile().mkdirs();
        Files.createSymbolicLink(linking.toPath(),
                new File("../../a/node_modules/dep").toPath());

        File linkingExternal = new File(nodeModules,
                ".pnpm/b/node_modules/external");
        Files.createSymbolicLink(linkingExternal.toPath(),
                new File("../../../../external").toPath());

        Assert.assertTrue(nodeModules.exists());
        Assert.assertTrue(linking.exists());
        Assert.assertTrue(new File(linking, "LICENSE").exists());
        Assert.assertTrue(new File(linkingExternal, "LICENSE").exists());

        FrontendUtils.deleteNodeModules(nodeModules);

        Assert.assertFalse(nodeModules.exists());
        Assert.assertTrue(externalLicense.exists());
    }

    @Test
    public void symlinkByNpm_deleteDirectory_doesNotDeleteSymlinkFolderFiles()
            throws IOException, ExecutionFailedException {
        File npmFolder = tmpDir.newFolder();

        File symbolic = new File(npmFolder, "symbolic");
        symbolic.mkdir();
        File symbolicPackageJson = new File(symbolic, "package.json");
        FileUtils.writeStringToFile(symbolicPackageJson, "{}",
                StandardCharsets.UTF_8);
        File linkFolderFile = new File(symbolic, "symbol.txt");
        linkFolderFile.createNewFile();

        final ObjectNode packageJson = JacksonUtils.createObjectNode();
        packageJson.set(DEPENDENCIES, JacksonUtils.createObjectNode());

        ((ObjectNode) packageJson.get(DEPENDENCIES)).put("@symbolic/link",
                "./" + symbolic.getName());

        FileUtils.writeStringToFile(new File(npmFolder, PACKAGE_JSON),
                packageJson.toString(), StandardCharsets.UTF_8);

        Logger logger = Mockito.spy(LoggerFactory.getLogger(NodeUpdater.class));
        Options options = new MockOptions(npmFolder).withBuildDirectory(TARGET);
        NodeUpdater nodeUpdater = new NodeUpdater(
                Mockito.mock(FrontendDependencies.class), options) {
            @Override
            public void execute() {
                // no need to execute logic for this test
            }

            @Override
            Logger log() {
                return logger;
            }
        };

        options.withNodeVersion(FrontendTools.DEFAULT_NODE_VERSION)
                .withNodeDownloadRoot(
                        URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT));
        new TaskRunNpmInstall(nodeUpdater, options).execute();

        FrontendUtils.deleteNodeModules(new File(npmFolder, "node_modules"));

        Assert.assertTrue("Linked folder contents should not be removed.",
                linkFolderFile.exists());
    }

    @Test
    public void consumeProcessStreams_streamsConsumed() throws Exception {

        Pair<String, String> streams = executeExternalProcess("STDOUT", "Test",
                "text");
        String stdOut = streams.getFirst();
        String stdErr = streams.getSecond();
        Assert.assertTrue("Unexpected STDOUT contents: " + stdOut,
                stdOut.contains("STDOUT, Test, text"));
        Assert.assertTrue("Expected STDERR to be empty, but was " + stdErr,
                stdErr.isBlank());

        streams = executeExternalProcess("STDERR", "Test", "text");
        stdOut = streams.getFirst();
        stdErr = streams.getSecond();
        Assert.assertTrue("Expected STDOUT to be empty, but was " + stdOut,
                stdOut.isBlank());
        Assert.assertTrue("Unexpected STDERR contents: " + stdErr,
                stdErr.contains("STDERR, Test, text"));

        streams = executeExternalProcess("BOTH", "Test", "text");
        stdOut = streams.getFirst();
        stdErr = streams.getSecond();
        Assert.assertTrue("Unexpected STDERR contents: " + stdOut,
                stdOut.contains("STDOUT: BOTH, Test, text"));
        Assert.assertTrue("Unexpected STDERR contents: " + stdErr,
                stdErr.contains("STDERR: BOTH, Test, text"));

        streams = executeExternalProcess("THROW EXCEPTION");
        stdOut = streams.getFirst();
        stdErr = streams.getSecond();
        Assert.assertTrue("Expected STDOUT to be empty, but was " + stdOut,
                stdOut.isBlank());
        Assert.assertTrue("Unexpected STDERR contents: " + stdErr,
                stdErr.contains("RuntimeException")
                        && stdErr.contains("Invalid stream THROW EXCEPTION"));
    }

    @Test
    public void isReactRouterRequired_importsVaadinRouter_false()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.INDEX_TS,
                """
                            import { Router } from '@vaadin/router';
                            import { routes } from './routes';

                            export const router = new Router(document.querySelector('#outlet'));
                            router.setRoutes(routes);
                        """);
        Assert.assertFalse("vaadin-router expected when it imported",
                FrontendUtils.isReactRouterRequired(frontend));
    }

    @Test
    public void isReactRouterRequired_doesntImportVaadinRouter_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.INDEX_TS,
                """
                            import { createElement } from "react";
                            import { createRoot } from "react-dom/client";
                            import App from "./App.js";

                            createRoot(document.getElementById("outlet")!).render(createElement(App));
                        """);
        Assert.assertTrue(
                "react-router expected when no vaadin-router imported",
                FrontendUtils.isReactRouterRequired(frontend));
    }

    @Test
    public void isReactRouterRequired_noIndexTsFile_true() throws IOException {
        File frontend = tmpDir.newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);
        Assert.assertTrue("react-router expected when index.ts isn't there",
                FrontendUtils.isReactRouterRequired(frontend));
    }

    @Test
    public void isHillaViewsUsed_onlyServerSideRoutesTs_false()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_ONLY_SERVERSIDE_ROUTES);
        Assert.assertFalse("hilla-views are not expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_onlyServerSideRoutesTsx_false()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_ONLY_SERVERSIDE_ROUTES);
        Assert.assertFalse("hilla-views are not expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_onlyClientSideRoutesTs_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_ONLY_CLIENTSIDE_ROUTES);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_onlyClientSideRoutesTsx_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_ONLY_CLIENTSIDE_ROUTES);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesTs1_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_1);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesTsx1_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_1);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesTs2_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_2);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesTsx2_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_2);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesTs3_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_3);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesTsx3_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_3);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_clientAndServerSideRoutesMainLayoutTsx_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_MAINLAYOUT_TSX);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_serverSideRoutesMainLayoutTsx_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_SERVER_SIDE_ROUTES_MAINLAYOUT_TSX);
        Assert.assertTrue("hilla-views are expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_withFileRoutes_true() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_WITH_FILE_ROUTES);
        Assert.assertTrue("hilla-views are expected, as withFileRoutes is used",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_withReactRoutes_true() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_WITH_REACT_ROUTES);
        Assert.assertTrue(
                "hilla-views are expected, as withReactRoutes is used",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_nonEmptyHillaViewInViews_true()
            throws IOException {
        File frontend = null;
        for (String ext : Arrays.asList(".js", ".jsx", ".ts", ".tsx")) {
            try {
                frontend = prepareFrontendForRoutesFile(
                        FrontendUtils.HILLA_VIEWS_PATH + "/HillaView" + ext,
                        HILLA_VIEW_TSX);
                Assert.assertTrue(
                        "hilla view is present, thus Hilla is expected",
                        FrontendUtils.isHillaViewsUsed(frontend));
            } finally {
                if (frontend != null && frontend.exists()) {
                    FileUtils.deleteQuietly(frontend);
                }
            }
        }
    }

    @Test
    public void isHillaViewsUsed_emptyHillaViewContent_false()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(
                FrontendUtils.HILLA_VIEWS_PATH + "/HillaView.ts", "//comment");
        Assert.assertFalse("empty Hilla view, Hilla not expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    @Test
    public void isHillaViewsUsed_noViews_false() throws IOException {
        File frontend = prepareFrontendForRoutesFile(
                FrontendUtils.HILLA_VIEWS_PATH + "/foo.css", "some css");
        Assert.assertFalse("no Hilla views, Hilla not expected",
                FrontendUtils.isHillaViewsUsed(frontend));
    }

    private File prepareFrontendForRoutesFile(String fileName, String content)
            throws IOException {
        return prepareFrontendForRoutesFile(fileName, content, false);
    }

    private File prepareFrontendForRoutesFile(String fileName, String content,
            boolean generateDummyFSView) throws IOException {
        File frontend = tmpDir.newFolder(FrontendUtils.DEFAULT_FRONTEND_DIR);
        FileUtils.write(new File(frontend, fileName), content,
                StandardCharsets.UTF_8);
        if (generateDummyFSView) {
            FileUtils.write(new File(frontend, "views/testFSView.ts"),
                    "export default function TestFSView() { return 'TestFSView'; }",
                    StandardCharsets.UTF_8);
        }
        return frontend;
    }

    private Pair<String, String> executeExternalProcess(String... args)
            throws Exception {
        List<String> cmd = new ArrayList<>(List.of(
                Paths.get(System.getProperty("java.home"), "bin", "java")
                        .toFile().getAbsolutePath(),
                "-cp", System.getProperty("java.class.path"),
                TestExecutable.class.getName()));
        cmd.addAll(List.of(args));
        Process process = new ProcessBuilder(cmd).start();
        process.waitFor(1, TimeUnit.SECONDS);
        return FrontendUtils.consumeProcessStreams(process).get(100,
                TimeUnit.MILLISECONDS);
    }

    public static class TestExecutable {
        public static void main(String... args) {
            switch (args[0]) {
            case "STDOUT" -> System.out.println(String.join(", ", args));
            case "STDERR" -> System.err.println(String.join(", ", args));
            case "BOTH" -> {
                System.out.println("STDOUT: " + String.join(", ", args));
                System.err.println("STDERR: " + String.join(", ", args));
            }
            default -> throw new RuntimeException("Invalid stream " + args[0]);
            }
        }
    }
}
