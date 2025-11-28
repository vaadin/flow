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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;
import com.vaadin.flow.testcategory.SlowTests;
import com.vaadin.flow.testutil.FrontendStubs;

import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;
import static com.vaadin.flow.testutil.FrontendStubs.resetFrontendToolsNodeCache;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@NotThreadSafe
@Category(SlowTests.class)
public class FrontendToolsTest {

    public static final String DEFAULT_NODE = FrontendUtils.isWindows()
            ? "node\\node.exe"
            : "node/node";

    public static final String NPM_CLI_STRING = FrontendUtils.isWindows()
            ? "node\\node_modules\\npm\\bin\\npm-cli.js"
            : "node/lib/node_modules/npm/bin/npm-cli.js";

    private static final String OLD_PNPM_VERSION = "4.5.0";

    private static final String SUPPORTED_PNPM_VERSION = "7.0.0";

    private String baseDir;

    private String vaadinHomeDir;

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public final TemporaryFolder tmpDirWithNpmrc = new TemporaryFolder();

    private final FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    private FrontendTools tools;
    private FrontendToolsSettings settings;

    @Before
    public void setup() throws Exception {
        // Reset static state to ensure clean test isolation
        resetFrontendToolsNodeCache();
        baseDir = tmpDir.newFolder().getAbsolutePath();
        vaadinHomeDir = tmpDir.newFolder().getAbsolutePath();
        settings = new FrontendToolsSettings(baseDir, () -> vaadinHomeDir);
        tools = new FrontendTools(settings);
    }

    @Test
    public void installNode_NodeIsInstalledToTargetDirectory()
            throws FrontendUtils.UnknownVersionException {
        // Force alternative node to install and set up activeNodeInstallation
        settings.setForceAlternativeNode(true);
        tools = new FrontendTools(settings);
        String nodeExecutable = tools.getNodeExecutable();
        Assert.assertNotNull(nodeExecutable);

        List<String> nodeVersionCommand = new ArrayList<>();
        nodeVersionCommand.add(nodeExecutable);
        nodeVersionCommand.add("--version");
        FrontendVersion node = FrontendUtils.getVersion("node",
                nodeVersionCommand);
        Assert.assertEquals(
                new FrontendVersion(FrontendTools.DEFAULT_NODE_VERSION)
                        .getFullVersion(),
                node.getFullVersion());

        // Now test npm with the installed node
        List<String> npmVersionCommand = new ArrayList<>(
                tools.getNpmExecutable());
        npmVersionCommand.add("--version");
        FrontendVersion npm = FrontendUtils.getVersion("npm",
                npmVersionCommand);
        final FrontendVersion npmDefault = new FrontendVersion(
                FrontendTools.DEFAULT_NPM_VERSION);

        Assert.assertEquals("Major version should match",
                npmDefault.getMajorVersion(), npm.getMajorVersion());
        Assert.assertEquals("Minor version should match",
                npmDefault.getMinorVersion(), npm.getMinorVersion());
    }

    @Test
    public void nodeIsBeingLocated_updateTooOldNode_NodeInstalledToTargetDirectoryIsUpdated()
            throws FrontendUtils.UnknownVersionException {
        FrontendVersion updatedNodeVersion = getUpdatedAlternativeNodeVersion(
                "7.7.3", () -> tools.getNodeExecutable());

        Assert.assertEquals(
                "Failed to update the old Node version when being located",
                new FrontendVersion(FrontendTools.DEFAULT_NODE_VERSION)
                        .getFullVersion(),
                updatedNodeVersion.getFullVersion());
    }

    @Test
    public void forceAlternativeDirectory_updateTooOldNode_NodeInstalledToTargetDirectoryIsUpdated()
            throws FrontendUtils.UnknownVersionException {
        FrontendVersion updatedNodeVersion = getUpdatedAlternativeNodeVersion(
                "7.7.3", () -> tools.getNodeExecutable());

        Assert.assertEquals(
                "Failed to update the old Node version when alternative directory forced",
                new FrontendVersion(FrontendTools.DEFAULT_NODE_VERSION)
                        .getFullVersion(),
                updatedNodeVersion.getFullVersion());
    }

    private FrontendVersion getUpdatedAlternativeNodeVersion(
            String oldNodeVersion,
            SerializableSupplier<String> nodeUpdateCommand)
            throws FrontendUtils.UnknownVersionException {
        settings.setForceAlternativeNode(true);
        tools = new FrontendTools(settings);

        String toBeInstalled = "v" + oldNodeVersion;
        String nodeExecutable = FrontendToolsTestHelper.installNode(
                new File(vaadinHomeDir), tools.getProxies(), toBeInstalled,
                null);
        Assert.assertNotNull(nodeExecutable);

        List<String> nodeVersionCommand = new ArrayList<>();
        nodeVersionCommand.add(nodeExecutable);
        nodeVersionCommand.add("--version");
        FrontendVersion node = FrontendUtils.getVersion("node",
                nodeVersionCommand);
        Assert.assertEquals("Wrong node version installed", oldNodeVersion,
                node.getFullVersion());

        nodeExecutable = nodeUpdateCommand.get();
        nodeVersionCommand = new ArrayList<>();
        nodeVersionCommand.add(nodeExecutable);
        nodeVersionCommand.add("--version");
        return FrontendUtils.getVersion("node", nodeVersionCommand);
    }

    private void prepareNodeDownloadableZipAt(String baseDir, String version)
            throws IOException {
        Platform platform = Platform.guess();
        String nodeExec = platform.isWindows() ? "node.exe" : "node";
        String prefix = "node-" + version + "-"
                + platform.getNodeClassifier(new FrontendVersion(version));

        File downloadDir = new File(baseDir, version);
        FileUtils.forceMkdir(downloadDir);
        File archiveFile = new File(downloadDir,
                prefix + "." + platform.getArchiveExtension());
        archiveFile.createNewFile();
        Path tempArchive = archiveFile.toPath();

        if (platform.getArchiveExtension().equals("zip")) {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                    Files.newOutputStream(tempArchive))) {
                zipOutputStream
                        .putNextEntry(new ZipEntry(prefix + "/" + nodeExec));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(
                        new ZipEntry(prefix + "/node_modules/npm/bin/npm"));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(
                        new ZipEntry(prefix + "/node_modules/npm/bin/npm.cmd"));
                zipOutputStream.closeEntry();
                zipOutputStream.putNextEntry(new ZipEntry(
                        prefix + "/node_modules/npm/bin/npm-cli.js"));
                zipOutputStream.closeEntry();
            }
        } else {
            try (OutputStream fo = Files.newOutputStream(tempArchive);
                    OutputStream gzo = new GzipCompressorOutputStream(fo);
                    ArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/bin/" + nodeExec),
                        prefix + "/bin/" + nodeExec));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/bin/npm"), prefix + "/bin/npm"));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/lib/node_modules/npm/bin/npm"),
                        prefix + "/lib/node_modules/npm/bin/npm"));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix + "/lib/node_modules/npm/bin/npm.cmd"),
                        prefix + "/lib/node_modules/npm/bin/npm.cmd"));
                o.closeArchiveEntry();
                o.putArchiveEntry(o.createArchiveEntry(
                        new File(prefix
                                + "/lib/node_modules/npm/bin/npm-cli.js"),
                        prefix + "/lib/node_modules/npm/bin/npm-cli.js"));
                o.closeArchiveEntry();
            }
        }
    }

    @Test
    public void installNodeFromFileSystem_NodeIsInstalledToTargetDirectory()
            throws IOException {
        prepareNodeDownloadableZipAt(baseDir,
                FrontendTools.DEFAULT_NODE_VERSION);

        String nodeExecutable = installNodeToTempFolder();
        Assert.assertNotNull(nodeExecutable);

        // Check npm in version-specific directory
        String npmInstallPath = getVersionedNpmBinPath(
                FrontendTools.DEFAULT_NODE_VERSION) + "npm";

        Assert.assertTrue("npm should have been copied to node_modules",
                new File(vaadinHomeDir, npmInstallPath).exists());
    }

    @Test
    public void installNodeFromFileSystem_ForceAlternativeNodeExecutableInstallsToTargetDirectory()
            throws Exception {
        String testVersion = "v12.10.0";
        String npmPath = getVersionedNpmBinPath(testVersion) + "npm";
        Assert.assertFalse("npm should not yet be present",
                new File(vaadinHomeDir, npmPath).exists());

        settings.setNodeDownloadRoot(new File(baseDir).toURI());
        settings.setNodeVersion(testVersion);
        tools = new FrontendTools(settings);
        prepareNodeDownloadableZipAt(baseDir, testVersion);
        tools.getNodeExecutable();

        Assert.assertTrue("npm should have been copied to node_modules",
                new File(vaadinHomeDir, npmPath).exists());
    }

    private String getVersionedNpmBinPath(String nodeVersion) {
        return FrontendUtils.isWindows()
                ? "node-" + nodeVersion + "/node_modules/npm/bin/"
                : "node-" + nodeVersion + "/lib/node_modules/npm/bin/";
    }

    @Test
    public void homeNodeIsNotForced_useGlobalNode()
            throws IOException, FrontendUtils.UnknownVersionException {
        createStubNode(true, true, vaadinHomeDir);

        // Validate the global node to be applicable for testing.
        String nodeCommand = FrontendUtils.isWindows() ? "node.exe" : "node";
        File file = frontendToolsLocator.tryLocateTool(nodeCommand)
                .orElse(null);
        if (file == null) {
            LoggerFactory.getLogger(FrontendToolsTest.class)
                    .info("No global node found, skipping test");
            return;
        }
        List<String> versionCommand = Lists.newArrayList();
        versionCommand.add(file.getAbsolutePath());
        versionCommand.add("--version"); // NOSONAR
        final FrontendVersion installedNodeVersion = FrontendUtils
                .getVersion("node", versionCommand);
        if (installedNodeVersion
                .isOlderThan(FrontendTools.SUPPORTED_NODE_VERSION)) {
            LoggerFactory.getLogger(FrontendToolsTest.class).info(
                    "Global version of node is {} which is older than the supported version {}",
                    installedNodeVersion.getFullVersion(),
                    FrontendTools.SUPPORTED_NODE_VERSION.getFullVersion());
            return;
        }

        assertThat(tools.getNodeExecutable(), containsString("node"));
        assertThat(tools.getNodeExecutable(),
                not(containsString(DEFAULT_NODE)));
        assertThat(tools.getNodeExecutable(),
                not(containsString(NPM_CLI_STRING)));
        assertThat(tools.getNodeExecutable(),
                not(containsString(vaadinHomeDir)));
        assertThat(tools.getNodeExecutable(), not(containsString(baseDir)));

        assertEquals(4, tools.getNpmExecutable().size());
        assertThat(tools.getNpmExecutable().get(0), containsString("npm"));
        assertThat(tools.getNpmExecutable().get(1),
                containsString("--no-update-notifier"));
        assertThat(tools.getNpmExecutable().get(2),
                containsString("--no-audit"));
        assertThat(tools.getNpmExecutable().get(3),
                containsString("--scripts-prepend-node-path=true"));
    }

    @Test
    public void getNpmExecutable_removesPnpmLock() throws IOException {
        File file = new File(baseDir, "pnpm-lock.yaml");
        file.createNewFile();

        tools.getNpmExecutable();

        Assert.assertFalse(file.exists());
    }

    @Test
    public void knownFaultyNpmVersionThrowsException() {
        assertFaultyNpmVersion(new FrontendVersion(9, 2, 0));
    }

    @Ignore("Until a newer version of Node.js is installed in CI/CD, which doesn't let pnpm version check to fail")
    @Test
    public void getPnpmExecutable_executableIsAvailable() {
        List<String> executable = tools.getPnpmExecutable();
        // command line should contain --shamefully-hoist=true option
        Assert.assertTrue(executable.contains("--shamefully-hoist=true"));
        Assert.assertTrue(
                executable.stream().anyMatch(cmd -> cmd.contains("pnpm")));
    }

    @Test
    public void validateNodeAndNpmVersion_pnpmLockIsNotRemoved()
            throws IOException {
        File file = new File(baseDir, "pnpm-lock.yaml");
        file.createNewFile();

        tools.validateNodeAndNpmVersion();

        Assert.assertTrue(file.exists());
    }

    @Test(expected = IllegalStateException.class)
    public void ensureNodeExecutableInHome_vaadinHomeNodeIsAFolder_throws()
            throws IOException {
        // Create a folder where the node binary should be (versioned path)
        String version = FrontendTools.DEFAULT_NODE_VERSION;
        String nodePath = FrontendUtils.isWindows()
                ? "node-" + version + "/node.exe"
                : "node-" + version + "/bin/node";
        File node = new File(vaadinHomeDir, nodePath);
        FileUtils.forceMkdir(node);

        tools.getNodeExecutable();
    }

    @Test
    public void getProxies_noNpmrc_shouldReturnEmptyList() {
        File npmrc = new File(baseDir + "/.npmrc");
        if (npmrc.exists())
            npmrc.delete();

        List<ProxyConfig.Proxy> proxyList = tools.getProxies();
        Assert.assertTrue(proxyList.isEmpty());
    }

    @Test
    public synchronized void getProxies_systemPropertiesAndNpmrcWithProxySetting_shouldReturnAllProxies()
            throws IOException {
        File npmrc = new File(tmpDirWithNpmrc.newFolder("test2"), ".npmrc");

        settings.setBaseDir(npmrc.getParent());
        settings.setAlternativeDirGetter(null);

        tools = new FrontendTools(settings);

        Properties properties = new Properties();
        properties.put(ProxyFactory.NPMRC_PROXY_PROPERTY_KEY,
                "http://httpuser:httppassword@httphost:8080");
        properties.put(ProxyFactory.NPMRC_HTTPS_PROXY_PROPERTY_KEY,
                "http://httpsuser:httpspassword@httpshost:8081");
        properties.put(ProxyFactory.NPMRC_NOPROXY_PROPERTY_KEY,
                "192.168.1.1,vaadin.com,mycompany.com");
        try (FileOutputStream fileOutputStream = new FileOutputStream(npmrc)) {
            properties.store(fileOutputStream, null);
        }

        List<ProxyConfig.Proxy> proxyList = null;
        try {
            System.setProperty(FrontendUtils.SYSTEM_NOPROXY_PROPERTY_KEY,
                    "somethingelse,someotherip,75.41.41.33");
            System.setProperty(FrontendUtils.SYSTEM_HTTP_PROXY_PROPERTY_KEY,
                    "http://anotheruser:anotherpassword@aanotherhost:9090");
            System.setProperty(FrontendUtils.SYSTEM_HTTPS_PROXY_PROPERTY_KEY,
                    "http://anotherusers:anotherpasswords@aanotherhosts:9091/");

            proxyList = tools.getProxies();
        } finally {
            System.clearProperty(FrontendUtils.SYSTEM_NOPROXY_PROPERTY_KEY);
            System.clearProperty(FrontendUtils.SYSTEM_HTTP_PROXY_PROPERTY_KEY);
            System.clearProperty(FrontendUtils.SYSTEM_HTTPS_PROXY_PROPERTY_KEY);
        }

        Assert.assertEquals(4, proxyList.size());

        // The first two items should be system proxies
        ProxyConfig.Proxy systemHttpsProxy = proxyList.get(0).id.startsWith(
                "https-proxy") ? proxyList.get(0) : proxyList.get(1);
        ProxyConfig.Proxy systemProxy = proxyList.get(0).id.startsWith(
                "https-proxy") ? proxyList.get(1) : proxyList.get(0);

        // Items 2 and 3 should be npmrc proxies
        ProxyConfig.Proxy npmrcHttpsProxy = proxyList.get(2).id.startsWith(
                "https-proxy") ? proxyList.get(2) : proxyList.get(3);
        ProxyConfig.Proxy npmrcProxy = proxyList.get(2).id.startsWith(
                "https-proxy") ? proxyList.get(3) : proxyList.get(2);

        Assert.assertEquals("http", systemProxy.protocol);
        Assert.assertEquals("anotheruser", systemProxy.username);
        Assert.assertEquals("anotherpassword", systemProxy.password);
        Assert.assertEquals("aanotherhost", systemProxy.host);
        Assert.assertEquals(9090, systemProxy.port);
        Assert.assertEquals("somethingelse|someotherip|75.41.41.33",
                systemProxy.nonProxyHosts);

        Assert.assertEquals("http", systemHttpsProxy.protocol);
        Assert.assertEquals("anotherusers", systemHttpsProxy.username);
        Assert.assertEquals("anotherpasswords", systemHttpsProxy.password);
        Assert.assertEquals("aanotherhosts", systemHttpsProxy.host);
        Assert.assertEquals(9091, systemHttpsProxy.port);
        Assert.assertEquals("somethingelse|someotherip|75.41.41.33",
                systemHttpsProxy.nonProxyHosts);

        Assert.assertEquals("http", npmrcHttpsProxy.protocol);
        Assert.assertEquals("httpsuser", npmrcHttpsProxy.username);
        Assert.assertEquals("httpspassword", npmrcHttpsProxy.password);
        Assert.assertEquals("httpshost", npmrcHttpsProxy.host);
        Assert.assertEquals(8081, npmrcHttpsProxy.port);
        Assert.assertEquals("192.168.1.1|vaadin.com|mycompany.com",
                npmrcHttpsProxy.nonProxyHosts);

        Assert.assertEquals("http", npmrcProxy.protocol);
        Assert.assertEquals("httpuser", npmrcProxy.username);
        Assert.assertEquals("httppassword", npmrcProxy.password);
        Assert.assertEquals("httphost", npmrcProxy.host);
        Assert.assertEquals(8080, npmrcProxy.port);
        Assert.assertEquals("192.168.1.1|vaadin.com|mycompany.com",
                npmrcProxy.nonProxyHosts);
    }

    @Test
    public synchronized void getProxies_npmrcWithProxySettingNoNoproxy_shouldReturnNullNoproxy()
            throws IOException {
        File npmrc = new File(tmpDirWithNpmrc.newFolder("test1"), ".npmrc");
        Properties properties = new Properties();
        properties.put(ProxyFactory.NPMRC_PROXY_PROPERTY_KEY,
                "http://httpuser:httppassword@httphost:8080");
        properties.put(ProxyFactory.NPMRC_HTTPS_PROXY_PROPERTY_KEY,
                "http://httpsuser:httpspassword@httpshost:8081");
        try (FileOutputStream fileOutputStream = new FileOutputStream(npmrc)) {
            properties.store(fileOutputStream, null);
        }

        settings.setBaseDir(npmrc.getParent());
        settings.setAlternativeDirGetter(null);

        tools = new FrontendTools(settings);

        List<ProxyConfig.Proxy> proxyList = tools.getProxies();
        Assert.assertEquals(2, proxyList.size());
        ProxyConfig.Proxy httpsProxy = proxyList.get(0).id.startsWith(
                "https-proxy") ? proxyList.get(0) : proxyList.get(1);
        ProxyConfig.Proxy httpProxy = proxyList.get(0).id.startsWith(
                "https-proxy") ? proxyList.get(1) : proxyList.get(0);

        Assert.assertEquals("http", httpProxy.protocol);
        Assert.assertEquals("httpuser", httpProxy.username);
        Assert.assertEquals("httppassword", httpProxy.password);
        Assert.assertEquals("httphost", httpProxy.host);
        Assert.assertEquals(8080, httpProxy.port);
        Assert.assertNull(httpProxy.nonProxyHosts);

        Assert.assertEquals("http", httpsProxy.protocol);
        Assert.assertEquals("httpsuser", httpsProxy.username);
        Assert.assertEquals("httpspassword", httpsProxy.password);
        Assert.assertEquals("httpshost", httpsProxy.host);
        Assert.assertEquals(8081, httpsProxy.port);
        Assert.assertNull(httpsProxy.nonProxyHosts);
    }

    @Test
    public synchronized void getProxies_npmrcWithProxySetting_shouldReturnProxiesList()
            throws IOException {
        File npmrc = new File(tmpDirWithNpmrc.newFolder("test1"), ".npmrc");
        Properties properties = new Properties();
        properties.put(ProxyFactory.NPMRC_PROXY_PROPERTY_KEY,
                "http://httpuser:httppassword@httphost:8080");
        properties.put(ProxyFactory.NPMRC_HTTPS_PROXY_PROPERTY_KEY,
                "http://httpsuser:httpspassword@httpshost:8081");
        properties.put(ProxyFactory.NPMRC_NOPROXY_PROPERTY_KEY,
                "192.168.1.1,vaadin.com,mycompany.com");
        try (FileOutputStream fileOutputStream = new FileOutputStream(npmrc)) {
            properties.store(fileOutputStream, null);
        }

        settings.setBaseDir(npmrc.getParent());
        settings.setAlternativeDirGetter(null);

        tools = new FrontendTools(settings);

        List<ProxyConfig.Proxy> proxyList = tools.getProxies();
        Assert.assertEquals(2, proxyList.size());
        ProxyConfig.Proxy httpsProxy = proxyList.get(0).id.startsWith(
                "https-proxy") ? proxyList.get(0) : proxyList.get(1);
        ProxyConfig.Proxy httpProxy = proxyList.get(0).id.startsWith(
                "https-proxy") ? proxyList.get(1) : proxyList.get(0);

        Assert.assertEquals("http", httpProxy.protocol);
        Assert.assertEquals("httpuser", httpProxy.username);
        Assert.assertEquals("httppassword", httpProxy.password);
        Assert.assertEquals("httphost", httpProxy.host);
        Assert.assertEquals(8080, httpProxy.port);
        Assert.assertEquals("192.168.1.1|vaadin.com|mycompany.com",
                httpProxy.nonProxyHosts);

        Assert.assertEquals("http", httpsProxy.protocol);
        Assert.assertEquals("httpsuser", httpsProxy.username);
        Assert.assertEquals("httpspassword", httpsProxy.password);
        Assert.assertEquals("httpshost", httpsProxy.host);
        Assert.assertEquals(8081, httpsProxy.port);
        Assert.assertEquals("192.168.1.1|vaadin.com|mycompany.com",
                httpsProxy.nonProxyHosts);
    }

    @Test
    public void forceHomeNode_useHomeNpmFirst() throws Exception {
        settings.setForceAlternativeNode(true);
        settings.setNodeDownloadRoot(new File(baseDir).toPath().toUri());
        tools = new FrontendTools(settings);

        // Install node to vaadin home dir using the test helper
        prepareNodeDownloadableZipAt(baseDir,
                FrontendTools.DEFAULT_NODE_VERSION);
        String nodeExecutable = FrontendToolsTestHelper.installNode(
                new File(vaadinHomeDir), tools.getProxies(),
                FrontendTools.DEFAULT_NODE_VERSION,
                new File(baseDir).toPath().toUri());
        Assert.assertNotNull(nodeExecutable);

        // Verify that node and npm from vaadin home are being used
        assertThat(tools.getNodeExecutable(), containsString("node"));
        assertThat(tools.getNodeExecutable(), containsString(vaadinHomeDir));
        List<String> npmExecutable = tools.getNpmExecutable();
        assertThat(npmExecutable.get(0), containsString(vaadinHomeDir));
        assertThat(npmExecutable.get(1), containsString(NPM_CLI_STRING));
    }

    @Test
    public void getSuitablePnpm_tooOldGlobalVersionInstalled_throws() {
        settings.setUseGlobalPnpm(true);
        tools = new FrontendTools(settings);
        try {
            installGlobalPnpm(OLD_PNPM_VERSION);
            IllegalStateException exception = Assert.assertThrows(
                    IllegalStateException.class, () -> tools.getSuitablePnpm());
            Assert.assertTrue(
                    "Unexpected exception message content '"
                            + exception.getMessage() + "'",
                    exception.getMessage().contains(
                            "Found too old globally installed 'pnpm'. Please upgrade 'pnpm' to at least 7.0.0"));
        } finally {
            uninstallGlobalPnpm(OLD_PNPM_VERSION);
        }
    }

    @Test
    public void getSuitablePnpm_tooOldGlobalVersionInstalledAndSkipVersionCheck_accepted() {
        settings.setUseGlobalPnpm(true);
        settings.setIgnoreVersionChecks(true);
        tools = new FrontendTools(settings);
        try {
            installGlobalPnpm(OLD_PNPM_VERSION);
            List<String> pnpmCommand = tools.getSuitablePnpm();
            Assert.assertTrue(
                    "expected old global pnpm version accepted when skip version flag is set",
                    pnpmCommand.get(pnpmCommand.size() - 1).contains("pnpm"));
        } finally {
            uninstallGlobalPnpm(OLD_PNPM_VERSION);
        }
    }

    @Test
    public void getSuitablePnpm_supportedGlobalVersionInstalled_accepted() {
        settings.setUseGlobalPnpm(true);
        tools = new FrontendTools(settings);
        try {
            installGlobalPnpm(SUPPORTED_PNPM_VERSION);
            List<String> pnpmCommand = tools.getSuitablePnpm();
            Assert.assertTrue("expected supported global pnpm version accepted",
                    pnpmCommand.get(pnpmCommand.size() - 1).contains("pnpm"));
        } finally {
            uninstallGlobalPnpm(SUPPORTED_PNPM_VERSION);
        }
    }

    @Test
    public void getSuitablePnpm_useGlobalPnpm_noPnpmInstalled_throws() {
        Assume.assumeFalse("Skipping test on windows.",
                FrontendUtils.isWindows());
        Optional<File> pnpm = frontendToolsLocator.tryLocateTool("pnpm");
        Assume.assumeFalse("Skip this test once globally installed pnpm is "
                + "discovered", pnpm.isPresent());

        settings.setNodeDownloadRoot(URI.create(baseDir));
        settings.setUseGlobalPnpm(true);
        tools = new FrontendTools(settings);

        IllegalStateException exception = Assert.assertThrows(
                IllegalStateException.class, () -> tools.getSuitablePnpm());
        Assert.assertTrue(
                "Unexpected exception message content '"
                        + exception.getMessage() + "'",
                exception.getMessage().contains(
                        "Vaadin is configured to use a globally installed "
                                + "pnpm ('pnpm.global=true'), but pnpm was not found "
                                + "on your system."));
    }

    @Test
    public void folderIsAcceptableByNpm_npmCacheDirWithWhitespaces_falseForWindows()
            throws IOException {
        Assume.assumeTrue("This test is only for Windows, since the issue with "
                + "whitespaces in npm processed directories reproduces only on "
                + "Windows", FrontendUtils.isWindows());
        // given
        // dir with whitespaces
        File npmCacheDir = tmpDir.newFolder("Foo Bar");

        FrontendStubs.ToolStubInfo nodeStub = FrontendStubs.ToolStubInfo.none();
        // Old npm version
        FrontendStubs.ToolStubInfo npmStub = FrontendStubs.ToolStubInfo
                .builder(FrontendStubs.Tool.NPM).withVersion("6.0.0").build();
        createStubNode(nodeStub, npmStub, baseDir);

        // when
        boolean accepted = tools.folderIsAcceptableByNpm(npmCacheDir);

        // then
        Assert.assertFalse(accepted);
    }

    @Test
    public void folderIsAcceptableByNpm_npmCacheDirWithWhitespaces_trueForNonWindows()
            throws IOException {
        Assume.assumeFalse(
                "This test is for the rest of OS rather than Windows, since "
                        + "the issue with whitespaces in directories processed by npm, "
                        + "is not reproduced on them",
                FrontendUtils.isWindows());

        // given
        // dir with whitespaces
        File npmCacheDir = tmpDir.newFolder("Foo Bar");

        FrontendStubs.ToolStubInfo nodeStub = FrontendStubs.ToolStubInfo.none();
        // Old npm version
        FrontendStubs.ToolStubInfo npmStub = FrontendStubs.ToolStubInfo
                .builder(FrontendStubs.Tool.NPM).withVersion("6.0.0").build();
        createStubNode(nodeStub, npmStub, baseDir);

        // when
        boolean accepted = tools.folderIsAcceptableByNpm(npmCacheDir);

        // then
        Assert.assertTrue(accepted);
    }

    @Test
    public void folderIsAcceptableByNpm_npmCacheNoWhitespaces_trueForWindows()
            throws IOException {
        Assume.assumeTrue("This test is only for Windows, since the issue with "
                + "whitespaces in npm processed directories reproduces only on "
                + "Windows", FrontendUtils.isWindows());

        // given
        // dir with no whitespaces
        File npmCacheDir = tmpDir.newFolder("FooBar");

        FrontendStubs.ToolStubInfo nodeStub = FrontendStubs.ToolStubInfo.none();
        // Old npm version
        FrontendStubs.ToolStubInfo npmStub = FrontendStubs.ToolStubInfo
                .builder(FrontendStubs.Tool.NPM).withVersion("6.0.0").build();
        createStubNode(nodeStub, npmStub, baseDir);

        // when
        boolean accepted = tools.folderIsAcceptableByNpm(npmCacheDir);

        // then
        Assert.assertTrue(accepted);
    }

    @Test
    public void folderIsAcceptableByNpm_npm7_trueForWindows()
            throws IOException {
        Assume.assumeTrue("This test is only for Windows, since the issue with "
                + "whitespaces in npm processed directories reproduces only on "
                + "Windows", FrontendUtils.isWindows());

        // given
        // dir with whitespaces
        File npmCacheDir = tmpDir.newFolder("Foo  Bar");

        FrontendStubs.ToolStubInfo nodeStub = FrontendStubs.ToolStubInfo.none();
        // Acceptable npm version
        FrontendStubs.ToolStubInfo npmStub = FrontendStubs.ToolStubInfo
                .builder(FrontendStubs.Tool.NPM).withVersion("7.0.0").build();
        createStubNode(nodeStub, npmStub, baseDir);

        // when
        boolean accepted = tools.folderIsAcceptableByNpm(npmCacheDir);

        // then
        Assert.assertTrue(accepted);
    }

    @Test
    public void getViteExecutable_returnsCorrectPath()
            throws IOException, FrontendUtils.CommandExecutionException {
        var projectDir = tmpDir.newFolder();
        var packageJson = Files
                .createFile(projectDir.toPath().resolve("package.json"));

        Files.write(packageJson, """
                  {
                  "name": "test",
                  "private": true
                }
                """.getBytes());
        Files.createDirectories(
                projectDir.toPath().resolve("node_modules/vite/bin/"));
        var vitePackageJson = Files.createFile(
                projectDir.toPath().resolve("node_modules/vite/package.json"));

        Files.write(vitePackageJson, """
                {
                  "name": "vite",
                  "version": "4.0.0",
                  "bin": {
                    "vite": "bin/vite.js"
                  }
                }
                """.getBytes());
        // Create the actual vite.js file for toRealPath() to work
        Files.createFile(
                projectDir.toPath().resolve("node_modules/vite/bin/vite.js"));
        var vite = tools.getNpmPackageExecutable("vite", "vite", projectDir);
        // Use toRealPath() to handle symlinks (e.g., /var -> /private/var on
        // macOS)
        Assert.assertEquals(projectDir.toPath()
                .resolve("node_modules/vite/bin/vite.js").toRealPath(),
                vite.toRealPath());
    }

    private void assertNpmCommand(Supplier<String> path) throws IOException {
        createStubNode(false, true, vaadinHomeDir);

        assertThat(tools.getNodeExecutable(), containsString("node"));

        List<String> npmExecutable = tools.getNpmExecutable();
        assertThat(npmExecutable.get(0), containsString("node"));
        assertThat(npmExecutable.get(1), containsString(NPM_CLI_STRING));
        assertThat(npmExecutable.get(1), containsString(path.get()));
    }

    private void assertNodeCommand(Supplier<String> path) throws IOException {
        createStubNode(true, true, vaadinHomeDir);

        assertThat(tools.getNodeExecutable(), containsString(DEFAULT_NODE));
        assertThat(tools.getNodeExecutable(), containsString(path.get()));
        List<String> npmExecutable = tools.getNpmExecutable();
        assertThat(npmExecutable.get(0), containsString(path.get()));
        assertThat(npmExecutable.get(0), containsString(DEFAULT_NODE));
        assertThat(npmExecutable.get(1), containsString(NPM_CLI_STRING));
    }

    private void assertFaultyNpmVersion(FrontendVersion version) {
        try {
            tools.checkForFaultyNpmVersion(version);
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

    private void installGlobalPnpm(String pnpmVersion) {
        Optional<File> npmInstalled = frontendToolsLocator
                .tryLocateTool(getCommand("npm"));
        if (!npmInstalled.isPresent()) {
            installNodeToTempFolder();
        }
        doInstallPnpmGlobally(pnpmVersion, false);
    }

    private String installNodeToTempFolder() {
        return FrontendToolsTestHelper.installNode(new File(vaadinHomeDir),
                tools.getProxies(), FrontendTools.DEFAULT_NODE_VERSION,
                new File(baseDir).toPath().toUri());
    }

    private void uninstallGlobalPnpm(String pnpmVersion) {
        doInstallPnpmGlobally(pnpmVersion, true);
    }

    private void doInstallPnpmGlobally(String pnpmVersion, boolean uninstall) {
        final String pnpmPackageSpecifier = "pnpm"
                + (uninstall ? "" : "@" + pnpmVersion);
        final List<String> installPnpmCommand = Arrays.asList(getCommand("npm"),
                uninstall ? "rm" : "install", "-g", pnpmPackageSpecifier);
        try {
            FrontendUtils.executeCommand(installPnpmCommand);
        } catch (FrontendUtils.CommandExecutionException e) {
            throw new RuntimeException(String.format(
                    "Pnpm installation failed, pnpm version='%s', uninstall='%s'",
                    pnpmVersion, uninstall), e);
        }
    }

    private String getCommand(String name) {
        return FrontendUtils.isWindows() ? name + ".cmd" : name;
    }

    /**
     * Manual testing utility to demonstrate which Node.js installation will be
     * used.
     * <p>
     * The resolution logic uses any installed Node.js >= minimum supported
     * version (v24.0.0). If no suitable installation exists, it installs the
     * preferred version specified by -DnodeVersion.
     * <p>
     * Usage examples:
     * <ul>
     * <li>Test with global node: {@code mvn exec:java
     * -Dexec.mainClass="com.vaadin.flow.server.frontend.FrontendToolsTest"
     * -Dexec.classpathScope=test}</li>
     * <li>Test forcing alternative: {@code mvn exec:java ...
     * -Dalternative=true}</li>
     * <li>Test with custom preferred version: {@code mvn exec:java ...
     * -DnodeVersion=v24.5.0}</li>
     * </ul>
     *
     * @param args
     *            command line arguments (not used)
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("Node.js Resolution Test");
        System.out.println("=".repeat(80));

        try {
            // Read configuration from system properties
            boolean forceAlternative = Boolean.getBoolean("alternative");
            String preferredVersion = System.getProperty("nodeVersion",
                    FrontendTools.DEFAULT_NODE_VERSION);
            String baseDir = System.getProperty("baseDir",
                    System.getProperty("user.dir"));

            System.out.println("\nConfiguration:");
            System.out.println("  Base directory: " + baseDir);
            System.out.println("  Supported version for global: >= "
                    + FrontendTools.SUPPORTED_NODE_VERSION.getFullVersion());
            System.out
                    .println("  Minimum auto-installed version (~/.vaadin): >= "
                            + FrontendTools.MINIMUM_AUTO_INSTALLED_NODE
                                    .getFullVersion());
            System.out.println("  Maximum major version: "
                    + FrontendTools.MAX_SUPPORTED_NODE_MAJOR_VERSION);
            System.out.println("  Preferred version (to install if needed): "
                    + preferredVersion);
            System.out.println("  Force alternative node: " + forceAlternative);
            System.out.println();

            // Create FrontendTools instance
            FrontendToolsSettings settings = new FrontendToolsSettings(baseDir,
                    () -> FrontendUtils.getVaadinHomeDirectory()
                            .getAbsolutePath());
            settings.setNodeVersion(preferredVersion);
            settings.setForceAlternativeNode(forceAlternative);

            FrontendTools tools = new FrontendTools(settings);

            // Get resolved node information
            String nodeExecutable = tools.getNodeExecutable();
            String actualVersionUsed = tools.getNodeVersion().getFullVersion();
            String npmVersion = tools.getNpmVersion().getFullVersion();

            System.out.println("Resolved Node.js installation:");
            System.out.println("  Node executable: " + nodeExecutable);
            System.out.println("  Actual version used: " + actualVersionUsed);
            System.out.println("  npm version: " + npmVersion);

            // Check if using global or alternative installation
            File nodeFile = new File(nodeExecutable);
            boolean isGlobal = !nodeFile.getAbsolutePath()
                    .contains(FrontendUtils.getVaadinHomeDirectory().getName());

            System.out.println("\nInstallation type: "
                    + (isGlobal ? "GLOBAL" : "ALTERNATIVE (~/.vaadin)"));

            if (!isGlobal) {
                System.out.println("  Location: " + FrontendUtils
                        .getVaadinHomeDirectory().getAbsolutePath());
            }

            // Try to run node --version to verify it works
            System.out.println("\nVerification:");
            try {
                List<String> versionCommand = new ArrayList<>();
                versionCommand.add(nodeExecutable);
                versionCommand.add("--version");
                FrontendVersion version = FrontendUtils.getVersion("node",
                        versionCommand);
                System.out.println("  ✓ Node executable is working");
                System.out.println(
                        "  ✓ Verified version: " + version.getFullVersion());
            } catch (Exception e) {
                System.out.println("  ✗ Failed to verify node executable: "
                        + e.getMessage());
            }

            System.out.println("\n" + "=".repeat(80));
            System.out.println("Resolution completed successfully");
            System.out.println("=".repeat(80));

        } catch (Exception e) {
            System.err.println("\n" + "=".repeat(80));
            System.err.println("ERROR: Resolution failed");
            System.err.println("=".repeat(80));
            System.err.println("\nException: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            System.err.println("\nStack trace:");
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
