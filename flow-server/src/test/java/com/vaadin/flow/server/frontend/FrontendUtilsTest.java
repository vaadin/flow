/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.checkForFaultyNpmVersion;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubNode;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FrontendUtilsTest {

    public static final String DEFAULT_NODE = FrontendUtils.isWindows()
            ? "node\\node.exe"
            : "node/node";

    public static final String NPM_CLI_STRING = Stream
            .of("node", "node_modules", "npm", "bin", "npm-cli.js")
            .collect(Collectors.joining(File.separator));

    public static final String PNPM_INSTALL_LOCATION = Stream
            .of("node_modules","pnpm","bin","pnpm.js")
            .collect(Collectors.joining(File.separator));

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private String baseDir;

    @Before
    public void setup() {
        baseDir = tmpDir.getRoot().getAbsolutePath();
    }

    @Test
    public void should_useProjectNodeFirst() throws Exception {
        if (FrontendUtils.isWindows()) {
            LoggerFactory.getLogger(FrontendUtilsTest.class).info(
                    "Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.");
            return;
        }
        createStubNode(true, true, baseDir);

        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                containsString(DEFAULT_NODE));
        assertThat(FrontendUtils.getNpmExecutable(baseDir).get(0),
                containsString(DEFAULT_NODE));
        assertThat(FrontendUtils.getNpmExecutable(baseDir).get(1),
                containsString(NPM_CLI_STRING));
    }

    @Test
    public void should_useProjectNpmFirst() throws Exception {
        if (FrontendUtils.isWindows()) {
            LoggerFactory.getLogger(FrontendUtilsTest.class).info(
                    "Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.");
            return;
        }
        createStubNode(false, true, baseDir);

        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                containsString("node"));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(DEFAULT_NODE)));
        assertThat(FrontendUtils.getNpmExecutable(baseDir).get(0),
                containsString("node"));
        assertThat(FrontendUtils.getNpmExecutable(baseDir).get(1),
                containsString(NPM_CLI_STRING));
    }

    @Test
    public void should_useSystemNode() {
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                containsString("node"));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(DEFAULT_NODE)));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(NPM_CLI_STRING)));

        assertEquals(2, FrontendUtils.getNpmExecutable(baseDir).size());
        assertThat(FrontendUtils.getNpmExecutable(baseDir).get(0),
                containsString("npm"));
        assertThat(FrontendUtils.getNpmExecutable(baseDir).get(1),
                containsString("--no-update-notifier"));
    }

    @Test
    public void parseValidVersions() {
        FrontendVersion sixPointO = new FrontendVersion(6, 0);

        FrontendVersion requiredVersionTen = new FrontendVersion(10, 0);
        assertFalse(
                FrontendUtils.isVersionAtLeast(sixPointO, requiredVersionTen));
        assertFalse(FrontendUtils.isVersionAtLeast(sixPointO,
                new FrontendVersion(6, 1)));
        assertTrue(FrontendUtils.isVersionAtLeast(new FrontendVersion("10.0.0"),
                requiredVersionTen));
        assertTrue(FrontendUtils.isVersionAtLeast(new FrontendVersion("10.0.2"),
                requiredVersionTen));
        assertTrue(FrontendUtils.isVersionAtLeast(new FrontendVersion("10.2.0"),
                requiredVersionTen));
    }

    @Test
    public void validateLargerThan_passesForNewVersion() {
        FrontendUtils.validateToolVersion("test", new FrontendVersion("10.0.2"),
                new FrontendVersion(10, 0), new FrontendVersion(10, 0));
        FrontendUtils.validateToolVersion("test", new FrontendVersion("10.1.2"),
                new FrontendVersion(10, 0), new FrontendVersion(10, 0));
        FrontendUtils.validateToolVersion("test", new FrontendVersion("11.0.2"),
                new FrontendVersion(10, 0), new FrontendVersion(10, 0));
    }

    @Test
    public void validateLargerThan_logsForSlightlyOldVersion()
            throws UnsupportedEncodingException {
        PrintStream orgErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(out));
        try {
            FrontendUtils.validateToolVersion("test",
                    new FrontendVersion(9, 0, 0), new FrontendVersion(10, 0),
                    new FrontendVersion(8, 0));
            String logged = out.toString("utf-8")
                    // fix for windows
                    .replace("\r", "");
            Assert.assertTrue(logged.contains(
                    "Your installed 'test' version (9.0.0) is not supported but should still work. Supported versions are 10.0+\n"));
        } finally {
            System.setErr(orgErr);
        }
    }

    @Test
    public void validateLargerThan_throwsForOldVersion() {
        try {
            FrontendUtils.validateToolVersion("test",
                    new FrontendVersion(7, 5, 0), new FrontendVersion(10, 0),
                    new FrontendVersion(8, 0));
            Assert.fail("No exception was thrown for old version");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains(
                    "Your installed 'test' version (7.5.0) is too old. Supported versions are 10.0+"));
        }
    }

    @Test
    public void validateLargerThan_ignoredWithProperty() {
        try {
            System.setProperty("vaadin.ignoreVersionChecks", "true");
            FrontendUtils.validateToolVersion("test", new FrontendVersion(0, 0),
                    new FrontendVersion(10, 2), new FrontendVersion(10, 2));
        } finally {
            System.clearProperty("vaadin.ignoreVersionChecks");
        }
    }

    @Test
    public void parseValidToolVersions() throws IOException {
        Assert.assertEquals("10.11.12",
                FrontendUtils.parseVersionString("v10.11.12"));
        Assert.assertEquals("8.0.0",
                FrontendUtils.parseVersionString("v8.0.0"));
        Assert.assertEquals("8.0.0", FrontendUtils.parseVersionString("8.0.0"));
        Assert.assertEquals("6.9.0", FrontendUtils.parseVersionString(
                "Aktive Codepage: 1252\n" + "6.9.0\n" + ""));
    }

    @Test(expected = IOException.class)
    public void parseEmptyToolVersions() throws IOException {
        FrontendUtils.parseVersionString(" \n");
    }

    @Test
    public void knownFaultyNpmVersionThrowsException() {
        assertFaultyNpmVersion(new FrontendVersion(6, 11, 0));
        assertFaultyNpmVersion(new FrontendVersion(6, 11, 1));
        assertFaultyNpmVersion(new FrontendVersion(6, 11, 2));
    }

    private void assertFaultyNpmVersion(FrontendVersion version) {
        try {
            checkForFaultyNpmVersion(version);
            Assert.fail("No exception was thrown for bad npm version");
        } catch (IllegalStateException e) {
            Assert.assertTrue(
                    "Faulty version " + version.getFullVersion()
                            + " returned wrong exception message",
                    e.getMessage()
                            .contains("Your installed 'npm' version ("
                                    + version.getFullVersion()
                                    + ") is known to have problems."));
        }
    }

    @Test
    public void assetsByChunkIsCorrectlyParsedFromStats() throws IOException {
        VaadinService service = setupStatsAssetMocks("ValidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals("{" + "\"index\": \"build/index-1111.cache.js\","
                + "\"index.es5\": \"build/index.es5-2222.cache.js\"" + "}",
                statsAssetsByChunkName);
    }

    @Test
    public void formattingError_assetsByChunkIsCorrectlyParsedFromStats()
            throws IOException {
        VaadinService service = setupStatsAssetMocks("MissFormatStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals("{" + "\"index\": \"build/index-1111.cache.js\","
                + "\"index.es5\": \"build/index.es5-2222.cache.js\"" + "}",
                statsAssetsByChunkName);
    }

    @Test
    public void faultyStatsFileReturnsNull() throws IOException {
        VaadinService service = setupStatsAssetMocks("InvalidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertNull(statsAssetsByChunkName);
    }

    /**
     * This test doesn't do anything if pnpm is already installed (globally)
     * which is true e.g. for or CI servers (TC/bender).
     */
    @Test
    public void ensurePnpm_requestInstall_keepPackageJson_removePackageLock_ignoredPnpmExists_localPnpmIsRemoved()
            throws IOException {
        Assume.assumeTrue(FrontendUtils.getPnpmExecutable(baseDir,
                false).isEmpty());
        File packageJson = new File(baseDir, "package.json");
        FileUtils.writeStringToFile(packageJson, "{}",
                StandardCharsets.UTF_8);

        File packageLockJson = new File(baseDir, "package-lock.json");
        FileUtils.writeStringToFile(packageLockJson, "{}",
                StandardCharsets.UTF_8);

        FrontendUtils.ensurePnpm(baseDir);
        Assert.assertFalse(
                FrontendUtils.getPnpmExecutable(baseDir, false).isEmpty());

        // locally installed pnpm (via npm/pnpm) is removed
        Assert.assertFalse(new File("node_modules/pnpm").exists());

        Assert.assertEquals("{}", FileUtils.readFileToString(packageJson,
                StandardCharsets.UTF_8));
        Assert.assertFalse(packageLockJson.exists());
    }

    @Test
    public void getPnpmExecutable_executableIsAvailable() {
        List<String> executable = FrontendUtils.getPnpmExecutable(baseDir);
        // command line should contain --shamefully-hoist=true option
        Assert.assertTrue(executable.contains("--shamefully-hoist=true"));
        Assert.assertTrue(
                executable.stream().anyMatch(cmd -> cmd.contains("pnpm")));
    }

    // #7219
    @Test
    public void ensurePnpm_globalPnpmIsTooOld_localPnpmIsInstalled()
            throws IOException {
        // this unit test must be run on a Unix-like OS
        Assume.assumeFalse(FrontendUtils.isWindows());

        FrontendToolsLocator defaultFrontendToolsLocator = FrontendUtils.frontendToolsLocator;
        try {
            // given: an existing globally installed version of pnpm that is too
            // old
            FrontendUtils.frontendToolsLocator = new FrontendToolsLocator() {
                private final Path oldPnpm = Files.createTempFile("pnpm", "old",
                        PosixFilePermissions.asFileAttribute(
                                PosixFilePermissions.fromString("rwxrwxrwx")));
                {
                    Files.write(oldPnpm, Arrays.asList("#!/bin/sh",
                            "if [ $1 = '--version' ]; then echo '3.8.1'; fi"),
                            StandardCharsets.UTF_8);
                }

                public Optional<File> tryLocateTool(String toolName) {
                    return "pnpm".equals(toolName)
                            ? Optional.of(oldPnpm.toFile())
                            : super.tryLocateTool(toolName);
                }
            };

            // when
            FrontendUtils.ensurePnpm(baseDir);

            // then: pnpm is installed locally
            List<String> pnpmExecutable = FrontendUtils
                    .getPnpmExecutable(baseDir, false);
            Assert.assertTrue(pnpmExecutable.size() > 1);
            Assert.assertTrue(
                    pnpmExecutable.get(1).contains(PNPM_INSTALL_LOCATION));
        } finally {
            FrontendUtils.frontendToolsLocator = defaultFrontendToolsLocator;
        }
    }

    private VaadinService setupStatsAssetMocks(String statsFile)
            throws IOException {
        String stats = IOUtils.toString(FrontendUtilsTest.class.getClassLoader()
                .getResourceAsStream(statsFile), StandardCharsets.UTF_8);

        VaadinService service = Mockito.mock(VaadinService.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        DeploymentConfiguration deploymentConfiguration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(service.getClassLoader()).thenReturn(classLoader);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(deploymentConfiguration.getStringProperty(
                SERVLET_PARAMETER_STATISTICS_JSON,
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT);
        Mockito.when(classLoader.getResourceAsStream(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(new ByteArrayInputStream(stats.getBytes()));
        return service;
    }
}
