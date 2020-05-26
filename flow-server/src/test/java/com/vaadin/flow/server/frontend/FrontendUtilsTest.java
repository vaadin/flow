/*
 * Copyright 2000-2020 Vaadin Ltd.
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
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinService;

import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FrontendUtilsTest {

    private static final String USER_HOME = "user.home";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

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
    public void assetsByChunkIsCorrectlyParsedFromStats() throws IOException {
        VaadinService service = setupStatsAssetMocks("ValidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals(
                "{\"bundle\": \"build/vaadin-bundle-1111.cache.js\",\"export\": \"build/vaadin-export-2222.cache.js\"}",
                statsAssetsByChunkName);
    }

    @Test
    public void formattingError_assetsByChunkIsCorrectlyParsedFromStats()
            throws IOException {
        VaadinService service = setupStatsAssetMocks("MissFormatStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals(
                "{\"bundle\": \"build/vaadin-bundle-1111.cache.js\"}",
                statsAssetsByChunkName);
    }

    @Test
    public void noStatsFile_assetsByChunkReturnsNull() throws IOException {
        VaadinService service = getServiceWithResource(null);

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertNull(statsAssetsByChunkName);
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
    public void faultyStatsFileReturnsNull() throws IOException {
        VaadinService service = setupStatsAssetMocks("InvalidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertNull(statsAssetsByChunkName);
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
                "--watchDogPort=57798", "-d", "--inline=false");
        String wrappedCommand = FrontendUtils.commandToString(".", command);
        Assert.assertEquals("\n" + "./node/node \\ \n"
                + "    ./node_modules/webpack-dev-server/bin/webpack-dev-server.js \\ \n"
                + "    --config ./webpack.config.js --port 57799 \\ \n"
                + "    --watchDogPort=57798 -d --inline=false \n", wrappedCommand);
    }

    @Test
    public void commandToString_commandContainsBaseDir_baseDirIsReplaced() {
        List<String> command = Arrays.asList("./node/node",
                "/somewhere/not/disclosable/node_modules/webpack-dev-server/bin/webpack-dev-server.js");
        String wrappedCommand = FrontendUtils
                .commandToString("/somewhere/not/disclosable", command);
        Assert.assertEquals("\n" + "./node/node \\ \n"
                + "    ./node_modules/webpack-dev-server/bin/webpack-dev-server.js \n",
                wrappedCommand);
    }

    private VaadinService setupStatsAssetMocks(String statsFile)
            throws IOException {
        String stats = IOUtils.toString(FrontendUtilsTest.class.getClassLoader()
                .getResourceAsStream(statsFile), StandardCharsets.UTF_8);

        return getServiceWithResource(
                new ByteArrayInputStream(stats.getBytes()));
    }

    private VaadinService getServiceWithResource(InputStream stats) {
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
                .thenReturn(stats);
        return service;
    }

}
