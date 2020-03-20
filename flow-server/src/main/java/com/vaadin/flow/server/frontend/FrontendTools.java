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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.FrontendUtils.UnknownVersionException;
import com.vaadin.flow.server.frontend.installer.InstallationException;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.ProxyConfig;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Provides access to frontend tools (node.js and npm, pnpm) and optionally
 * installs the tools if needed.
 * <p>
 * <b>WARNING:</b> This class is intended for internal usage only.
 *
 * @author Vaadin Ltd
 *
 */
public class FrontendTools {

    public static final String INSTALL_NODE_LOCALLY = "%n  $ mvn com.github.eirslett:frontend-maven-plugin:1.7.6:install-node-and-npm -DnodeVersion=\"v12.14.0\" ";

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

    private static final String PNMP_INSTALLED_BY_NPM_FOLDER = "node_modules/pnpm/";

    private static final String PNMP_INSTALLED_BY_NPM = PNMP_INSTALLED_BY_NPM_FOLDER
            + "bin/pnpm.js";

    private static final FrontendVersion SUPPORTED_NODE_VERSION = new FrontendVersion(
            Constants.SUPPORTED_NODE_MAJOR_VERSION,
            Constants.SUPPORTED_NODE_MINOR_VERSION);
    private static final FrontendVersion SHOULD_WORK_NODE_VERSION = new FrontendVersion(
            Constants.SHOULD_WORK_NODE_MAJOR_VERSION,
            Constants.SHOULD_WORK_NODE_MINOR_VERSION);

    private static final FrontendVersion SUPPORTED_NPM_VERSION = new FrontendVersion(
            Constants.SUPPORTED_NPM_MAJOR_VERSION,
            Constants.SUPPORTED_NPM_MINOR_VERSION);
    private static final FrontendVersion SHOULD_WORK_NPM_VERSION = new FrontendVersion(
            Constants.SHOULD_WORK_NPM_MAJOR_VERSION,
            Constants.SHOULD_WORK_NPM_MINOR_VERSION);

    static final String NPMRC_NOPROXY_PROPERTY_KEY = "noproxy";
    static final String NPMRC_HTTPS_PROXY_PROPERTY_KEY = "https-proxy";
    static final String NPMRC_PROXY_PROPERTY_KEY = "proxy";

    // Proxy config properties keys (for both system properties and environment
    // variables) can be either fully upper case or fully lower case
    static final String SYSTEM_NOPROXY_PROPERTY_KEY = "NOPROXY";
    static final String SYSTEM_HTTPS_PROXY_PROPERTY_KEY = "HTTPS_PROXY";
    static final String SYSTEM_HTTP_PROXY_PROPERTY_KEY = "HTTP_PROXY";

    private static final FrontendVersion SUPPORTED_PNPM_VERSION = new FrontendVersion(
            Constants.SUPPORTED_PNPM_MAJOR_VERSION,
            Constants.SUPPORTED_PNPM_MINOR_VERSION);

    private final String baseDir;
    private final Supplier<String> alternativeDirGetter;

    private final FrontendToolsLocator frontendToolsLocator = new FrontendToolsLocator();

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
        this.baseDir = Objects.requireNonNull(baseDir);
        this.alternativeDirGetter = alternativeDirGetter;
    }

    /**
     * Locate <code>node</code> executable.
     *
     * @return the full path to the executable
     */
    public String getNodeExecutable() {
        Pair<String, String> nodeCommands = getNodeCommands();
        return getExecutable(nodeCommands.getFirst(), nodeCommands.getSecond(),
                alternativeDirGetter != null).getAbsolutePath();
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
                    Constants.DEFAULT_NODE_VERSION);
            return installNode(Constants.DEFAULT_NODE_VERSION, null);
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
     * @see #getPnpmExecutable(String, boolean)
     */
    public List<String> getPnpmExecutable() {
        ensurePnpm();
        List<String> pnpmCommand = getPnpmExecutable(baseDir, true);
        if (!pnpmCommand.isEmpty()) {
            pnpmCommand.add("--shamefully-hoist=true");
        }
        return pnpmCommand;
    }

    /**
     * Ensure that pnpm tool is available and install it if it's not.
     *
     */
    public void ensurePnpm() {
        if (isPnpmTooOldOrAbsent(baseDir)) {
            // copy the current content of package.json file to a temporary
            // location
            File packageJson = new File(baseDir, "package.json");
            File tempFile = null;
            boolean packageJsonExists = packageJson.canRead();
            if (packageJsonExists) {
                try {
                    tempFile = File.createTempFile("package", "json");
                    FileUtils.copyFile(packageJson, tempFile);
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Couldn't make a copy of package.json file",
                            exception);
                }
                packageJson.delete();
            }
            try {
                JsonObject pkgJson = Json.createObject();
                pkgJson.put("name", "temp");
                pkgJson.put("license", "UNLICENSED");
                pkgJson.put("repository", "npm/npm");
                pkgJson.put("description",
                        "Temporary package for pnpm installation");
                FileUtils.writeLines(packageJson,
                        Collections.singletonList(pkgJson.toJson()));
                JsonObject lockJson = Json.createObject();
                lockJson.put("lockfileVersion", 1);
                FileUtils.writeLines(new File(baseDir, "package-lock.json"),
                        Collections.singletonList(lockJson.toJson()));
            } catch (IOException e) {
                getLogger().warn("Couldn't create temporary package.json");
            }
            LoggerFactory.getLogger("dev-updater").info(
                    "Installing pnpm v{} locally. It is suggested to install it globally using 'npm add -g pnpm@{}'",
                    Constants.DEFAULT_PNPM_VERSION,
                    Constants.DEFAULT_PNPM_VERSION);
            // install pnpm locally using npm
            installPnpm(getNpmExecutable(false));

            // remove package-lock.json which contains pnpm as a dependency.
            new File(baseDir, "package-lock.json").delete();

            if (packageJsonExists && tempFile != null) {
                // return back the original package.json
                try {
                    FileUtils.copyFile(tempFile, packageJson);
                } catch (IOException exception) {
                    throw new IllegalStateException(
                            "Couldn't restore package.json file back",
                            exception);
                }
                tempFile.delete();
            }
        }
    }

    /**
     * Validate that the found node and npm versions are new enough. Throws an
     * exception with a descriptive message if a version is too old.
     */
    public void validateNodeAndNpmVersion() {
        try {
            List<String> nodeVersionCommand = new ArrayList<>();
            nodeVersionCommand.add(getNodeExecutable());
            nodeVersionCommand.add("--version"); // NOSONAR
            FrontendVersion nodeVersion = FrontendUtils.getVersion("node",
                    nodeVersionCommand);
            FrontendUtils.validateToolVersion("node", nodeVersion,
                    SUPPORTED_NODE_VERSION, SHOULD_WORK_NODE_VERSION);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if node is new enough", e);
        }

        try {
            List<String> npmVersionCommand = new ArrayList<>(
                    getNpmExecutable(false));
            npmVersionCommand.add("--version"); // NOSONAR
            FrontendVersion npmVersion = FrontendUtils.getVersion("npm",
                    npmVersionCommand);
            FrontendUtils.validateToolVersion("npm", npmVersion,
                    SUPPORTED_NPM_VERSION, SHOULD_WORK_NPM_VERSION);
            checkForFaultyNpmVersion(npmVersion);
        } catch (UnknownVersionException e) {
            getLogger().warn("Error checking if npm is new enough", e);
        }

    }

    /**
     * Locate <code>pnpm</code> executable if it's possible.
     * <p>
     * In case the tool is not found either {@link IllegalStateException} is
     * thrown or an empty list is returned depending on {@code failOnAbsence}
     * value.
     *
     * @param dir
     *            the directory to search local pnpm script
     *
     * @param failOnAbsence
     *            if {@code true} throws IllegalStateException if tool is not
     *            found, if {@code false} return an empty list if tool is not
     *            found
     *
     * @return the list of all commands in sequence that need to be executed to
     *         have pnpm running
     */
    protected List<String> getPnpmExecutable(String dir,
            boolean failOnAbsence) {
        // First try local pnpm JS script if it exists
        List<String> returnCommand = new ArrayList<>();
        Optional<File> localPnpmScript = getLocalPnpmScript(dir);
        if (localPnpmScript.isPresent()) {
            returnCommand.add(getNodeExecutable());
            returnCommand.add(localPnpmScript.get().getAbsolutePath());
        } else {
            // Otherwise look for regular `pnpm`
            String command = FrontendUtils.isWindows() ? "pnpm.cmd" : "pnpm";
            if (failOnAbsence) {
                returnCommand.add(
                        getExecutable(command, null, false).getAbsolutePath());
            } else {
                returnCommand.addAll(frontendToolsLocator.tryLocateTool(command)
                        .map(File::getPath).map(Collections::singletonList)
                        .orElse(Collections.emptyList()));
            }
        }
        return returnCommand;
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
                readProxySettingsFromNpmrcFile("project .npmrc", projectNpmrc));
        proxyList.addAll(
                readProxySettingsFromNpmrcFile("user .npmrc", userNpmrc));
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

    private File getExecutable(String cmd, String defaultLocation,
            boolean installNode) {
        File file = null;
        try {
            if (defaultLocation == null) {
                file = frontendToolsLocator.tryLocateTool(cmd).orElse(null);
            } else {
                file = Arrays.asList(() -> baseDir, alternativeDirGetter)
                        .stream().map(Supplier::get)
                        .map(dir -> new File(dir, defaultLocation))
                        .filter(frontendToolsLocator::verifyTool).findFirst()
                        .orElseGet(() -> frontendToolsLocator.tryLocateTool(cmd)
                                .orElse(null));
            }
            if (file == null && installNode) {
                getLogger().info(
                        "Couldn't find {}. Installing Node and NPM to {}.", cmd,
                        installNode);
                return new File(
                        installNode(Constants.DEFAULT_NODE_VERSION, null));
            }
        } catch (Exception e) { // NOSONAR
            // There are IOException coming from process fork
        }
        if (file == null) {
            throw new IllegalStateException(String.format(NODE_NOT_FOUND));
        }
        return file;
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
        List<String> returnCommand = getNpmScriptCommand(baseDir);
        if (returnCommand.isEmpty()) {
            returnCommand = getNpmScriptCommand(getAlternativeDir());
        }

        if (returnCommand.isEmpty()) {
            // Otherwise look for regular `npm`
            String command = FrontendUtils.isWindows() ? "npm.cmd" : "npm";
            returnCommand
                    .add(getExecutable(command, null, true).getAbsolutePath());
        }
        returnCommand.add("--no-update-notifier");
        returnCommand.add("--no-audit");

        if (removePnpmLock) {
            // remove pnpm-lock.yaml which contains pnpm as a dependency.
            new File(baseDir, "pnpm-lock.yaml").delete();
            getLogger().debug("pnpm-lock.yaml file is removed from " + baseDir);
        }

        return returnCommand;
    }

    private List<String> getNpmScriptCommand(String dir) {
        // If `node` is not found in PATH, `node/node_modules/npm/bin/npm` will
        // not work because it's a shell or windows script that looks for node
        // and will fail. Thus we look for the `npm-cli` node script instead
        File file = new File(dir, "node/node_modules/npm/bin/npm-cli.js");
        List<String> returnCommand = new ArrayList<>();
        if (file.canRead()) {
            // We return a two element list with node binary and npm-cli script
            returnCommand.add(getNodeExecutable());
            returnCommand.add(file.getAbsolutePath());
        }
        return returnCommand;
    }

    private boolean isPnpmTooOldOrAbsent(String dir) {
        final List<String> pnpmCommand = getPnpmExecutable(dir, false);
        if (!pnpmCommand.isEmpty()) {
            // check whether globally or locally installed pnpm is new enough
            try {
                List<String> versionCmd = new ArrayList<>(pnpmCommand);
                versionCmd.add("--version"); // NOSONAR
                FrontendVersion pnpmVersion = FrontendUtils.getVersion("pnpm",
                        versionCmd);
                if (FrontendUtils.isVersionAtLeast(pnpmVersion,
                        SUPPORTED_PNPM_VERSION)) {
                    return false;
                } else {
                    getLogger().warn(String.format(
                            "installed pnpm ('%s', version %s) is too old, installing supported version locally",
                            String.join(" ", pnpmCommand),
                            pnpmVersion.getFullVersion()));
                }
            } catch (UnknownVersionException e) {
                getLogger().warn(
                        "Error checking pnpm version, installing pnpm locally",
                        e);
            }
        }
        return true;
    }

    private void installPnpm(List<String> installCommand) {
        List<String> command = new ArrayList<>();
        command.addAll(installCommand);
        command.add("install");
        command.add("pnpm@" + Constants.DEFAULT_PNPM_VERSION);

        FrontendUtils.console(FrontendUtils.YELLOW,
                FrontendUtils.commandToString(baseDir, command));

        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.environment().put("ADBLOCK", "1");
        builder.directory(new File(baseDir));

        builder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);

        Process process = null;
        try {
            process = builder.start();
            getLogger().debug("Output of `{}`:",
                    command.stream().collect(Collectors.joining(" ")));
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String stdoutLine;
                while ((stdoutLine = reader.readLine()) != null) {
                    getLogger().debug(stdoutLine);
                }
            }

            int errorCode = process.waitFor();
            if (errorCode != 0) {
                getLogger().error("Couldn't install 'pnpm'");
            } else {
                getLogger().debug("Pnpm is successfully installed");
            }
        } catch (InterruptedException | IOException e) {
            getLogger().error("Error when running `npm install`", e);
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
    }

    private Optional<File> getLocalPnpmScript(String dir) {
        File npmInstalled = new File(dir, PNMP_INSTALLED_BY_NPM);
        if (npmInstalled.canRead()) {
            return Optional.of(npmInstalled);
        }

        // For version 4.3.3 check ".ignored" folders
        File movedPnpmScript = new File(dir,
                "node_modules/.ignored_pnpm/bin/pnpm.js");
        if (movedPnpmScript.canRead()) {
            return Optional.of(movedPnpmScript);
        }

        movedPnpmScript = new File(dir,
                "node_modules/.ignored/pnpm/bin/pnpm.js");
        if (movedPnpmScript.canRead()) {
            return Optional.of(movedPnpmScript);
        }
        return Optional.empty();
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

}
