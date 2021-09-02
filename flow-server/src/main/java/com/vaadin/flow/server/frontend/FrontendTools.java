/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.frontend.FrontendUtils.CommandExecutionException;
import com.vaadin.flow.server.frontend.FrontendUtils.UnknownVersionException;
import com.vaadin.flow.server.frontend.installer.InstallationException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;

/**
 * Provides access to frontend tools (node.js and npm, pnpm) and optionally
 * installs the tools if needed.
 * <p>
 * <b>WARNING:</b> This class is intended for internal usage only. May be
 * renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public class FrontendTools {

    public static final String DEFAULT_NODE_VERSION = "v16.7.0";

    public static final String DEFAULT_PNPM_VERSION = "5";

    public static final String INSTALL_NODE_LOCALLY = "%n  $ mvn com.github.eirslett:frontend-maven-plugin:1.10.0:install-node-and-npm "
            + "-DnodeVersion=\"" + DEFAULT_NODE_VERSION + "\" ";

    private static final String MSG_PREFIX = "%n%n======================================================================================================";
    private static final String MSG_SUFFIX = "%n======================================================================================================%n";

    private static final String NODE_NOT_FOUND = MSG_PREFIX
            + "%nVaadin requires node.js & npm to be installed. Please install the latest LTS version of node.js (with npm) either by:"
            + "%n  1) following the https://nodejs.org/en/download/ guide to install it globally. This is the recommended way."
            + "%n  2) running the following Maven plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY
            + "%n%nNote that in case you don't install it globally, you'll need to install it again for another Vaadin project."
            + "%nIn case you have just installed node.js globally, it was not discovered, so you need to restart your system to get the path variables updated."
            + MSG_SUFFIX;

    private static final String LOCAL_NODE_NOT_FOUND = MSG_PREFIX
            + "%nVaadin requires node.js & npm to be installed. The %s directory already contains 'node' but it's either not a file "
            + "or not a 'node' executable. Please check %s directory and clean up it: remove '%s'."
            + "%n then please run the app or maven goal again." + MSG_SUFFIX;

    private static final String BAD_VERSION = MSG_PREFIX
            + "%nYour installed '%s' version (%s) is known to have problems." //
            + "%nPlease update to a new one either:"
            + "%n  - by following the https://nodejs.org/en/download/ guide to install it globally"
            + "%s"
            + "%n  - or by running the frontend-maven-plugin goal to install it in this project:"
            + INSTALL_NODE_LOCALLY + "%n" //
            + FrontendUtils.DISABLE_CHECK //
            + MSG_SUFFIX;

    private static final List<FrontendVersion> NPM_BLACKLISTED_VERSIONS = Arrays
            .asList(new FrontendVersion("6.11.0"),
                    new FrontendVersion("6.11.1"),
                    new FrontendVersion("6.11.2"));

    private static final FrontendVersion WHITESPACE_ACCEPTING_NPM_VERSION = new FrontendVersion(
            7, 0);

    private static final int SUPPORTED_NODE_MAJOR_VERSION = 10;
    private static final int SUPPORTED_NODE_MINOR_VERSION = 0;
    private static final int SUPPORTED_NPM_MAJOR_VERSION = 5;
    private static final int SUPPORTED_NPM_MINOR_VERSION = 6;
    private static final int SHOULD_WORK_NODE_MAJOR_VERSION = 8;
    private static final int SHOULD_WORK_NODE_MINOR_VERSION = 9;
    private static final int SHOULD_WORK_NPM_MAJOR_VERSION = 5;
    private static final int SHOULD_WORK_NPM_MINOR_VERSION = 5;

    private static final FrontendVersion SUPPORTED_NODE_VERSION = new FrontendVersion(
            SUPPORTED_NODE_MAJOR_VERSION, SUPPORTED_NODE_MINOR_VERSION);
    private static final FrontendVersion SHOULD_WORK_NODE_VERSION = new FrontendVersion(
            SHOULD_WORK_NODE_MAJOR_VERSION, SHOULD_WORK_NODE_MINOR_VERSION);

    private static final FrontendVersion SUPPORTED_NPM_VERSION = new FrontendVersion(
            SUPPORTED_NPM_MAJOR_VERSION, SUPPORTED_NPM_MINOR_VERSION);
    private static final FrontendVersion SHOULD_WORK_NPM_VERSION = new FrontendVersion(
            SHOULD_WORK_NPM_MAJOR_VERSION, SHOULD_WORK_NPM_MINOR_VERSION);

    static final String NPMRC_NOPROXY_PROPERTY_KEY = "noproxy";
    static final String NPMRC_HTTPS_PROXY_PROPERTY_KEY = "https-proxy";
    static final String NPMRC_PROXY_PROPERTY_KEY = "proxy";

    // Proxy config properties keys (for both system properties and environment
    // variables) can be either fully upper case or fully lower case
    static final String SYSTEM_NOPROXY_PROPERTY_KEY = "NOPROXY";
    static final String SYSTEM_HTTPS_PROXY_PROPERTY_KEY = "HTTPS_PROXY";
    static final String SYSTEM_HTTP_PROXY_PROPERTY_KEY = "HTTP_PROXY";

    private static final int SUPPORTED_PNPM_MAJOR_VERSION = 5;
    private static final int SUPPORTED_PNPM_MINOR_VERSION = 0;

    private static final FrontendVersion SUPPORTED_PNPM_VERSION = new FrontendVersion(
            SUPPORTED_PNPM_MAJOR_VERSION, SUPPORTED_PNPM_MINOR_VERSION);

    private enum NpmCliTool {
        NPM("npm", "npm-cli.js"), NPX("npx", "npx-cli.js");

        private final String name;
        private final String script;

        NpmCliTool(String tool, String script) {
            this.name = tool;
            this.script = script;
        }

        String getCommand() {
            return FrontendUtils.isWindows() ? name + ".cmd" : name;
        }

        String getScript() {
            return script;
        }
    }

    private final String baseDir;
    private final Supplier<String> alternativeDirGetter;

    private final FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    private final String nodeVersion;
    private final URI nodeDownloadRoot;

    private final boolean ignoreVersionChecks;

    private final boolean forceAlternativeNode;

    /**
     * Creates an instance of the class using the {@code baseDir} as a base
     * directory to locate the tools and the directory returned by the
     * {@code alternativeDirGetter} as a directory to install tools if they are
     * not found and use it as an alternative tools location.
     * <p>
     * If {@code alternativeDir} is {@code null} tools won't be installed.
     *
     *
     * @param baseDir
     *            the base directory to locate the tools, not {@code null}
     * @param alternativeDirGetter
     *            the getter for a directory where tools will be installed if
     *            they are not found globally or in the {@code baseDir}, may be
     *            {@code null}
     * @param forceAlternativeNode
     *            force usage of node executable from alternative directory
     */
    public FrontendTools(String baseDir, Supplier<String> alternativeDirGetter,
            boolean forceAlternativeNode) {
        this(baseDir, alternativeDirGetter, DEFAULT_NODE_VERSION,
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT),
                forceAlternativeNode);
    }

    /**
     * Creates an instance of the class using the {@code baseDir} as a base
     * directory to locate the tools and the directory returned by the
     * {@code alternativeDirGetter} as a directory to install tools if they are
     * not found and use it as an alternative tools location.
     * <p>
     * If {@code alternativeDir} is {@code null} tools won't be installed.
     *
     *
     * @param baseDir
     *            the base directory to locate the tools, not {@code null}
     * @param alternativeDirGetter
     *            the getter for a directory where tools will be installed if
     *            they are not found globally or in the {@code baseDir}, may be
     *            {@code null}
     */
    public FrontendTools(String baseDir,
            Supplier<String> alternativeDirGetter) {
        this(baseDir, alternativeDirGetter, DEFAULT_NODE_VERSION,
                URI.create(NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT), false);
    }

    /**
     * Creates an instance of the class using the {@code baseDir} as a base
     * directory to locate the tools and the directory returned by the
     * {@code alternativeDirGetter} as a directory to install tools if they are
     * not found and use it as an alternative tools location.
     * <p>
     * If {@code alternativeDir} is {@code null} tools won't be installed.
     *
     *
     * @param baseDir
     *            the base directory to locate the tools, not {@code null}
     * @param alternativeDirGetter
     *            the getter for a directory where tools will be installed if
     *            they are not found globally or in the {@code baseDir}, may be
     *            {@code null}
     * @param nodeVersion
     *            The node.js version to be used when node.js is installed
     *            automatically by Vaadin, for example <code>"v16.0.0"</code>.
     *            Use {@value #DEFAULT_NODE_VERSION} by default.
     * @param nodeDownloadRoot
     *            Download node.js from this URL. Handy in heavily firewalled
     *            corporate environments where the node.js download can be
     *            provided from an intranet mirror. Use
     *            {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT} by default.
     * @param forceAlternativeNode
     *            force usage of node executable from alternative directory
     */
    public FrontendTools(String baseDir, Supplier<String> alternativeDirGetter,
            String nodeVersion, URI nodeDownloadRoot,
            boolean forceAlternativeNode) {
        this(baseDir, alternativeDirGetter, nodeVersion, nodeDownloadRoot,
                "true".equalsIgnoreCase(System.getProperty(
                        FrontendUtils.PARAM_IGNORE_VERSION_CHECKS)),
                forceAlternativeNode);
    }

    /**
     * Creates an instance of the class using the {@code baseDir} as a base
     * directory to locate the tools and the directory returned by the
     * {@code alternativeDirGetter} as a directory to install tools if they are
     * not found and use it as an alternative tools location.
     * <p>
     * If {@code alternativeDir} is {@code null} tools won't be installed.
     *
     *
     * @param baseDir
     *            the base directory to locate the tools, not {@code null}
     * @param alternativeDirGetter
     *            the getter for a directory where tools will be installed if
     *            they are not found globally or in the {@code baseDir}, may be
     *            {@code null}
     * @param nodeVersion
     *            The node.js version to be used when node.js is installed
     *            automatically by Vaadin, for example <code>"v16.0.0"</code>.
     *            Use {@value #DEFAULT_NODE_VERSION} by default.
     * @param nodeDownloadRoot
     *            Download node.js from this URL. Handy in heavily firewalled
     *            corporate environments where the node.js download can be
     *            provided from an intranet mirror. Use
     *            {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT} by default.
     */
    public FrontendTools(String baseDir, Supplier<String> alternativeDirGetter,
            String nodeVersion, URI nodeDownloadRoot) {
        this(baseDir, alternativeDirGetter, nodeVersion, nodeDownloadRoot,
                false);
    }

    FrontendTools(String baseDir, Supplier<String> alternativeDirGetter,
            String nodeVersion, URI nodeDownloadRoot,
            boolean ignoreVersionChecks, boolean forceAlternativeNode) {
        this.baseDir = Objects.requireNonNull(baseDir);
        this.alternativeDirGetter = alternativeDirGetter;
        this.nodeVersion = Objects.requireNonNull(nodeVersion);
        this.nodeDownloadRoot = Objects.requireNonNull(nodeDownloadRoot);
        this.ignoreVersionChecks = ignoreVersionChecks;
        this.forceAlternativeNode = forceAlternativeNode;
    }

    /**
     * Locate <code>node</code> executable.
     *
     * @return the full path to the executable
     */
    public String getNodeExecutable() {
        Pair<String, String> nodeCommands = getNodeCommands();
        File file = getExecutable(baseDir, nodeCommands.getSecond());
        if (file == null) {
            file = frontendToolsLocator.tryLocateTool(nodeCommands.getFirst())
                    .orElse(null);
        }
        if (file == null) {
            file = getExecutable(getAlternativeDir(), nodeCommands.getSecond());
        }
        if (file == null && alternativeDirGetter != null) {
            getLogger().info("Couldn't find {}. Installing Node and NPM to {}.",
                    nodeCommands.getFirst(), getAlternativeDir());
            file = new File(installNode(nodeVersion, nodeDownloadRoot));
        }
        if (file == null) {
            throw new IllegalStateException(String.format(NODE_NOT_FOUND));
        }
        return file.getAbsolutePath();
    }

    /**
     * Locate <code>node</code> executable from the alternative directory given.
     *
     * <p>
     * The difference between {@link #getNodeExecutable()} and this method in a
     * search algorithm: {@link #getNodeExecutable()} first searches executable
     * in the base/alternative directory and fallbacks to the globally installed
     * if it's not found there. The {@link #forceAlternativeNodeExecutable()}
     * doesn't search for globally installed executable. It tries to find it in
     * the installation directory and if it's not found it downloads and
     * installs it there.
     *
     * @see #getNodeExecutable()
     *
     * @return the full path to the executable
     */
    public String forceAlternativeNodeExecutable() {
        Pair<String, String> nodeCommands = getNodeCommands();
        String dir = getAlternativeDir();
        File file = new File(dir, nodeCommands.getSecond());
        if (file.exists()) {
            if (!frontendToolsLocator.verifyTool(file)) {
                throw new IllegalStateException(
                        String.format(LOCAL_NODE_NOT_FOUND, dir, dir,
                                file.getAbsolutePath()));
            }
            return file.getAbsolutePath();
        } else {
            getLogger().info("Node not found in {}. Installing node {}.", dir,
                    nodeVersion);
            return installNode(nodeVersion, nodeDownloadRoot);
        }
    }

    /**
     * Locate <code>npm</code> executable.
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have npm running
     */
    public List<String> getNpmExecutable() {
        return getNpmExecutable(true);
    }

    /**
     * Locate <code>pnpm</code> executable.
     * <p>
     * In case pnpm is not available it will be installed.
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have pnpm running
     */
    public List<String> getPnpmExecutable() {
        List<String> pnpmCommand = getSuitablePnpm();
        assert !pnpmCommand.isEmpty();
        pnpmCommand = new ArrayList<>(pnpmCommand);
        pnpmCommand.add("--shamefully-hoist=true");
        return pnpmCommand;
    }

    /**
     * Validate that the found node and npm versions are new enough. Throws an
     * exception with a descriptive message if a version is too old.
     */
    public void validateNodeAndNpmVersion() {
        if (ignoreVersionChecks) {
            return;
        }
        try {
            List<String> nodeVersionCommand = new ArrayList<>();
            nodeVersionCommand.add(doGetNodeExecutable());
            nodeVersionCommand.add("--version"); // NOSONAR
            FrontendVersion foundNodeVersion = FrontendUtils.getVersion("node",
                    nodeVersionCommand);
            FrontendUtils.validateToolVersion("node", foundNodeVersion,
                    SUPPORTED_NODE_VERSION, SHOULD_WORK_NODE_VERSION);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if node is new enough", e);
        }

        try {
            FrontendVersion foundNpmVersion = getNpmVersion();
            FrontendUtils.validateToolVersion("npm", foundNpmVersion,
                    SUPPORTED_NPM_VERSION, SHOULD_WORK_NPM_VERSION);
            checkForFaultyNpmVersion(foundNpmVersion);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if npm is new enough", e);
        }

    }

    /**
     * Install node and npm.
     *
     * @param nodeVersion
     *            node version to install
     * @param downloadRoot
     *            optional download root for downloading node. May be a
     *            filesystem file or a URL see
     *            {@link NodeInstaller#setNodeDownloadRoot(URI)}.
     * @return node installation path
     */
    protected String installNode(String nodeVersion, URI downloadRoot) {
        NodeInstaller nodeInstaller = new NodeInstaller(
                new File(getAlternativeDir()), getProxies())
                        .setNodeVersion(nodeVersion);
        if (downloadRoot != null) {
            nodeInstaller.setNodeDownloadRoot(downloadRoot);
        }

        try {
            nodeInstaller.install();
        } catch (InstallationException e) {
            throw new IllegalStateException("Failed to install Node", e);
        }

        return new File(nodeInstaller.getInstallDirectory(),
                getNodeCommands().getFirst()).toString();
    }

    /**
     * Read list of configured proxies in order from system properties, .npmrc
     * file in the project root folder, .npmrc file in user root folder and
     * system environment variables.
     *
     * @return list of configured proxies
     */
    // Not private because of test
    protected List<ProxyConfig.Proxy> getProxies() {
        File projectNpmrc = new File(baseDir, ".npmrc");
        File userNpmrc = new File(FileUtils.getUserDirectory(), ".npmrc");
        List<ProxyConfig.Proxy> proxyList = new ArrayList<>();

        proxyList.addAll(readProxySettingsFromSystemProperties());
        proxyList.addAll(
                readProxySettingsFromNpmrcFile("user .npmrc", userNpmrc));
        proxyList.addAll(
                readProxySettingsFromNpmrcFile("project .npmrc", projectNpmrc));
        proxyList.addAll(readProxySettingsFromEnvironmentVariables());

        return proxyList;
    }

    void checkForFaultyNpmVersion(FrontendVersion npmVersion) {
        if (NPM_BLACKLISTED_VERSIONS.contains(npmVersion)) {
            String badNpmVersion = buildBadVersionString("npm",
                    npmVersion.getFullVersion(),
                    "by updating your global npm installation with `npm install -g npm@latest`");
            throw new IllegalStateException(badNpmVersion);
        }
    }

    /**
     * Checks whether the currently installed npm version accepts/properly
     * processes the path to a given folder.
     * <p>
     * For example, the older versions of npm don't accept whitespaces in
     * folders path.
     * 
     * @param folder
     *            the folder to check.
     * @return <code>true</code>, if the current version of npm accepts the
     *         given folder path, <code>false</code> if it causes issues.
     */
    boolean folderIsAcceptableByNpm(File folder) {
        Objects.requireNonNull(folder);
        boolean hidden = folder.isHidden()
                || folder.getPath().contains(File.separator + ".");
        if (!hidden && (!folder.exists() || !folder.isDirectory())) {
            getLogger().warn(
                    "Failed to check whether npm accepts the folder '{}', because the folder doesn't exist or not a directory",
                    folder);
            return true;
        }

        if (FrontendUtils.isWindows()
                && folder.getAbsolutePath().matches(".*[\\s+].*")) {
            try {
                FrontendVersion foundNpmVersion = getNpmVersion();
                // npm < 7.0.0 doesn't accept whitespaces in path
                return FrontendUtils.isVersionAtLeast(foundNpmVersion,
                        WHITESPACE_ACCEPTING_NPM_VERSION);
            } catch (UnknownVersionException e) {
                getLogger().warn("Error checking if npm accepts path '{}'",
                        folder, e);
            }
        }
        return true;
    }

    /**
     * Gives a file object representing path to the cache directory of currently
     * installed npm.
     *
     * @return the file object representing path to npm cache directory.
     * @throws CommandExecutionException
     *             if getting the npm cache directory completes exceptionally.
     * @throws IllegalStateException
     *             if npm cache command return an empty path.
     */
    File getNpmCacheDir()
            throws CommandExecutionException, IllegalStateException {
        List<String> npmCacheCommand = new ArrayList<>(getNpmExecutable(false));
        npmCacheCommand.add("config");
        npmCacheCommand.add("get");
        npmCacheCommand.add("cache");
        npmCacheCommand.add("--global");
        String output = FrontendUtils.executeCommand(npmCacheCommand);
        output = removeLineBreaks(output);
        if (output.isEmpty()) {
            throw new IllegalStateException(
                    String.format("Command '%s' returned an empty path",
                            String.join(" ", npmCacheCommand)));
        }
        return new File(output);
    }

    private FrontendVersion getNpmVersion() throws UnknownVersionException {
        List<String> npmVersionCommand = new ArrayList<>(
                getNpmExecutable(false));
        npmVersionCommand.add("--version"); // NOSONAR
        return FrontendUtils.getVersion("npm", npmVersionCommand);
    }

    private File getExecutable(String dir, String location) {
        File file = new File(dir, location);
        if (frontendToolsLocator.verifyTool(file)) {
            return file;
        }
        return null;
    }

    private Pair<String, String> getNodeCommands() {
        if (FrontendUtils.isWindows()) {
            return new Pair<>("node.exe", "node/node.exe");
        } else {
            return new Pair<>("node", "node/node");
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(FrontendTools.class);
    }

    private List<ProxyConfig.Proxy> readProxySettingsFromNpmrcFile(
            String fileDescription, File npmrc) {
        if (!npmrc.exists()) {
            return Collections.emptyList();
        }

        try (FileReader fileReader = new FileReader(npmrc)) { // NOSONAR
            List<ProxyConfig.Proxy> proxyList = new ArrayList<>(2);
            Properties properties = new Properties();
            properties.load(fileReader);
            String noproxy = properties.getProperty(NPMRC_NOPROXY_PROPERTY_KEY);
            if (noproxy != null)
                noproxy = noproxy.replaceAll(",", "|");
            String httpsProxyUrl = properties
                    .getProperty(NPMRC_HTTPS_PROXY_PROPERTY_KEY);
            if (httpsProxyUrl != null) {
                proxyList.add(new ProxyConfig.Proxy(
                        "https-proxy - " + fileDescription, httpsProxyUrl,
                        noproxy));
            }
            String proxyUrl = properties.getProperty(NPMRC_PROXY_PROPERTY_KEY);
            if (proxyUrl != null) {
                proxyList.add(new ProxyConfig.Proxy(
                        "proxy - " + fileDescription, proxyUrl, noproxy));
            }
            return proxyList;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<ProxyConfig.Proxy> readProxySettingsFromSystemProperties() {
        List<ProxyConfig.Proxy> proxyList = new ArrayList<>(2);

        String noproxy = getNonNull(
                System.getProperty(SYSTEM_NOPROXY_PROPERTY_KEY),
                System.getProperty(SYSTEM_NOPROXY_PROPERTY_KEY.toLowerCase()));
        if (noproxy != null) {
            noproxy = noproxy.replaceAll(",", "|");
        }

        String httpsProxyUrl = getNonNull(
                System.getProperty(SYSTEM_HTTPS_PROXY_PROPERTY_KEY),
                System.getProperty(
                        SYSTEM_HTTPS_PROXY_PROPERTY_KEY.toLowerCase()));
        if (httpsProxyUrl != null) {
            proxyList.add(new ProxyConfig.Proxy("https-proxy - system",
                    httpsProxyUrl, noproxy));
        }

        String proxyUrl = getNonNull(
                System.getProperty(SYSTEM_HTTP_PROXY_PROPERTY_KEY),
                System.getProperty(
                        SYSTEM_HTTP_PROXY_PROPERTY_KEY.toLowerCase()));
        if (proxyUrl != null) {
            proxyList.add(
                    new ProxyConfig.Proxy("proxy - system", proxyUrl, noproxy));
        }

        return proxyList;
    }

    private List<ProxyConfig.Proxy> readProxySettingsFromEnvironmentVariables() {
        List<ProxyConfig.Proxy> proxyList = new ArrayList<>(2);

        String noproxy = getNonNull(System.getenv(SYSTEM_NOPROXY_PROPERTY_KEY),
                System.getenv(SYSTEM_NOPROXY_PROPERTY_KEY.toLowerCase()));
        if (noproxy != null) {
            noproxy = noproxy.replaceAll(",", "|");
        }

        String httpsProxyUrl = getNonNull(
                System.getenv(SYSTEM_HTTPS_PROXY_PROPERTY_KEY),
                System.getenv(SYSTEM_HTTPS_PROXY_PROPERTY_KEY.toLowerCase()));
        if (httpsProxyUrl != null) {
            proxyList.add(new ProxyConfig.Proxy("https-proxy - env",
                    httpsProxyUrl, noproxy));
        }

        String proxyUrl = getNonNull(
                System.getenv(SYSTEM_HTTP_PROXY_PROPERTY_KEY),
                System.getenv(SYSTEM_HTTP_PROXY_PROPERTY_KEY.toLowerCase()));
        if (proxyUrl != null) {
            proxyList.add(
                    new ProxyConfig.Proxy("proxy - env", proxyUrl, noproxy));
        }

        return proxyList;
    }

    /**
     * Get the first non null value from the given array.
     *
     * @param valueArray
     *            array of values to get non null from
     * @return first non null value or null if no values found
     */
    private String getNonNull(String... valueArray) {
        for (String value : valueArray) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<String> getNpmExecutable(boolean removePnpmLock) {
        List<String> returnCommand = new ArrayList<>(
                getNpmCliToolExecutable(NpmCliTool.NPM));
        returnCommand.add("--no-update-notifier");
        returnCommand.add("--no-audit");
        returnCommand.add("--scripts-prepend-node-path=true");

        if (removePnpmLock) {
            // remove pnpm-lock.yaml which contains pnpm as a dependency.
            if (new File(baseDir, "pnpm-lock.yaml").delete()) {
                getLogger().debug(
                        "pnpm-lock.yaml file is removed from " + baseDir);
            }
        }

        return returnCommand;
    }

    private List<String> getNpmCliToolExecutable(NpmCliTool cliTool,
            String... flags) {
        // First look for *-cli.js script in project/node_modules
        List<String> returnCommand = getNpmScriptCommand(baseDir,
                cliTool.getScript());
        boolean alternativeDirChecked = false;
        if (returnCommand.isEmpty() && forceAlternativeNode) {
            // First look for *-cli.js script in ~/.vaadin/node/node_modules
            // only if alternative node takes precedence over all other location
            returnCommand = getNpmScriptCommand(getAlternativeDir(),
                    cliTool.getScript());
            alternativeDirChecked = true;
        }
        if (returnCommand.isEmpty()) {
            // Otherwise look for regular `npm`/`npx` global search path
            Optional<String> command = frontendToolsLocator
                    .tryLocateTool(cliTool.getCommand())
                    .map(File::getAbsolutePath);
            if (command.isPresent()) {
                returnCommand = Collections.singletonList(command.get());
            }
        }
        if (!alternativeDirChecked && returnCommand.isEmpty()) {
            // Use alternative if global is not found and alternative location
            // is not yet checked
            returnCommand = getNpmScriptCommand(getAlternativeDir(),
                    cliTool.getScript());
        }

        if (flags.length > 0) {
            returnCommand = new ArrayList<>(returnCommand);
            Collections.addAll(returnCommand, flags);
        }
        return returnCommand;
    }

    private List<String> getNpmScriptCommand(String dir, String scriptName) {
        // If `node` is not found in PATH, `node/node_modules/npm/bin/npm` will
        // not work because it's a shell or windows script that looks for node
        // and will fail. Thus we look for the `npm-cli` node script instead
        File file = new File(dir, "node/node_modules/npm/bin/" + scriptName);
        List<String> returnCommand = new ArrayList<>();
        if (file.canRead()) {
            // We return a two element list with node binary and npm-cli script
            returnCommand.add(doGetNodeExecutable());
            returnCommand.add(file.getAbsolutePath());
        }
        return returnCommand;
    }

    List<String> getSuitablePnpm() {
        // install pnpm version < 6.0.0, later requires ensuring
        // NodeJS >= 12.17
        final String pnpmSpecifier = ignoreVersionChecks ? "pnpm"
                : "pnpm@" + DEFAULT_PNPM_VERSION + "+";
        List<String> pnpmCommand = Stream.of(pnpmSpecifier)
                // do NOT modify the order of the --yes and --quiet flags, as it
                // changes the behavior of npx
                .map(pnpm -> getNpmCliToolExecutable(NpmCliTool.NPX, "--yes",
                        "--quiet", pnpm))
                .filter(this::validatePnpmVersion).findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Found too old 'pnpm'. If installed into the project "
                                + "'node_modules', upgrade 'pnpm' to at least "
                                + SUPPORTED_PNPM_VERSION.getFullVersion()));
        getLogger().info("using '{}' for frontend package installation",
                String.join(" ", pnpmCommand));
        return pnpmCommand;
    }

    private boolean validatePnpmVersion(List<String> pnpmCommand) {
        String commandLine = String.join(" ", pnpmCommand);
        try {
            List<String> versionCmd = new ArrayList<>(pnpmCommand);
            versionCmd.add("--version"); // NOSONAR
            FrontendVersion pnpmVersion = FrontendUtils.getVersion("pnpm",
                    versionCmd);
            boolean versionNewEnough = FrontendUtils
                    .isVersionAtLeast(pnpmVersion, SUPPORTED_PNPM_VERSION);
            boolean versionAccepted = ignoreVersionChecks || versionNewEnough;
            if (!versionAccepted) {
                getLogger().info(
                        "pnpm '{}' is version {} which is not supported (expected >={})",
                        commandLine, pnpmVersion.getFullVersion(),
                        SUPPORTED_PNPM_VERSION.getFullVersion());
            }
            return versionAccepted;
        } catch (UnknownVersionException e) {
            getLogger().warn("version check '{}' failed", commandLine, e);
            return false;
        }
    }

    private String buildBadVersionString(String tool, String version,
            String... extraUpdateInstructions) {
        StringBuilder extraInstructions = new StringBuilder();
        for (String instruction : extraUpdateInstructions) {
            extraInstructions.append("%n  - or ").append(instruction);
        }
        return String.format(BAD_VERSION, tool, version,
                extraInstructions.toString(),
                FrontendUtils.PARAM_IGNORE_VERSION_CHECKS);
    }

    private String getAlternativeDir() {
        return alternativeDirGetter.get();
    }

    private String doGetNodeExecutable() {
        if (forceAlternativeNode) {
            return forceAlternativeNodeExecutable();
        } else {
            return getNodeExecutable();
        }
    }

    private String removeLineBreaks(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return String.join("", str.split(System.lineSeparator()));
    }
}
