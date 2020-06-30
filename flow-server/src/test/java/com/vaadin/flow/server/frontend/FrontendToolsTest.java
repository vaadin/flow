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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;

import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubNode;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class FrontendToolsTest {

    public static final String DEFAULT_NODE = FrontendUtils.isWindows()
            ? "node\\node.exe"
            : "node/node";

    public static final String NPM_CLI_STRING = Stream
            .of("node", "node_modules", "npm", "bin", "npm-cli.js")
            .collect(Collectors.joining(File.separator));

    private String baseDir;

    private String vaadinHomeDir;

    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    @Rule
    public final TemporaryFolder tmpDirWithNpmrc = new TemporaryFolder();

    private FrontendTools tools;

    @Before
    public void setup() throws IOException {
        baseDir = tmpDir.newFolder().getAbsolutePath();
        vaadinHomeDir = tmpDir.newFolder().getAbsolutePath();
        tools = new FrontendTools(baseDir, () -> vaadinHomeDir);
    }

    @Test
    @Ignore("Ignored to lessen PRs hitting the server too often")
    public void installNode_NodeIsInstalledToTargetDirectory()
            throws FrontendUtils.UnknownVersionException {
        String nodeExecutable = tools.installNode("v12.16.0", null);
        Assert.assertNotNull(nodeExecutable);

        List<String> nodeVersionCommand = new ArrayList<>();
        nodeVersionCommand.add(nodeExecutable);
        nodeVersionCommand.add("--version");
        FrontendVersion node = FrontendUtils.getVersion("node",
                nodeVersionCommand);
        Assert.assertEquals("12.16.0", node.getFullVersion());

        FrontendTools newTools = new FrontendTools(vaadinHomeDir, null);
        List<String> npmVersionCommand = new ArrayList<>(
                newTools.getNpmExecutable());
        npmVersionCommand.add("--version");
        FrontendVersion npm = FrontendUtils.getVersion("npm",
                npmVersionCommand);
        Assert.assertEquals("6.13.4", npm.getFullVersion());

    }

    private void prepareNodeDownloadableZipAt(String baseDir, String version) throws IOException {
        Platform platform = Platform.guess();
        String nodeExec = platform.isWindows() ? "node.exe" : "node";
        String prefix = "node-" + version + "-" + platform.getNodeClassifier();

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
            }
        }
    }

    @Test
    public void installNodeFromFileSystem_NodeIsInstalledToTargetDirectory()
            throws IOException {
        prepareNodeDownloadableZipAt(baseDir, "v12.16.0");

        String nodeExecutable = tools.installNode("v12.16.0",
                new File(baseDir).toPath().toUri());
        Assert.assertNotNull(nodeExecutable);

        Assert.assertTrue("npm should have been copied to node_modules",
                new File(vaadinHomeDir, "node/node_modules/npm/bin/npm")
                        .exists());
    }

    @Test
    public void installNodeFromFileSystem_ForceAlternativeNodeExecutableInstallsToTargetDirectory()
            throws Exception{
        Assert.assertFalse("npm should not yet be present",
                new File(vaadinHomeDir, "node/node_modules/npm/bin/npm")
                        .exists());

        tools = new FrontendTools(baseDir, () -> vaadinHomeDir,
                "v12.10.0", new File(baseDir).toURI());
        prepareNodeDownloadableZipAt(baseDir, "v12.10.0");
        tools.forceAlternativeNodeExecutable();

        Assert.assertTrue("npm should have been copied to node_modules",
                new File(vaadinHomeDir, "node/node_modules/npm/bin/npm")
                        .exists());
    }

    @Test
    public void should_useSystemNode() {
        assertThat(tools.getNodeExecutable(), containsString("node"));
        assertThat(tools.getNodeExecutable(),
                not(containsString(DEFAULT_NODE)));
        assertThat(tools.getNodeExecutable(),
                not(containsString(NPM_CLI_STRING)));

        assertEquals(3, tools.getNpmExecutable().size());
        assertThat(tools.getNpmExecutable().get(0), containsString("npm"));
        assertThat(tools.getNpmExecutable().get(1),
                containsString("--no-update-notifier"));
        assertThat(tools.getNpmExecutable().get(2),
                containsString("--no-audit"));
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
        assertFaultyNpmVersion(new FrontendVersion(6, 11, 0));
        assertFaultyNpmVersion(new FrontendVersion(6, 11, 1));
        assertFaultyNpmVersion(new FrontendVersion(6, 11, 2));
    }

    /**
     * This test doesn't do anything if pnpm is already installed (globally)
     * which is true e.g. for or CI servers (TC/bender).
     */
    @Test
    public void ensurePnpm_requestInstall_keepPackageJson_removePackageLock_ignoredPnpmExists_localPnpmIsRemoved()
            throws IOException {
        Assume.assumeTrue(
                tools.getPnpmExecutable(vaadinHomeDir, false).isEmpty());
        File packageJson = new File(vaadinHomeDir, "package.json");
        FileUtils.writeStringToFile(packageJson, "{}", StandardCharsets.UTF_8);

        File packageLockJson = new File(vaadinHomeDir, "package-lock.json");
        FileUtils.writeStringToFile(packageLockJson, "{}",
                StandardCharsets.UTF_8);

        tools.ensurePnpm();
        Assert.assertFalse(
                tools.getPnpmExecutable(vaadinHomeDir, false).isEmpty());

        Assert.assertEquals("{}", FileUtils.readFileToString(packageJson,
                StandardCharsets.UTF_8));
        Assert.assertFalse(packageLockJson.exists());
    }

    @Test
    public void getPnpmExecutable_executableIsAvailable() {
        List<String> executable = tools.getPnpmExecutable();
        // command line should contain --shamefully-hoist=true option
        Assert.assertTrue(executable.contains("--shamefully-hoist=true"));
        Assert.assertTrue(
                executable.stream().anyMatch(cmd -> cmd.contains("pnpm")));
    }

    @Test
    public void getPnpmExecutable_pnpmIsNotInstalledGlobally_pnpmIsInstalledInHome() {
        List<String> executable = tools.getPnpmExecutable(baseDir, false);
        Assume.assumeTrue(executable.isEmpty());

        executable = tools.getPnpmExecutable();
        Assert.assertThat(executable.get(1),
                CoreMatchers.startsWith(vaadinHomeDir));
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
        File node = new File(vaadinHomeDir,
                FrontendUtils.isWindows() ? "node/node.exe" : "node/node");
        FileUtils.forceMkdir(node);

        tools.forceAlternativeNodeExecutable();
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

        FrontendTools tools = new FrontendTools(npmrc.getParent(), null);

        Properties properties = new Properties();
        properties.put(FrontendTools.NPMRC_PROXY_PROPERTY_KEY,
                "http://httpuser:httppassword@httphost:8080");
        properties.put(FrontendTools.NPMRC_HTTPS_PROXY_PROPERTY_KEY,
                "http://httpsuser:httpspassword@httpshost:8081");
        properties.put(FrontendTools.NPMRC_NOPROXY_PROPERTY_KEY,
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
                    "http://anotherusers:anotherpasswords@aanotherhosts:9091");

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
        properties.put(FrontendTools.NPMRC_PROXY_PROPERTY_KEY,
                "http://httpuser:httppassword@httphost:8080");
        properties.put(FrontendTools.NPMRC_HTTPS_PROXY_PROPERTY_KEY,
                "http://httpsuser:httpspassword@httpshost:8081");
        try (FileOutputStream fileOutputStream = new FileOutputStream(npmrc)) {
            properties.store(fileOutputStream, null);
        }

        FrontendTools tools = new FrontendTools(npmrc.getParent(), null);

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
        properties.put(FrontendTools.NPMRC_PROXY_PROPERTY_KEY,
                "http://httpuser:httppassword@httphost:8080");
        properties.put(FrontendTools.NPMRC_HTTPS_PROXY_PROPERTY_KEY,
                "http://httpsuser:httpspassword@httpshost:8081");
        properties.put(FrontendTools.NPMRC_NOPROXY_PROPERTY_KEY,
                "192.168.1.1,vaadin.com,mycompany.com");
        try (FileOutputStream fileOutputStream = new FileOutputStream(npmrc)) {
            properties.store(fileOutputStream, null);
        }

        FrontendTools tools = new FrontendTools(npmrc.getParent(), null);

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
    public void should_useProjectNodeFirst() throws Exception {
        Assume.assumeFalse(
                "Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.",
                FrontendUtils.isWindows());
        createStubNode(true, true, false, baseDir);

        assertNodeCommand(() -> baseDir);
    }

    @Test
    public void should_useHomeFirst() throws Exception {
        Assume.assumeFalse(
                "Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.",
                FrontendUtils.isWindows());
        assertNodeCommand(() -> vaadinHomeDir);
    }

    @Test
    public void should_useProjectNpmFirst() throws Exception {
        Assume.assumeFalse(
                "Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.",
                FrontendUtils.isWindows());
        createStubNode(false, true, false, baseDir);

        assertNpmCommand(() -> baseDir);
    }

    @Test
    public void should_useHomeNpmFirst() throws Exception {
        Assume.assumeFalse(
                "Skipping test on windows until a fake node.exe that isn't caught by Window defender can be created.",
                FrontendUtils.isWindows());
        assertNpmCommand(() -> vaadinHomeDir);
    }

    private void assertNpmCommand(Supplier<String> path) throws IOException {
        createStubNode(false, true, false, vaadinHomeDir);

        assertThat(tools.getNodeExecutable(), containsString("node"));
        assertThat(tools.getNodeExecutable(),
                not(containsString(DEFAULT_NODE)));
        List<String> npmExecutable = tools.getNpmExecutable();
        assertThat(npmExecutable.get(0), containsString("node"));
        assertThat(npmExecutable.get(1), containsString(NPM_CLI_STRING));
        assertThat(npmExecutable.get(1), containsString(path.get()));
    }

    private void assertNodeCommand(Supplier<String> path) throws IOException {
        createStubNode(true, true, false, vaadinHomeDir);

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

}
