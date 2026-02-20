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
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.experimental.CoreFeatureFlagProvider;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendVersion;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependencies;
import com.vaadin.tests.util.MockOptions;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.NodeUpdater.DEPENDENCIES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class FrontendUtilsTest {

    private static final String USER_HOME = "user.home";

    @TempDir
    File tmpDir;

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
    void parseValidVersions() {
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
    void parseValidToolVersions() throws IOException {
        assertEquals("10.11.12", FrontendUtils.parseVersionString("v10.11.12"));
        assertEquals("8.0.0", FrontendUtils.parseVersionString("v8.0.0"));
        assertEquals("8.0.0", FrontendUtils.parseVersionString("8.0.0"));
        assertEquals("6.9.0", FrontendUtils
                .parseVersionString("Aktive Codepage: 1252\n6.9.0\n"));
    }

    @Test
    void parseEmptyToolVersions() throws IOException {
        assertThrows(IOException.class,
                () -> FrontendUtils.parseVersionString(" \n"));
    }

    @Test
    void should_getUnixRelativePath_when_givenTwoPaths() {
        Path sourcePath = Mockito.mock(Path.class);
        Path relativePath = Mockito.mock(Path.class);
        Mockito.when(sourcePath.relativize(Mockito.any()))
                .thenReturn(relativePath);
        Mockito.when(relativePath.toString())
                .thenReturn("this\\is\\windows\\path");

        String relativeUnixPath = FrontendUtils.getUnixRelativePath(sourcePath,
                tmpDir.toPath());
        assertEquals("this/is/windows/path", relativeUnixPath,
                "Should replace windows path separator with unix path separator");
        Mockito.when(relativePath.toString()).thenReturn("this/is/unix/path");

        relativeUnixPath = FrontendUtils.getUnixRelativePath(sourcePath,
                tmpDir.toPath());
        assertEquals("this/is/unix/path", relativeUnixPath,
                "Should keep the same path when it uses unix path separator");
    }

    @Test
    public synchronized void getVaadinHomeDirectory_noVaadinFolder_folderIsCreated()
            throws IOException {
        String originalHome = System.getProperty(USER_HOME);
        File home = Files.createTempDirectory(tmpDir.toPath(), "tmp").toFile();
        System.setProperty(USER_HOME, home.getPath());
        try {
            File vaadinDir = new File(home, ".vaadin");
            if (vaadinDir.exists()) {
                FileUtils.deleteDirectory(vaadinDir);
            }
            File vaadinHomeDirectory = FrontendUtils.getVaadinHomeDirectory();
            assertTrue(vaadinHomeDirectory.exists());
            assertTrue(vaadinHomeDirectory.isDirectory());

            // access it one more time
            vaadinHomeDirectory = FrontendUtils.getVaadinHomeDirectory();
            assertEquals(".vaadin", vaadinDir.getName());
        } finally {
            System.setProperty(USER_HOME, originalHome);
        }
    }

    @Test
    public synchronized void getVaadinHomeDirectory_vaadinFolderIsAFile_throws()
            throws IOException {
        assertThrows(IllegalStateException.class, () -> {
            String originalHome = System.getProperty(USER_HOME);
            File home = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                    .toFile();
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
        });
    }

    @Test
    void commandToString_longCommand_resultIsWrapped() {
        List<String> command = Arrays.asList("./node/node",
                "./node_modules/webpack-dev-server/bin/webpack-dev-server.js",
                "--config", "./webpack.config.js", "--port 57799",
                "--env watchDogPort=57798", "-d", "--inline=false");
        String wrappedCommand = FrontendUtils.commandToString(".", command);
        assertEquals(
                """

                        ./node/node \\\s
                            ./node_modules/webpack-dev-server/bin/webpack-dev-server.js \\\s
                            --config ./webpack.config.js --port 57799 \\\s
                            --env watchDogPort=57798 -d --inline=false\s
                        """,
                wrappedCommand);
    }

    @Test
    void commandToString_commandContainsBaseDir_baseDirIsReplaced() {
        List<String> command = Arrays.asList("./node/node",
                "/somewhere/not/disclosable/node_modules/webpack-dev-server/bin/webpack-dev-server.js");
        String wrappedCommand = FrontendUtils
                .commandToString("/somewhere/not/disclosable", command);
        assertEquals(
                """

                        ./node/node \\\s
                            ./node_modules/webpack-dev-server/bin/webpack-dev-server.js\s
                        """,
                wrappedCommand);
    }

    @Test
    void deleteNodeModules_nopIfNotExists() throws IOException {
        File nodeModules = new File(tmpDir, "node_modules");
        FrontendUtils.deleteNodeModules(nodeModules);
    }

    @Test
    void deleteNodeModules_throwsIfNotNamedNodeModules() throws IOException {
        assertThrows(IOException.class, () -> {
            File myModules = new File(tmpDir, "my_modules");
            myModules.mkdirs();
            FrontendUtils.deleteNodeModules(myModules);
        });
    }

    @Test
    void deleteNodeModules_canDeleteSymlinksAndNotFollowThem()
            throws IOException {

        // Test fails on Windows due to UAC FileSystemException
        assumeFalse(FrontendUtils.isWindows());

        File externalDir = new File(tmpDir, "external");
        File externalLicense = new File(externalDir, "LICENSE");

        externalLicense.getParentFile().mkdirs();
        externalLicense.createNewFile();

        File nodeModules = new File(tmpDir, "node_modules");
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

        assertTrue(nodeModules.exists());
        assertTrue(linking.exists());
        assertTrue(new File(linking, "LICENSE").exists());
        assertTrue(new File(linkingExternal, "LICENSE").exists());

        FrontendUtils.deleteNodeModules(nodeModules);

        assertFalse(nodeModules.exists());
        assertTrue(externalLicense.exists());
    }

    @Test
    void symlinkByNpm_deleteDirectory_doesNotDeleteSymlinkFolderFiles()
            throws IOException, ExecutionFailedException {
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();

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

        assertTrue(linkFolderFile.exists(),
                "Linked folder contents should not be removed.");
    }

    @Test
    void consumeProcessStreams_streamsConsumed() throws Exception {

        Pair<String, String> streams = executeExternalProcess("STDOUT", "Test",
                "text");
        String stdOut = streams.getFirst();
        String stdErr = streams.getSecond();
        assertTrue(stdOut.contains("STDOUT, Test, text"),
                "Unexpected STDOUT contents: " + stdOut);
        assertTrue(stdErr.isBlank(),
                "Expected STDERR to be empty, but was " + stdErr);

        streams = executeExternalProcess("STDERR", "Test", "text");
        stdOut = streams.getFirst();
        stdErr = streams.getSecond();
        assertTrue(stdOut.isBlank(),
                "Expected STDOUT to be empty, but was " + stdOut);
        assertTrue(stdErr.contains("STDERR, Test, text"),
                "Unexpected STDERR contents: " + stdErr);

        streams = executeExternalProcess("BOTH", "Test", "text");
        stdOut = streams.getFirst();
        stdErr = streams.getSecond();
        assertTrue(stdOut.contains("STDOUT: BOTH, Test, text"),
                "Unexpected STDERR contents: " + stdOut);
        assertTrue(stdErr.contains("STDERR: BOTH, Test, text"),
                "Unexpected STDERR contents: " + stdErr);

        streams = executeExternalProcess("THROW EXCEPTION");
        stdOut = streams.getFirst();
        stdErr = streams.getSecond();
        assertTrue(stdOut.isBlank(),
                "Expected STDOUT to be empty, but was " + stdOut);
        assertTrue(
                stdErr.contains("RuntimeException")
                        && stdErr.contains("Invalid stream THROW EXCEPTION"),
                "Unexpected STDERR contents: " + stdErr);
    }

    @Test
    void isReactRouterRequired_importsVaadinRouter_false() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.INDEX_TS,
                """
                            import { Router } from '@vaadin/router';
                            import { routes } from './routes';

                            export const router = new Router(document.querySelector('#outlet'));
                            router.setRoutes(routes);
                        """);
        assertFalse(FrontendUtils.isReactRouterRequired(frontend),
                "vaadin-router expected when it imported");
    }

    @Test
    void isReactRouterRequired_doesntImportVaadinRouter_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.INDEX_TS,
                """
                            import { createElement } from "react";
                            import { createRoot } from "react-dom/client";
                            import App from "./App.js";

                            createRoot(document.getElementById("outlet")!).render(createElement(App));
                        """);
        assertTrue(FrontendUtils.isReactRouterRequired(frontend),
                "react-router expected when no vaadin-router imported");
    }

    @Test
    void isReactRouterRequired_noIndexTsFile_true() throws IOException {
        File frontend = new File(tmpDir, FrontendUtils.DEFAULT_FRONTEND_DIR);
        frontend.mkdirs();
        assertTrue(FrontendUtils.isReactRouterRequired(frontend),
                "react-router expected when index.ts isn't there");
    }

    @Test
    void isHillaViewsUsed_onlyServerSideRoutesTs_false() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_ONLY_SERVERSIDE_ROUTES);
        assertFalse(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are not expected");
    }

    @Test
    void isHillaViewsUsed_onlyServerSideRoutesTsx_false() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_ONLY_SERVERSIDE_ROUTES);
        assertFalse(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are not expected");
    }

    @Test
    void isHillaViewsUsed_onlyClientSideRoutesTs_true() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_ONLY_CLIENTSIDE_ROUTES);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_onlyClientSideRoutesTsx_true() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_ONLY_CLIENTSIDE_ROUTES);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesTs1_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_1);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesTsx1_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_1);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesTs2_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_2);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesTsx2_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_2);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesTs3_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TS,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_3);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesTsx3_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_3);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_clientAndServerSideRoutesMainLayoutTsx_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_CLIENT_AND_SERVER_SIDE_ROUTES_MAINLAYOUT_TSX);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_serverSideRoutesMainLayoutTsx_true()
            throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_SERVER_SIDE_ROUTES_MAINLAYOUT_TSX);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected");
    }

    @Test
    void isHillaViewsUsed_withFileRoutes_true() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_WITH_FILE_ROUTES);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected, as withFileRoutes is used");
    }

    @Test
    void isHillaViewsUsed_withReactRoutes_true() throws IOException {
        File frontend = prepareFrontendForRoutesFile(FrontendUtils.ROUTES_TSX,
                ROUTES_CONTENT_WITH_WITH_REACT_ROUTES);
        assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                "hilla-views are expected, as withReactRoutes is used");
    }

    @Test
    void isHillaViewsUsed_nonEmptyHillaViewInViews_true() throws IOException {
        File frontend = null;
        for (String ext : Arrays.asList(".js", ".jsx", ".ts", ".tsx")) {
            try {
                frontend = prepareFrontendForRoutesFile(
                        FrontendUtils.HILLA_VIEWS_PATH + "/HillaView" + ext,
                        HILLA_VIEW_TSX);
                assertTrue(FrontendUtils.isHillaViewsUsed(frontend),
                        "hilla view is present, thus Hilla is expected");
            } finally {
                if (frontend != null && frontend.exists()) {
                    FileUtils.deleteQuietly(frontend);
                }
            }
        }
    }

    @Test
    void isHillaViewsUsed_emptyHillaViewContent_false() throws IOException {
        File frontend = prepareFrontendForRoutesFile(
                FrontendUtils.HILLA_VIEWS_PATH + "/HillaView.ts", "//comment");
        assertFalse(FrontendUtils.isHillaViewsUsed(frontend),
                "empty Hilla view, Hilla not expected");
    }

    @Test
    void isHillaViewsUsed_noViews_false() throws IOException {
        File frontend = prepareFrontendForRoutesFile(
                FrontendUtils.HILLA_VIEWS_PATH + "/foo.css", "some css");
        assertFalse(FrontendUtils.isHillaViewsUsed(frontend),
                "no Hilla views, Hilla not expected");
    }

    @Test
    void platformVersion_returnsExpectedVersion() throws IOException {
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();
        File versionJsonFile = new File(npmFolder, "versions.json");
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        //@formatter:off
        String versionJsonString = "{"
                + "  \"platform\": \"21.0.0\"\n"
                + "}\n";
        //@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);

        Optional<String> vaadinVersion = FrontendBuildUtils
                .getVaadinVersion(finder);

        assertTrue(vaadinVersion.isPresent(),
                "versions.json should have had the platform field");
        assertEquals("21.0.0", vaadinVersion.get(), "Received faulty version");

        //@formatter:off
        versionJsonString = "{"
                + "}\n";
        //@formatter:on
        FileUtils.write(versionJsonFile, versionJsonString,
                StandardCharsets.UTF_8);
        vaadinVersion = FrontendBuildUtils.getVaadinVersion(finder);

        assertFalse(vaadinVersion.isPresent(),
                "versions.json should not contain platform version");
    }

    @Test
    void noVersionsJson_getVersionsDoesntThrow() throws IOException {
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();
        File versionJsonFile = new File(npmFolder, "versions.json");
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(null);

        Optional<String> vaadinVersion = FrontendBuildUtils
                .getVaadinVersion(finder);

        assertFalse(vaadinVersion.isPresent(),
                "versions.json should not contain platform version");
    }

    @Test
    void platformMajorVersionVersionUpdated_returnsTrueOnlyForMajorVersionChange_inNodeModulesVersion()
            throws IOException {
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();
        File versionJsonFile = new File(npmFolder, "versions.json");
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        //@formatter:off
        String versionJsonString = "{"
                + "  \"platform\": \"21.1.0\"\n"
                + "}\n";
        //@formatter:on
        Files.writeString(versionJsonFile.toPath(), versionJsonString,
                StandardCharsets.UTF_8);

        File nodeModules = new File(npmFolder, "node_modules");
        File projectVaadinJson = new File(nodeModules, ".vaadin/vaadin.json");
        Files.createDirectories(projectVaadinJson.getParentFile().toPath());
        String projectVersionString = """
                        {
                          "vaadinVersion" : "21.0.0"
                        }
                """;

        Files.writeString(projectVaadinJson.toPath(), projectVersionString,
                StandardCharsets.UTF_8);

        assertFalse(
                FrontendBuildUtils.isPlatformMajorVersionUpdated(finder,
                        npmFolder, nodeModules, npmFolder),
                "Change in minor version should return false");

        //@formatter:off
        versionJsonString = "{"
                + "  \"platform\": \"22.0.0\"\n"
                + "}\n";
        //@formatter:on
        Files.writeString(versionJsonFile.toPath(), versionJsonString,
                StandardCharsets.UTF_8);

        assertTrue(
                FrontendBuildUtils.isPlatformMajorVersionUpdated(finder,
                        npmFolder, nodeModules, npmFolder),
                "Change in major version should return true");
    }

    @Test
    void platformMajorVersionVersionUpdated_returnsTrueOnlyForMajorVersionChange_inBundleVersion()
            throws IOException {
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();
        File versionJsonFile = new File(npmFolder, "versions.json");
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        //@formatter:off
        String versionJsonString = "{"
                + "  \"platform\": \"21.1.0\"\n"
                + "}\n";
        //@formatter:on
        Files.writeString(versionJsonFile.toPath(), versionJsonString,
                StandardCharsets.UTF_8);

        File nodeModules = new File(npmFolder, "node_modules");
        File buildFolder = new File(npmFolder, "target");
        File bundleFolder = new File(buildFolder, "dev-bundle");
        Files.createDirectories(bundleFolder.toPath());
        File bundleVaadinJson = new File(bundleFolder, "vaadin.json");
        String bundleVersionString = """
                        {
                          "vaadinVersion" : "21.0.0"
                        }
                """;

        Files.writeString(bundleVaadinJson.toPath(), bundleVersionString,
                StandardCharsets.UTF_8);

        assertFalse(
                FrontendBuildUtils.isPlatformMajorVersionUpdated(finder,
                        npmFolder, nodeModules, buildFolder),
                "Change in minor version should return false");

        //@formatter:off
        versionJsonString = "{"
                + "  \"platform\": \"22.0.0\"\n"
                + "}\n";
        //@formatter:on
        Files.writeString(versionJsonFile.toPath(), versionJsonString,
                StandardCharsets.UTF_8);

        assertTrue(
                FrontendBuildUtils.isPlatformMajorVersionUpdated(finder,
                        npmFolder, nodeModules, buildFolder),
                "Change in major version should return true");
    }

    @Test
    void platformMajorVersionVersionUpdated_bundleVersionIsCheckedOverNodeModulesVersion()
            throws IOException {
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();
        File versionJsonFile = new File(npmFolder, "versions.json");
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResource(Constants.VAADIN_CORE_VERSIONS_JSON))
                .thenReturn(versionJsonFile.toURI().toURL());

        //@formatter:off
        String versionJsonString = "{"
                + "  \"platform\": \"21.1.0\"\n"
                + "}\n";
        //@formatter:on
        Files.writeString(versionJsonFile.toPath(), versionJsonString,
                StandardCharsets.UTF_8);

        File nodeModules = new File(npmFolder, "node_modules");
        File buildFolder = new File(npmFolder, "target");
        File bundleFolder = new File(buildFolder, "dev-bundle");
        Files.createDirectories(bundleFolder.toPath());
        File bundleVaadinJson = new File(bundleFolder, "vaadin.json");
        String bundleVersionString = """
                        {
                          "vaadinVersion" : "21.0.0"
                        }
                """;

        Files.writeString(bundleVaadinJson.toPath(), bundleVersionString,
                StandardCharsets.UTF_8);

        File projectVaadinJson = new File(nodeModules, ".vaadin/vaadin.json");
        Files.createDirectories(projectVaadinJson.getParentFile().toPath());
        String projectVersionString = """
                        {
                          "vaadinVersion" : "20.0.0"
                        }
                """;

        Files.writeString(projectVaadinJson.toPath(), projectVersionString,
                StandardCharsets.UTF_8);

        assertFalse(
                FrontendBuildUtils.isPlatformMajorVersionUpdated(finder,
                        npmFolder, nodeModules, buildFolder),
                "Change in minor version should return false");
    }

    @Test
    void isTailwindCssEnabled_withOptions() throws IOException {
        FeatureFlags featureFlags = Mockito.mock(FeatureFlags.class);
        Mockito.doReturn(true).when(featureFlags)
                .isEnabled(CoreFeatureFlagProvider.TAILWIND_CSS);
        File npmFolder = Files.createTempDirectory(tmpDir.toPath(), "tmp")
                .toFile();
        Options options = new MockOptions(npmFolder)
                .withFeatureFlags(featureFlags);
        assertTrue(FrontendBuildUtils.isTailwindCssEnabled(options),
                "Expected TailwindCSS to be enabled when feature flag is set in Node tasks options");
    }

    private File prepareFrontendForRoutesFile(String fileName, String content)
            throws IOException {
        return prepareFrontendForRoutesFile(fileName, content, false);
    }

    private File prepareFrontendForRoutesFile(String fileName, String content,
            boolean generateDummyFSView) throws IOException {
        File frontend = new File(tmpDir, FrontendUtils.DEFAULT_FRONTEND_DIR);
        frontend.mkdirs();
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
