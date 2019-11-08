/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
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

    public static final String DEFAULT_NODE = FrontendUtils.isWindows() ?
            "node\\node.exe" :
            "node/node";

    public static final String NPM_CLI_STRING = Stream
            .of("node", "node_modules", "npm", "bin", "npm-cli.js")
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
            LoggerFactory.getLogger(FrontendUtilsTest.class).info("Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.");
            return;
        }
        createStubNode(true, true, baseDir);

        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                containsString(DEFAULT_NODE));
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(0), containsString(DEFAULT_NODE));
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(1), containsString(NPM_CLI_STRING));
    }

    @Test
    public void should_useProjectNpmFirst() throws Exception {
        if (FrontendUtils.isWindows()) {
            LoggerFactory.getLogger(FrontendUtilsTest.class).info("Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.");
            return;
        }
        createStubNode(false, true, baseDir);

        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                containsString("node"));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(DEFAULT_NODE)));
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(0), containsString("node"));
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(1), containsString(NPM_CLI_STRING));
    }

    @Test
    public void should_useSystemNode() {
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                containsString("node"));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(DEFAULT_NODE)));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(NPM_CLI_STRING)));

        assertEquals(2, FrontendUtils
                .getNpmExecutable(baseDir).size());
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(0), containsString("npm"));
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(1), containsString("--no-update-notifier"));
    }

    @Test
    public void parseValidVersions() {
        FrontendVersion sixPointO = new FrontendVersion(6, 0);

        FrontendVersion requiredVersionTen = new FrontendVersion(10, 0);
        assertFalse(
                FrontendUtils.isVersionAtLeast(sixPointO, requiredVersionTen));
        assertFalse(FrontendUtils
                .isVersionAtLeast(sixPointO, new FrontendVersion(6, 1)));
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
                    new FrontendVersion(9,0, 0), new FrontendVersion(10, 0),new FrontendVersion( 8, 0));
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
                    new FrontendVersion(7,5,0), new FrontendVersion(10, 0),new FrontendVersion(8, 0));
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
        Assert.assertEquals("8.0.0",
                FrontendUtils.parseVersionString("8.0.0"));
        Assert.assertEquals("6.9.0", FrontendUtils
                .parseVersionString("Aktive Codepage: 1252\n" + "6.9.0\n" + ""));
    }

    @Test(expected = IOException.class)
    public void parseEmptyToolVersions() throws IOException {
        FrontendUtils.parseVersionString(" \n");
    }

    @Test
    public void knownFaultyNpmVersionThrowsException() {
        assertFaultyNpmVersion(new FrontendVersion(6,11,0));
        assertFaultyNpmVersion(new FrontendVersion(6,11,1));
        assertFaultyNpmVersion(new FrontendVersion(6,11,2));
    }

    private void assertFaultyNpmVersion(FrontendVersion version) {
        try {
            checkForFaultyNpmVersion(version);
            Assert.fail("No exception was thrown for bad npm version");
        } catch (IllegalStateException e) {
            Assert.assertTrue("Faulty version "+version.getFullVersion()+" returned wrong exception message", e.getMessage().contains(
                    "Your installed 'npm' version ("+version.getFullVersion()+") is known to have problems."));
        }
    }

    @Test
    public void assetsByChunkIsCorrectlyParsedFromStats() throws IOException {
        VaadinService service = setupStatsAssetMocks("ValidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals("{" +
                "\"index\": \"build/index-1111.cache.js\"," +
                "\"index.es5\": \"build/index.es5-2222.cache.js\"" +
                "}", statsAssetsByChunkName);
    }

    @Test
    public void formattingError_assetsByChunkIsCorrectlyParsedFromStats() throws IOException {
        VaadinService service = setupStatsAssetMocks("MissFormatStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals("{" +
                "\"index\": \"build/index-1111.cache.js\"," +
                "\"index.es5\": \"build/index.es5-2222.cache.js\"" +
                "}", statsAssetsByChunkName);
    }

    @Test
    public void faultyStatsFileReturnsNull() throws IOException {
        VaadinService service = setupStatsAssetMocks("InvalidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertNull(statsAssetsByChunkName);
    }

    private VaadinService setupStatsAssetMocks(String statsFile) throws IOException {
        String stats = IOUtils.toString(
                FrontendUtilsTest.class.getClassLoader().getResourceAsStream(statsFile),
                StandardCharsets.UTF_8);

        VaadinService service = Mockito.mock(VaadinService.class);
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        DeploymentConfiguration deploymentConfiguration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(service.getClassLoader()).thenReturn(classLoader);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(deploymentConfiguration
                .getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                        VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT);
        Mockito.when(classLoader.getResourceAsStream(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(new ByteArrayInputStream(stats.getBytes()));
        return service;
    }
}
