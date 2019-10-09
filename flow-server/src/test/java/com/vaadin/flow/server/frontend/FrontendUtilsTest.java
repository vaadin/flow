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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendUtils.UnknownVersionException;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
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
        assertThat(FrontendUtils.getNpmExecutable(baseDir)
                .get(0), containsString("npm"));
        assertThat(FrontendUtils.getNodeExecutable(baseDir),
                not(containsString(NPM_CLI_STRING)));
        assertEquals(1, FrontendUtils
                .getNpmExecutable(baseDir).size());
    }

    @Test
    public void parseValidVersions() throws UnknownVersionException {
        assertFalse(FrontendUtils.isVersionAtLeast("test",
                new String[] { "6", "0", "0" }, 10, 0));
        assertFalse(FrontendUtils.isVersionAtLeast("test",
                new String[] { "6", "0", "0" }, 6, 1));
        assertTrue(FrontendUtils.isVersionAtLeast("test",
                new String[] { "10", "0", "0" }, 10, 0));
        assertTrue(FrontendUtils.isVersionAtLeast("test",
                new String[] { "10", "0", "2" }, 10, 0));
        assertTrue(FrontendUtils.isVersionAtLeast("test",
                new String[] { "10", "2", "0" }, 10, 0));
    }

    @Test(expected = UnknownVersionException.class)
    public void parseInvalidMajorVersion() throws UnknownVersionException {
        FrontendUtils.isVersionAtLeast("test", new String[] { "6", "0b2", "0" },
                10, 0);
    }

    @Test(expected = UnknownVersionException.class)
    public void parseInvalidMinorVersion() throws UnknownVersionException {
        FrontendUtils.isVersionAtLeast("test", new String[] { "6", "0b2", "0" },
                10, 0);
    }

    @Test
    public void validateLargerThan_passesForNewVersion()
            throws UnknownVersionException {
        FrontendUtils.validateToolVersion("test",
                new String[] { "10", "0", "2" }, 10, 0, 10, 0);
        FrontendUtils.validateToolVersion("test",
                new String[] { "10", "1", "2" }, 10, 0, 10, 0);
        FrontendUtils.validateToolVersion("test",
                new String[] { "11", "0", "2" }, 10, 0, 10, 0);
    }

    @Test
    public void validateLargerThan_logsForSlightlyOldVersion()
            throws UnknownVersionException, UnsupportedEncodingException {
        PrintStream orgErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setErr(new PrintStream(out));
        try {
            FrontendUtils.validateToolVersion("test",
                    new String[] { "9", "0", "0" }, 10, 0, 8, 0);
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
    public void validateLargerThan_throwsForOldVersion()
            throws UnknownVersionException, UnsupportedEncodingException {
        try {
            FrontendUtils.validateToolVersion("test",
                    new String[] { "7", "5", "0" }, 10, 0, 8, 0);
            Assert.fail("No exception was thrown for old version");
        } catch (IllegalStateException e) {
            Assert.assertTrue(e.getMessage().contains(
                    "Your installed 'test' version (7.5.0) is too old. Supported versions are 10.0+"));
        }
    }

    @Test(expected = UnknownVersionException.class)
    public void validateLargerThan_invalidVersionThrows()
            throws UnknownVersionException {
        FrontendUtils.validateToolVersion("test",
                new String[] { "a", "b", "c" }, 10, 2, 10, 2);
    }

    @Test
    public void validateLargerThan_ignoredWithProperty()
            throws UnknownVersionException {
        try {
            System.setProperty("vaadin.ignoreVersionChecks", "true");
            FrontendUtils.validateToolVersion("test",
                    new String[] { "a", "b", "c" }, 10, 2, 10, 2);
        } finally {
            System.clearProperty("vaadin.ignoreVersionChecks");
        }
    }

    @Test
    public void parseValidToolVersions() throws IOException {
        Assert.assertArrayEquals(new String[] { "10", "11", "12" },
                FrontendUtils.parseVersion("v10.11.12"));
        Assert.assertArrayEquals(new String[] { "8", "0", "0" },
                FrontendUtils.parseVersion("v8.0.0"));
        Assert.assertArrayEquals(new String[] { "8", "0", "0" },
                FrontendUtils.parseVersion("8.0.0"));
        Assert.assertArrayEquals(new String[] { "6", "9", "0" }, FrontendUtils
                .parseVersion("Aktive Codepage: 1252\n" + "6.9.0\n" + ""));
    }

    @Test(expected = IOException.class)
    public void parseEmptyToolVersions() throws IOException {
        FrontendUtils.parseVersion(" \n");
    }

    @Test
    public void assetsByChunkIsCorrectlyParsedFromStats() throws IOException {
        File statsFile = tmpDir.newFile("stats.json");

        Files.write(statsFile.toPath(),
                Collections.singletonList("{\n" +
                        "  \"errors\": [],\n" +
                        "  \"warnings\": [],\n" +
                        "  \"version\": \"4.29.1\",\n" +
                        "  \"hash\": \"64bb80639ef116681818\",\n" +
                        "  \"time\": 1148,\n" +
                        "  \"builtAt\": 1549540586721,\n" +
                        "  \"publicPath\": \"\",\n" +
                        "  \"outputPath\": \"/Volumes/Framework/updates/skeleton-starter-flow/src/main/webapp/frontend/dist\",\n" +
                        "  \"assetsByChunkName\" :{\n" +
                        "    \"index\": \"build/index-1111.cache.js\",\n" +
                        "    \"index.es5\": \"build/index.es5-2222.cache.js\"\n" +
                        "  },\n" +
                        "  \"assets\": [\n" +
                        "    {\n" +
                        "      \"name\": \"0.fragment.js\",\n" +
                        "      \"size\": 618382,\n" +
                        "      \"chunks\": [\n" +
                        "        0\n" +
                        "      ],\n" +
                        "      \"chunkNames\": [],\n" +
                        "      \"emitted\": true\n" +
                        "    }\n" +
                        "]\n" +
                        "}\n"));

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
        Mockito.when(classLoader.getResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(statsFile.toURI().toURL());


        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals("{" +
                "\"index\": \"build/index-1111.cache.js\"," +
                "\"index.es5\": \"build/index.es5-2222.cache.js\"" +
                "}", statsAssetsByChunkName);
    }


    @Test
    public void faultyStatsFileReturnsNull() throws IOException {
        File statsFile = tmpDir.newFile("stats.json");

        Files.write(statsFile.toPath(),
                Collections.singletonList("{\n" +
                        "  \"errors\": [],\n" +
                        "  \"warnings\": [],\n" +
                        "  \"version\": \"4.29.1\",\n" +
                        "  \"hash\": \"64bb80639ef116681818\",\n" +
                        "  \"time\": 1148,\n" +
                        "  \"builtAt\": 1549540586721,\n" +
                        "  \"publicPath\": \"\",\n" +
                        "  \"outputPath\": \"/Volumes/Framework/updates/skeleton-starter-flow/src/main/webapp/frontend/dist\",\n" +
                        "  \"assetsByChunkName\" :{\n" +
                        "    \"index\": \"build/index-1111.cache.js\",\n" +
                        "    \"index.es5\": \"build/index.es5-2222.cache.js\"\n" +
                        "{\n" +
                        "}\n" +
                        "  },\n" +
                        "  \"assets\": [\n" +
                        "    {\n" +
                        "      \"name\": \"0.fragment.js\",\n" +
                        "      \"size\": 618382,\n" +
                        "      \"chunks\": [\n" +
                        "        0\n" +
                        "      ],\n" +
                        "      \"chunkNames\": [],\n" +
                        "      \"emitted\": true\n" +
                        "    }\n" +
                        "]\n" +
                        "}\n"));

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
        Mockito.when(classLoader.getResource(
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn(statsFile.toURI().toURL());


        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertNull(statsAssetsByChunkName);
    }
}
