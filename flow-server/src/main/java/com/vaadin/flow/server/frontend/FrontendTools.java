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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendUtils.CommandExecutionException;
import com.vaadin.flow.server.frontend.FrontendUtils.UnknownVersionException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;

/**
 * Provides access to frontend tools (Node.js and npm, pnpm, bun) and optionally
 * installs the tools if needed.
 * <p>
 * <b>WARNING:</b> This class is intended for internal usage only. May be
 * renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public class FrontendTools {

    /**
     * This is the version that is installed if there is no node installed or
     * the installed version is older than {@link #SUPPORTED_NODE_VERSION}, i.e.
     * {@value #SUPPORTED_NODE_MAJOR_VERSION}.{@value #SUPPORTED_NODE_MINOR_VERSION}.
     */
    public static final String DEFAULT_NODE_VERSION = "v24.10.0";
    /**
     * This is the version shipped with the default Node version.
     */
    public static final String DEFAULT_NPM_VERSION = "11.6.0";

    public static final String DEFAULT_PNPM_VERSION = "10.24.0";

    private static final String MSG_PREFIX = "%n%n======================================================================================================";
    private static final String MSG_SUFFIX = "%n======================================================================================================%n";

    private static final String PNPM_NOT_FOUND = MSG_PREFIX
            + "%nVaadin is configured to use a globally installed pnpm ('pnpm.global=true'), but pnpm was not found on your system."
            + "%nInstall pnpm by following the instruction at https://pnpm.io/installation "
            + "%nor exclude 'pnpm.global' from the configuration or set it to false."
            + MSG_SUFFIX;
    private static final String BUN_NOT_FOUND = MSG_PREFIX
            + "%nVaadin is configured to use a globally installed bun, but bun was not found on your system."
            + "%nInstall bun by following the instruction at https://bun.sh "
            + MSG_SUFFIX;

    private static final String LOCAL_NODE_NOT_FOUND = MSG_PREFIX
            + "%nVaadin requires Node.js and npm to be installed. The %s directory already contains 'node' but it's either not a file "
            + "or not a 'node' executable. Please check the %s directory and clean it up: remove '%s'."
            + "%n then run the application or Maven goal again." + MSG_SUFFIX;

    private static final String BAD_VERSION = MSG_PREFIX
            + "%nYour installed '%s' version (%s) is known to have problems." //
            + "%nPlease update it%s." + "%n" //
            + FrontendUtils.DISABLE_CHECK //
            + MSG_SUFFIX;

    private static final List<FrontendVersion> BAD_NPM_VERSIONS = Collections
            .singletonList(new FrontendVersion("9.2.0"));

    private static final FrontendVersion WHITESPACE_ACCEPTING_NPM_VERSION = new FrontendVersion(
            7, 0);

    private static final int SUPPORTED_NODE_MAJOR_VERSION = 24;
    private static final int SUPPORTED_NODE_MINOR_VERSION = 0;
    /**
     * Maximum supported Node.js major version. Versions with a higher major
     * version are not tested and may not be compatible.
     */
    public static final int MAX_SUPPORTED_NODE_MAJOR_VERSION = 24;
    private static final int SUPPORTED_NPM_MAJOR_VERSION = 11;
    private static final int SUPPORTED_NPM_MINOR_VERSION = 3;

    public static final FrontendVersion SUPPORTED_NODE_VERSION = new FrontendVersion(
            SUPPORTED_NODE_MAJOR_VERSION, SUPPORTED_NODE_MINOR_VERSION);

    /**
     * Minimum Node.js version for auto-installed versions in ~/.vaadin. Global
     * installations are accepted if they meet SUPPORTED_NODE_VERSION, but
     * auto-installed versions must meet this higher threshold.
     */
    public static final FrontendVersion MINIMUM_AUTO_INSTALLED_NODE = new FrontendVersion(
            24, 10, 0);

    private static final FrontendVersion SUPPORTED_NPM_VERSION = new FrontendVersion(
            SUPPORTED_NPM_MAJOR_VERSION, SUPPORTED_NPM_MINOR_VERSION);

    private static final int SUPPORTED_PNPM_MAJOR_VERSION = 7;
    private static final int SUPPORTED_PNPM_MINOR_VERSION = 0;

    private static final FrontendVersion SUPPORTED_PNPM_VERSION = new FrontendVersion(
            SUPPORTED_PNPM_MAJOR_VERSION, SUPPORTED_PNPM_MINOR_VERSION);
    private static final FrontendVersion SUPPORTED_BUN_VERSION = new FrontendVersion(
            1, 0, 6); // Bun 1.0.6 is the first version with "overrides" support

    private enum BuildTool {
        NPM("npm", "npm-cli.js"),
        NPX("npx", "npx-cli.js"),
        PNPM("pnpm", null),
        BUN("bun", null);

        private final String name;
        private final String script;

        BuildTool(String tool, String script) {
            this.name = tool;
            this.script = script;
        }

        String getCommand() {
            if (name.equals("bun")) {
                return name;
            }
            return FrontendUtils.isWindows() ? name + ".cmd" : name;
        }

        String getScript() {
            if (script == null) {
                throw new RuntimeException(String.format(
                        "'%s' build tool doesn't have a CLI script", name));
            }
            return script;
        }
    }

    private final String baseDir;
    private final Supplier<String> alternativeDirGetter;

    private final FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

    // The active node installation - shared across all FrontendTools instances
    private static volatile NodeResolver.ActiveNodeInstallation activeNodeInstallation;

    // Lock object for synchronizing node resolution
    private static final Object RESOLUTION_LOCK = new Object();

    private final String nodeVersion;
    private final URI nodeDownloadRoot;

    private final boolean ignoreVersionChecks;
    private final boolean forceAlternativeNode;
    private final boolean useGlobalPnpm;

    /**
     * Creates an instance of the class using the {@code baseDir} as a base
     * directory to locate the tools and the directory returned by the
     * {@code alternativeDirGetter} as a directory to install tools if they are
     * not found and use it as an alternative tools location.
     * <p>
     * If {@code alternativeDir} is {@code null} tools won't be installed.
     * <p>
     * Note: settings for this object can not be changed through the settings
     * object after creation.
     *
     * @param settings
     *            tooling settings to use
     */
    public FrontendTools(FrontendToolsSettings settings) {
        this.baseDir = Objects.requireNonNull(settings.getBaseDir());
        this.alternativeDirGetter = settings.getAlternativeDirGetter();
        this.nodeVersion = Objects.requireNonNull(settings.getNodeVersion());
        this.nodeDownloadRoot = Objects
                .requireNonNull(settings.getNodeDownloadRoot());
        this.ignoreVersionChecks = settings.isIgnoreVersionChecks();
        this.forceAlternativeNode = settings.isForceAlternativeNode();
        this.useGlobalPnpm = settings.isUseGlobalPnpm();
    }

    /**
     * Creates an instance using the the given project directory and application
     * configuration.
     *
     * @param projectRoot
     *            the project root directory
     * @param applicationConfiguration
     *            the configuration for the application
     */
    public FrontendTools(ApplicationConfiguration applicationConfiguration,
            File projectRoot) {
        this(createSettings(applicationConfiguration, projectRoot));
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
     *            The Node.js version to be used when Node.js is installed
     *            automatically by Vaadin, for example <code>"v16.0.0"</code>.
     *            Use {@value #DEFAULT_NODE_VERSION} by default.
     * @param nodeDownloadRoot
     *            Download Node.js from this URL. Handy in heavily firewalled
     *            corporate environments where the Node.js download can be
     *            provided from an intranet mirror. Use
     *            {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT} by default.
     * @param forceAlternativeNode
     *            force usage of node executable from alternative directory
     * @param useGlobalPnpm
     *            use globally installed pnpm instead of the default one (see
     *            {@link #DEFAULT_PNPM_VERSION})
     * @deprecated use
     *             {@link FrontendTools#FrontendTools(FrontendToolsSettings)}
     *             instead, as it simplifies configuring the frontend tools and
     *             gives the default values to configuration parameters.
     */
    @Deprecated
    public FrontendTools(String baseDir, Supplier<String> alternativeDirGetter,
            String nodeVersion, URI nodeDownloadRoot,
            boolean forceAlternativeNode, boolean useGlobalPnpm) {
        this(baseDir, alternativeDirGetter, nodeVersion, nodeDownloadRoot,
                "true".equalsIgnoreCase(System.getProperty(
                        FrontendUtils.PARAM_IGNORE_VERSION_CHECKS)),
                forceAlternativeNode, useGlobalPnpm);
    }

    FrontendTools(String baseDir, Supplier<String> alternativeDirGetter,
            String nodeVersion, URI nodeDownloadRoot,
            boolean ignoreVersionChecks, boolean forceAlternativeNode,
            boolean useGlobalPnpm) {
        this.baseDir = Objects.requireNonNull(baseDir);
        this.alternativeDirGetter = alternativeDirGetter;
        this.nodeVersion = Objects.requireNonNull(nodeVersion);
        this.nodeDownloadRoot = Objects.requireNonNull(nodeDownloadRoot);
        this.ignoreVersionChecks = ignoreVersionChecks;
        this.forceAlternativeNode = forceAlternativeNode;
        this.useGlobalPnpm = useGlobalPnpm;
    }

    private static FrontendToolsSettings createSettings(
            ApplicationConfiguration applicationConfiguration,
            File projectRoot) {
        boolean useHomeNodeExec = applicationConfiguration.getBooleanProperty(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, false);
        boolean useGlobalPnpm = applicationConfiguration.getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM, false);
        final String nodeVersion = applicationConfiguration.getStringProperty(
                NODE_VERSION, FrontendTools.DEFAULT_NODE_VERSION);
        final String nodeDownloadRoot = applicationConfiguration
                .getStringProperty(NODE_DOWNLOAD_ROOT,
                        Platform.guess().getNodeDownloadRoot());

        FrontendToolsSettings settings = new FrontendToolsSettings(
                projectRoot.getAbsolutePath(),
                () -> FrontendUtils.getVaadinHomeDirectory().getAbsolutePath());
        settings.setForceAlternativeNode(useHomeNodeExec);
        settings.setUseGlobalPnpm(useGlobalPnpm);
        settings.setNodeVersion(nodeVersion);
        settings.setNodeDownloadRoot(URI.create(nodeDownloadRoot));
        settings.setIgnoreVersionChecks(false);
        return settings;
    }

    /**
     * Locate <code>node</code> executable.
     *
     * @return the full path to the executable
     */
    public String getNodeExecutable() {
        return ensureNodeResolved().nodeExecutable();
    }

    /**
     * Ensures that node has been resolved and cached. Uses double-checked
     * locking to ensure thread-safe lazy initialization.
     *
     * @return the active node installation information
     */
    private NodeResolver.ActiveNodeInstallation ensureNodeResolved() {
        NodeResolver.ActiveNodeInstallation active = activeNodeInstallation;
        if (active != null) {
            return active;
        }

        synchronized (RESOLUTION_LOCK) {
            // Double-check after acquiring lock
            active = activeNodeInstallation;
            if (active != null) {
                return active;
            }

            // Perform resolution
            if (alternativeDirGetter == null) {
                throw new IllegalStateException(
                        "Node not found and no alternative directory configured for installation");
            }

            NodeResolver resolver = new NodeResolver(getAlternativeDir(),
                    nodeVersion, nodeDownloadRoot, forceAlternativeNode,
                    getProxies());
            activeNodeInstallation = resolver.resolve();
            return activeNodeInstallation;
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
     * Locate <code>bun</code> executable.
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have bun running
     */
    public List<String> getBunExecutable() {
        List<String> bunCommand = getSuitableBun();
        assert !bunCommand.isEmpty();
        bunCommand = new ArrayList<>(bunCommand);
        return bunCommand;
    }

    /**
     * Validate that the found node and npm versions are new enough. Throws an
     * exception with a descriptive message if a version is too old.
     */
    public void validateNodeAndNpmVersion() {
        if (ignoreVersionChecks) {
            return;
        }
        // Node version is already validated by NodeResolver, which ensures
        // we have a suitable version (either from global PATH or auto-installed
        // to ~/.vaadin). Just log which node we're using.
        try {
            Pair<FrontendVersion, String> foundNodeVersionAndExe = getNodeVersionAndExecutable();
            getLogger().debug("Using node {} located at {}",
                    foundNodeVersionAndExe.getFirst().getFullVersion(),
                    foundNodeVersionAndExe.getSecond());
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking node version", e);
        }

        // Validate npm version (npm comes bundled with node)
        try {
            FrontendVersion foundNpmVersion = getNpmVersion();
            getLogger().debug("Using npm {} located at {}",
                    foundNpmVersion.getFullVersion(),
                    getNpmExecutable(false).get(0));

            // If npm is too old, this is an internal configuration error - the
            // node version we accept/install should always come with a suitable
            // npm
            if (foundNpmVersion.isOlderThan(SUPPORTED_NPM_VERSION)) {
                throw new IllegalStateException(String.format(
                        "Internal error: npm version %s is older than required %s. "
                                + "This should not happen as Node %s should bundle a compatible npm version. "
                                + "Please report this issue.",
                        foundNpmVersion.getFullVersion(),
                        SUPPORTED_NPM_VERSION.getFullVersion(),
                        DEFAULT_NODE_VERSION));
            }

            checkForFaultyNpmVersion(foundNpmVersion);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking npm version", e);
        }

    }

    /**
     * Gets the version of the node executable.
     *
     * @return the version of the node executable
     * @throws UnknownVersionException
     *             if the node version cannot be determined
     */
    public FrontendVersion getNodeVersion() throws UnknownVersionException {
        return getNodeVersionAndExecutable().getFirst();
    }

    private Pair<FrontendVersion, String> getNodeVersionAndExecutable()
            throws UnknownVersionException {
        String executable = getNodeBinary();
        List<String> nodeVersionCommand = new ArrayList<>();
        nodeVersionCommand.add(executable);
        nodeVersionCommand.add("--version"); // NOSONAR
        return new Pair<>(FrontendUtils.getVersion("node", nodeVersionCommand),
                executable);
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
        return ProxyFactory.getProxies(new File(baseDir));
    }

    void checkForFaultyNpmVersion(FrontendVersion npmVersion) {
        if (BAD_NPM_VERSIONS.contains(npmVersion)) {
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
                return foundNpmVersion
                        .isEqualOrNewer(WHITESPACE_ACCEPTING_NPM_VERSION);
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

    /**
     * Executes <code>npm --version</code> to and parses the result.
     *
     * @return the version of npm.
     * @throws UnknownVersionException
     *             if the npm command fails or returns unexpected output.
     */
    public FrontendVersion getNpmVersion() throws UnknownVersionException {
        List<String> npmVersionCommand = new ArrayList<>(
                getNpmExecutable(false));
        npmVersionCommand.add("--version"); // NOSONAR
        return FrontendUtils.getVersion("npm", npmVersionCommand);
    }

    /**
     * Gives a path to the executable (bin) JS file of the given package using
     * the native node resolution mechanism.
     *
     * @param packageName
     *            the name of the package.
     * @param binName
     *            the name of the specific executable.
     * @param cwd
     *            the current working directory.
     * @return the path to the executable.
     * @throws CommandExecutionException
     *             if the node resolution fails.
     */
    public Path getNpmPackageExecutable(String packageName, String binName,
            File cwd) throws CommandExecutionException {
        var script = """
                var jsonPath = require.resolve('%s/package.json');
                var json = require(jsonPath);
                console.log(path.resolve(path.dirname(jsonPath), json.bin['%s']));
                """
                .formatted(packageName, binName);
        return Paths.get(FrontendUtils
                .executeCommand(List.of(getNodeExecutable(), "--eval", script),
                        (builder) -> builder.directory(cwd))
                .trim());
    }

    /**
     * Returns flags required to pass to Node for Webpack to function. Determine
     * whether webpack requires Node.js to be started with the
     * --openssl-legacy-provider parameter. This is a webpack 4 workaround of
     * the issue https://github.com/webpack/webpack/issues/14532 See:
     * https://github.com/vaadin/flow/issues/12649
     *
     * @return the flags
     * @deprecated Webpack is not used anymore, this method is obsolete and have
     *             no replacements.
     */
    @Deprecated(forRemoval = true, since = "24.8")
    public Map<String, String> getWebpackNodeEnvironment() {
        Map<String, String> environment = new HashMap<>();
        ProcessBuilder processBuilder = new ProcessBuilder()
                .command(getNodeExecutable(), "-p", "crypto.createHash('md4')");
        try {
            Process process = processBuilder.start();
            int errorLevel = process.waitFor();
            if (errorLevel != 0) {
                environment.put("NODE_OPTIONS", "--openssl-legacy-provider");
            }
        } catch (IOException e) {
            getLogger().error(
                    "IO error while determining --openssl-legacy-provider "
                            + "parameter requirement",
                    e);
        } catch (InterruptedException e) {
            getLogger().error(
                    "Interrupted while determining --openssl-legacy-provider "
                            + "parameter requirement",
                    e);
            // re-interrupt the thread
            Thread.currentThread().interrupt();
        }
        return environment;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(FrontendTools.class);
    }

    private List<String> getNpmExecutable(boolean removePnpmLock) {
        List<String> returnCommand = new ArrayList<>(
                getNpmCliToolExecutable(BuildTool.NPM));
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

    private List<String> getNpmCliToolExecutable(BuildTool cliTool,
            String... flags) {
        List<String> returnCommand = new ArrayList<>();

        // For npm/npx, we always use the resolved node installation
        if (cliTool.equals(BuildTool.NPM) || cliTool.equals(BuildTool.NPX)) {
            NodeResolver.ActiveNodeInstallation active = ensureNodeResolved();
            returnCommand.add(active.nodeExecutable());

            if (cliTool.equals(BuildTool.NPM)) {
                returnCommand.add(active.npmCliScript());
            } else {
                // NPX is in the same directory as npm, just different filename
                File npmCliFile = new File(active.npmCliScript());
                File npxCliFile = new File(npmCliFile.getParentFile(),
                        "npx-cli.js");
                if (!npxCliFile.exists()) {
                    throw new IllegalStateException(
                            "npx-cli.js not found at expected location: "
                                    + npxCliFile.getAbsolutePath());
                }
                returnCommand.add(npxCliFile.getAbsolutePath());
            }
        }

        if (flags.length > 0) {
            Collections.addAll(returnCommand, flags);
        }
        return returnCommand;
    }

    List<String> getSuitablePnpm() {
        List<String> pnpmCommand;
        if (useGlobalPnpm) {
            // try to locate already installed global pnpm, throw an exception
            // if pnpm not found or its version is too old (< 5).
            pnpmCommand = frontendToolsLocator
                    .tryLocateTool(BuildTool.PNPM.getCommand())
                    .map(File::getAbsolutePath).map(Collections::singletonList)
                    .orElseThrow(() -> new IllegalStateException(
                            String.format(PNPM_NOT_FOUND)));
            pnpmCommand = Stream.of(pnpmCommand)
                    .filter(this::validatePnpmVersion).findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Found too old globally installed 'pnpm'. Please upgrade 'pnpm' to at least "
                                    + SUPPORTED_PNPM_VERSION.getFullVersion()));
        } else {
            // install latest pnpm version as the minimum node requirement is
            // now at nodejs 16.14.0
            // see https://pnpm.io/installation#compatibility
            pnpmCommand = getNpmCliToolExecutable(BuildTool.NPX, "--yes",
                    "--quiet", "pnpm");
            if (!validatePnpmVersion(pnpmCommand)) {
                throw new IllegalStateException(
                        "Found too old globally installed 'pnpm'. Please upgrade 'pnpm' to at least "
                                + SUPPORTED_PNPM_VERSION.getFullVersion());
            }
        }
        return pnpmCommand;
    }

    List<String> getSuitableBun() {
        List<String> bunCommand;
        bunCommand = frontendToolsLocator
                .tryLocateTool(BuildTool.BUN.getCommand())
                .map(File::getAbsolutePath).map(Collections::singletonList)
                .orElseThrow(() -> new IllegalStateException(
                        String.format(BUN_NOT_FOUND)));
        bunCommand = Stream.of(bunCommand).filter(this::validateBunVersion)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Found too old globally installed 'bun'. Please upgrade 'bun' to at least "
                                + SUPPORTED_BUN_VERSION.getFullVersion()));
        return bunCommand;
    }

    private boolean validatePnpmVersion(List<String> pnpmCommand) {
        String commandLine = String.join(" ", pnpmCommand);
        try {
            List<String> versionCmd = new ArrayList<>(pnpmCommand);
            versionCmd.add("--version"); // NOSONAR
            FrontendVersion pnpmVersion = FrontendUtils.getVersion("pnpm",
                    versionCmd);
            boolean versionNewEnough = pnpmVersion
                    .isEqualOrNewer(SUPPORTED_PNPM_VERSION);
            boolean versionAccepted = ignoreVersionChecks || versionNewEnough;
            if (!versionAccepted) {
                getLogger().warn(
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

    private boolean validateBunVersion(List<String> bunCommand) {
        String commandLine = String.join(" ", bunCommand);
        try {
            List<String> versionCmd = new ArrayList<>(bunCommand);
            versionCmd.add("--version"); // NOSONAR
            FrontendVersion bunVersion = FrontendUtils.getVersion("bun",
                    versionCmd);
            boolean versionNewEnough = bunVersion
                    .isEqualOrNewer(SUPPORTED_BUN_VERSION);
            boolean versionAccepted = ignoreVersionChecks || versionNewEnough;
            if (!versionAccepted) {
                getLogger().warn(
                        "bun '{}' is version {} which is not supported (expected >={})",
                        commandLine, bunVersion.getFullVersion(),
                        SUPPORTED_BUN_VERSION.getFullVersion());
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
            extraInstructions.append(System.lineSeparator()).append("  - or ")
                    .append(instruction);
        }
        return String.format(BAD_VERSION, tool, version,
                extraInstructions.toString(),
                FrontendUtils.PARAM_IGNORE_VERSION_CHECKS);
    }

    private String getAlternativeDir() {
        return alternativeDirGetter.get();
    }

    /**
     * Gets a path to the used node binary.
     *
     * The return value can be used when executing node commands, as the first
     * part of a process builder command.
     *
     * @return the path to the node binary
     */
    public String getNodeBinary() {
        return getNodeExecutable();
    }

    private String removeLineBreaks(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return String.join("", str.split(System.lineSeparator()));
    }
}
