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

import com.vaadin.flow.server.frontend.FrontendUtils.StatsCache;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
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

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static com.vaadin.flow.server.Constants.STATISTICS_JSON_DEFAULT;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_STATISTICS_JSON;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
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
            assertTrue(logged.contains(
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
            assertTrue(e.getMessage().contains(
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
        Assert.assertEquals("6.9.0", FrontendUtils.parseVersionString(
                "Aktive Codepage: 1252\n" + "6.9.0\n" + ""));
    }

    @Test(expected = IOException.class)
    public void parseEmptyToolVersions() throws IOException {
        FrontendUtils.parseVersionString(" \n");
    }

    @Test
    public void assetsByChunkIsCorrectlyParsedFromStats()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        VaadinService service = setupStatsAssetMocks(configuration,
                "ValidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals(
                "{\"bundle\": \"build/vaadin-bundle-1111.cache.js\",\"export\": \"build/vaadin-export-2222.cache.js\"}",
                statsAssetsByChunkName);
    }

    @Test
    public void formattingError_assetsByChunkIsCorrectlyParsedFromStats()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        VaadinService service = setupStatsAssetMocks(configuration,
                "MissFormatStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        Assert.assertEquals(
                "{\"bundle\": \"build/vaadin-bundle-1111.cache.js\"}",
                statsAssetsByChunkName);
    }

    @Test
    public void noStatsFile_assetsByChunkReturnsNull()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        VaadinService service = getServiceWithResource(configuration, null);

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        assertNull(statsAssetsByChunkName);
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
    public void faultyStatsFileReturnsNull()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        VaadinService service = setupStatsAssetMocks(configuration,
                "InvalidStats.json");

        String statsAssetsByChunkName = FrontendUtils
                .getStatsAssetsByChunkName(service);

        assertNull(statsAssetsByChunkName);
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
            assertTrue(vaadinHomeDirectory.exists());
            assertTrue(vaadinHomeDirectory.isDirectory());

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
        Assert.assertEquals("\n" + "./node/node \\ \n"
                + "    ./node_modules/webpack-dev-server/bin/webpack-dev-server.js \\ \n"
                + "    --config ./webpack.config.js --port 57799 \\ \n"
                + "    --env watchDogPort=57798 -d --inline=false \n",
                wrappedCommand);
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

    @Test
    public void parseManifestJson_returnsValidPaths() {
        String manifestJson = "{\"index.html\": \"index.html\", \"sw.js\": "
                + "\"sw.js\", \"favicon.ico\": \"favicon.ico\", \"index.ts\": "
                + "\"VAADIN/build/vaadin-bundle-index.js\"}";
        List<String> manifestPaths = FrontendUtils
                .parseManifestPaths(manifestJson);
        assertTrue("Should list bundle path",
                manifestPaths.contains("/VAADIN/build/vaadin-bundle-index.js"));
        assertTrue("Should list /sw.js", manifestPaths.contains("/sw.js"));
        assertTrue("Should list /favicon.ico",
                manifestPaths.contains("/favicon.ico"));
        assertFalse("Should not list /index.html",
                manifestPaths.contains("/index.html"));
    }

    @Test
    public void getStatsContent_getStatsFromClassPath_delegateToGetApplicationResource()
            throws IOException {
        VaadinServletService service = mockServletService();

        ResourceProvider provider = mockResourceProvider(service);

        FrontendUtils.getStatsContent(service);

        VaadinServlet servlet = service.getServlet();

        Mockito.verify(provider).getApplicationResource("foo");
    }

    @Test
    public void getStatsAssetsByChunkName_getStatsFromClassPath_delegateToGetApplicationResource()
            throws IOException {
        VaadinServletService service = mockServletService();

        ResourceProvider provider = mockResourceProvider(service);

        FrontendUtils.getStatsAssetsByChunkName(service);

        VaadinServlet servlet = service.getServlet();

        Mockito.verify(provider).getApplicationResource("foo");
    }

    @Test
    public void getStatsContent_StatsCachedInProductionMode()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        configuration.setStatsExternal(false);
        VaadinService service = setupStatsAssetMocks(configuration,
                "ValidStats.json");

        assertTrue("getStatsContent in production mode returns stats.json",
                FrontendUtils.getStatsContent(service)
                        .contains("64bb80639ef116681818"));
        StatsCache cache = service.getContext().getAttribute(StatsCache.class);
        assertNotNull("getStatsContent in production mode populates cache",
                cache);
        assertNotNull(
                "getStatsContent in production mode populates cache with value",
                cache.statsJson);
    }

    @Test
    public void getStatsContent_StatsNotCachedInDevelopmentMode()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(false);
        configuration.setStatsExternal(false);
        configuration.setEnableDevServer(false); // has to be false, there is no
                                                 // webpack server
        VaadinService service = setupStatsAssetMocks(configuration,
                "ValidStats.json");

        assertTrue("getStatsContent in production mode returns stats.json",
                FrontendUtils.getStatsContent(service)
                        .contains("64bb80639ef116681818"));
        StatsCache cache = service.getContext().getAttribute(StatsCache.class);
        assertNull(
                "getStatsContent in development mode does not populate cache",
                cache);
    }

    @Test
    public void getStatsAssetsByChunkName_AssetChunksCachedInProductionMode()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(true);
        configuration.setStatsExternal(false);
        VaadinService service = setupStatsAssetMocks(configuration,
                "ValidStats.json");

        assertTrue(
                "getStatsAssetsByChunkName in production mode returns asset chunks",
                FrontendUtils.getStatsAssetsByChunkName(service)
                        .contains("vaadin-bundle-1111.cache.js"));
        StatsCache cache = service.getContext().getAttribute(StatsCache.class);
        assertNotNull(
                "getStatsAssetsByChunkName in production mode populates cache",
                cache);
        assertNotNull(
                "getStatsAssetsByChunkName in production mode populates cache with value",
                cache.statsJson);
    }

    @Test
    public void getStatsAssetsByChunkName_AssetChunksNotCachedInDevelopmentMode()
            throws IOException, ServiceException {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(false);
        configuration.setStatsExternal(false);
        configuration.setEnableDevServer(false); // has to be false, there is no
                                                 // webpack server
        VaadinService service = setupStatsAssetMocks(configuration,
                "ValidStats.json");

        assertTrue(
                "getStatsAssetsByChunkName in production mode returns asset chunks",
                FrontendUtils.getStatsAssetsByChunkName(service)
                        .contains("vaadin-bundle-1111.cache.js"));
        StatsCache cache = service.getContext().getAttribute(StatsCache.class);
        assertNull(
                "getStatsAssetsByChunkName in development mode does not populate cache",
                cache);
    }

    @Test
    public void StatsCache_createCacheFromInputstream() throws IOException {
        StatsCache statsCache = new StatsCache(
                getStatsContentAsStream("ValidStats.json"));
        assertNotNull("ValidStats.json should be stored", statsCache.statsJson);
        assertNotNull("Asset Chunks should be stored", statsCache.assetChunks);
        assertNull("Last Modified isn't set with internal files",
                statsCache.lastModified);

        statsCache = new StatsCache(getStatsContentAsStream("ValidStats.json"),
                "Tue, 3 Jun 2008 11:05:30 GMT");
        assertNotNull("ValidStats.json should be stored", statsCache.statsJson);
        assertNotNull("Asset Chunks should be stored", statsCache.assetChunks);
        assertNotNull("Last Modified is set with external files",
                statsCache.lastModified);

        statsCache = new StatsCache(getStatsContentAsStream("ValidStats.json"),
                null);
        assertNotNull("ValidStats.json should be stored", statsCache.statsJson);
        assertNotNull("Asset Chunks should be stored", statsCache.assetChunks);
        assertNull(
                "Last Modified is NOT set with external files without last modified",
                statsCache.lastModified);

        statsCache = new StatsCache(getStatsContentAsStream("ValidStats.json"),
                "Tue, 3 Jun 2008 11:05:30 GMT");
        assertNotNull("ValidStats.json should be stored", statsCache.statsJson);
        assertNotNull("Asset Chunks should be stored", statsCache.assetChunks);
        assertNotNull("Last Modified is set with external files",
                statsCache.lastModified);

        statsCache = new StatsCache(
                getStatsContentAsStream("InvalidStats.json"));
        assertNotNull("InvalidStats.json should be stored",
                statsCache.statsJson);
        assertNull("Invalid Asset Chunks should NOT be stored",
                statsCache.assetChunks);
        assertNull("Last Modified isn't set with internal files",
                statsCache.lastModified);

        statsCache = new StatsCache(
                getStatsContentAsStream("InvalidStats.json"),
                "Tue, 3 Jun 2008 11:05:30 GMT");
        assertNotNull("InvalidStats.json should be stored",
                statsCache.statsJson);
        assertNull("Invalid Asset Chunks should NOT be stored",
                statsCache.assetChunks);
        assertNotNull("Last Modified is set with external files",
                statsCache.lastModified);

        assertThrows("Creating StatsCache without Inputstream throws NPE",
                NullPointerException.class, () -> new StatsCache(null));
        assertThrows("Creating StatsCache without Inputstream throws NPE",
                NullPointerException.class,
                () -> new StatsCache(null, "Tue, 3 Jun 2008 11:05:30 GMT"));
    }

    @Test
    public void StatsCache_isCacheStillValid() throws IOException {
        assertFalse("NOT_CACHEABLE is always invalid",
                StatsCache.NOT_CACHEABLE.isCacheStillValid(null));
        assertFalse("NOT_CACHEABLE is always invalid", StatsCache.NOT_CACHEABLE
                .isCacheStillValid("Tue, 3 Jun 2008 11:05:30 GMT"));

        StatsCache internalCache = new StatsCache(
                getStatsContentAsStream("ValidStats.json"));
        // External files always have precedence over internal cache
        assertFalse(
                "Cached internal stats.json is NOT valid when external Stats.json has NO last modified header",
                internalCache.isCacheStillValid(null));
        assertFalse(
                "Cached internal stats.json is NOT valid when external Stats.json has a last modified header",
                internalCache
                        .isCacheStillValid("Tue, 3 Jun 2008 11:05:30 GMT"));

        StatsCache externalCache = new StatsCache(
                getStatsContentAsStream("ValidStats.json"),
                "Tue, 3 Jun 2008 11:05:30 GMT");
        assertTrue(
                "Cached external stats.json is valid when new external Stats.json has NO last modified header",
                externalCache.isCacheStillValid(null));
        assertTrue(
                "Cached external stats.json is valid when new external Stats.json has the SAME last modified header",
                externalCache
                        .isCacheStillValid("Tue, 3 Jun 2008 11:05:30 GMT"));
        assertTrue(
                "Cached external stats.json is valid when new external Stats.json has the OLDER last modified header",
                externalCache
                        .isCacheStillValid("Sun, 1 Jun 2008 11:05:30 GMT"));
        assertFalse(
                "Cached external stats.json is NOT valid when new external Stats.json has the NEWER last modified header",
                externalCache
                        .isCacheStillValid("Thu, 5 Jun 2008 11:05:30 GMT"));
    }

    @Test
    public void StatsCache_parseLastModified() {
        assertNull("Parsing null results in null.",
                StatsCache.parseLastModified(null));
        assertNotNull("Parsing valid date returns object.",
                StatsCache.parseLastModified("Tue, 3 Jun 2008 11:05:30 GMT"));
        assertThrows("Parsing invalid string throws exception",
                DateTimeParseException.class,
                () -> StatsCache.parseLastModified("invalid"));
    }

    private ResourceProvider mockResourceProvider(VaadinService service) {
        DeploymentConfiguration config = Mockito
                .mock(DeploymentConfiguration.class);

        VaadinContext context = Mockito.mock(VaadinContext.class);
        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(context.getAttribute(Lookup.class)).thenReturn(lookup);

        ResourceProvider provider = Mockito.mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(provider);

        Mockito.when(service.getDeploymentConfiguration()).thenReturn(config);
        Mockito.when(service.getContext()).thenReturn(context);

        Mockito.when(config.isProductionMode()).thenReturn(true);

        Mockito.when(config.getStringProperty(SERVLET_PARAMETER_STATISTICS_JSON,
                VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                .thenReturn("foo");
        return provider;
    }

    private InputStream getStatsContentAsStream(String statsFile) {
        return FrontendUtilsTest.class.getClassLoader()
                .getResourceAsStream(statsFile);
    }

    private VaadinService setupStatsAssetMocks(
            MockDeploymentConfiguration config, String statsFile)
            throws IOException, ServiceException {
        String stats = IOUtils.toString(getStatsContentAsStream(statsFile),
                StandardCharsets.UTF_8);
        return getServiceWithResource(config, stats);
    }

    private VaadinService getServiceWithResource(
            MockDeploymentConfiguration config, String content)
            throws ServiceException, IOException {
        MockVaadinServletService service = new MockVaadinServletService(config);

        VaadinContext context = service.getContext();

        Lookup lookup = Mockito.mock(Lookup.class);
        context.setAttribute(Lookup.class, lookup);

        ResourceProvider provider = Mockito.mock(ResourceProvider.class);

        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(provider);

        if (content != null) {
            File tmpFile = tmpDir.newFile();
            try (FileOutputStream outputStream = new FileOutputStream(
                    tmpFile)) {
                IOUtils.write(content, outputStream, StandardCharsets.UTF_8);
            }
            VaadinServlet servlet = service.getServlet();
            Mockito.when(provider.getApplicationResource(
                    VAADIN_SERVLET_RESOURCES + STATISTICS_JSON_DEFAULT))
                    .thenReturn(tmpFile.toURI().toURL());
        }

        return service;
    }

    private VaadinServletService mockServletService() {
        VaadinServletService service = Mockito.mock(VaadinServletService.class);

        VaadinServlet servlet = Mockito.mock(VaadinServlet.class);
        Mockito.when(service.getServlet()).thenReturn(servlet);
        return service;
    }

}
