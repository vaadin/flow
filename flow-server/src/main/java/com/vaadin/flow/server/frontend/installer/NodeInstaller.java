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
package com.vaadin.flow.server.frontend.installer;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.server.frontend.FileIOUtils;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.FrontendVersion;
import com.vaadin.frontendtools.installer.ArchiveExtractionException;
import com.vaadin.frontendtools.installer.ArchiveExtractor;
import com.vaadin.frontendtools.installer.DefaultArchiveExtractor;
import com.vaadin.frontendtools.installer.VerificationException;

/**
 * Node installation class.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 */
public class NodeInstaller {

    public static final String INSTALL_PATH_PREFIX = "/node";

    public static final String SHA_SUMS_FILE = "SHASUMS256.txt";

    public static final String PROVIDED_VERSION = "provided";

    private static final int MAX_DOWNLOAD_ATTEMPS = 5;

    private static final int DOWNLOAD_ATTEMPT_DELAY = 5;
    public static final String ACCEPT_MISSING_SHA = "vaadin.node.download.acceptMissingSHA";
    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";
    public static final String UNOFFICIAL_NODEJS_DOWNLOAD_ROOT = "https://unofficial-builds.nodejs.org/download/release/";

    private final Object lock = new Object();

    private String npmVersion = PROVIDED_VERSION;
    private String nodeVersion;
    /**
     * The actual node version being used. May differ from nodeVersion if a
     * compatible fallback version was found.
     */
    private String activeNodeVersion;
    private URI nodeDownloadRoot;
    private String userName;
    private String password;

    private final File installDirectory;
    private final Platform platform;

    private final ArchiveExtractor archiveExtractor;

    private final FileDownloader fileDownloader;

    /**
     * Create NodeInstaller with default extractor and downloader and guess
     * platform.
     *
     * @param installDirectory
     *            installation directory
     * @param proxies
     *            list of proxies
     */
    public NodeInstaller(File installDirectory,
            List<ProxyConfig.Proxy> proxies) {
        this(installDirectory, Platform.guess(), proxies);
    }

    /**
     * Create NoodeInstaller with default extractor and downloader.
     *
     * @param installDirectory
     *            installation directory
     * @param platform
     *            platform information
     * @param proxies
     *            list of proxies
     */
    public NodeInstaller(File installDirectory, Platform platform,
            List<ProxyConfig.Proxy> proxies) {
        this(installDirectory, platform, new DefaultArchiveExtractor(),
                new DefaultFileDownloader(new ProxyConfig(proxies)));
    }

    /**
     * Initialize a new NodeInstaller.
     *
     * @param installDirectory
     *            installation directory
     * @param platform
     *            platform information
     * @param archiveExtractor
     *            archive extractor
     * @param fileDownloader
     *            file downloader
     */
    public NodeInstaller(File installDirectory, Platform platform,
            ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.installDirectory = installDirectory;
        this.platform = platform;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    /**
     * Set the node version to install. (given as "v16.0.0")
     *
     * @param nodeVersion
     *            version string
     * @return this
     */
    public NodeInstaller setNodeVersion(String nodeVersion) {
        this.nodeVersion = nodeVersion;
        return this;
    }

    /**
     * Set a custom download root.
     * <p>
     * This should be a url or directory under which we can find a directory
     * {@link #nodeVersion} and there should then exist the archived node
     * packages. For instance for v16.0.0 we should have under nodeDownloadRoot:
     * ./v16.0.0/node-v16.0.0-linux-x64.tar.xz
     * ./v16.0.0/node-v16.0.0-darwin-x64.tar.gz
     * ./v16.0.0/node-v16.0.0-win-x64.zip ./v16.0.0/node-v16.0.0-win-x86.zip
     *
     * @param nodeDownloadRoot
     *            custom download root
     * @return this
     */
    public NodeInstaller setNodeDownloadRoot(URI nodeDownloadRoot) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        return this;
    }

    /**
     * Set user name to use.
     *
     * @param userName
     *            user name
     * @return this
     */
    public NodeInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * Set password to use.
     *
     * @param password
     *            password
     * @return this
     */
    public NodeInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    private boolean npmProvided() throws InstallationException {
        if (PROVIDED_VERSION.equals(npmVersion)) {
            if (Integer.parseInt(
                    nodeVersion.replace("v", "").split("[.]")[0]) < 4) {
                throw new InstallationException("npm version is '" + npmVersion
                        + "' but Node didn't include npm prior to v4.0.0");
            }
            return true;
        }
        return false;
    }

    /**
     * Install node and npm.
     *
     * @throws InstallationException
     *             exception thrown when installation fails
     */
    public void install() throws InstallationException {
        // use lock object for a synchronized block
        synchronized (lock) {
            // If no download root defined use default root
            if (nodeDownloadRoot == null) {
                nodeDownloadRoot = URI.create(platform.getNodeDownloadRoot());
            }

            if (nodeIsAlreadyInstalled()) {
                return;
            }

            getLogger().info("Installing node version {}", nodeVersion);
            if (!nodeVersion.startsWith("v")) {
                getLogger().warn(
                        "Node version does not start with naming convention 'v'. "
                                + "If download fails please add 'v' to the version string.");
            }
            InstallData data = new InstallData(nodeVersion, nodeDownloadRoot,
                    platform);
            installNode(data);

        }
    }

    private boolean nodeIsAlreadyInstalled() throws InstallationException {
        // First, check if the exact requested version is installed
        File nodeFile = getNodeExecutableForVersion(nodeVersion);
        if (nodeFile.exists()) {
            List<String> nodeVersionCommand = new ArrayList<>();
            nodeVersionCommand.add(nodeFile.toString());
            nodeVersionCommand.add("--version");
            String version = getVersion("Node", nodeVersionCommand)
                    .getFullVersion();

            if (version.equals(nodeVersion)) {
                getLogger().info("Node {} is already installed.", version);
                activeNodeVersion = nodeVersion;
                return true;
            } else {
                getLogger().info(
                        "Node {} was installed, but we need version {}",
                        version, nodeVersion);
            }
        }

        // Check if any other compatible version is available
        String fallbackVersion = findCompatibleInstalledVersion();
        if (fallbackVersion != null) {
            getLogger().debug("Using existing Node {} instead of installing {}",
                    fallbackVersion, nodeVersion);
            activeNodeVersion = fallbackVersion;
            return true;
        }

        return false;
    }

    /**
     * Scans the install directory for installed Node.js versions and returns
     * the newest one that is supported.
     *
     * @return the version string (e.g., "v24.10.0") of the best available
     *         version, or null if none found
     */
    private String findCompatibleInstalledVersion() {
        if (!installDirectory.exists() || !installDirectory.isDirectory()) {
            return null;
        }

        File[] nodeDirs = installDirectory.listFiles(file -> file.isDirectory()
                && file.getName().startsWith("node-v"));

        if (nodeDirs == null || nodeDirs.length == 0) {
            return null;
        }

        FrontendVersion bestVersion = null;
        String bestVersionString = null;

        for (File nodeDir : nodeDirs) {
            String dirName = nodeDir.getName();
            // Extract version from directory name (node-v24.10.0 -> v24.10.0)
            String versionString = dirName.substring("node-".length());

            try {
                FrontendVersion version = new FrontendVersion(versionString);

                // Skip versions older than minimum supported
                if (version.isOlderThan(FrontendTools.SUPPORTED_NODE_VERSION)) {
                    getLogger().debug(
                            "Skipping {} - older than minimum supported {}",
                            versionString, FrontendTools.SUPPORTED_NODE_VERSION
                                    .getFullVersion());
                    continue;
                }

                // Verify the node executable actually exists and works
                File nodeExecutable = getNodeExecutableForVersion(
                        versionString);
                if (!nodeExecutable.exists()) {
                    getLogger().debug(
                            "Skipping {} - executable not found at {}",
                            versionString, nodeExecutable);
                    continue;
                }

                // Keep the newest version
                if (bestVersion == null || version.isNewerThan(bestVersion)) {
                    bestVersion = version;
                    bestVersionString = versionString;
                }
            } catch (NumberFormatException e) {
                getLogger().debug("Could not parse version from directory: {}",
                        dirName);
            }
        }

        return bestVersionString;
    }

    /**
     * Gets the node executable path for a specific version.
     */
    private File getNodeExecutableForVersion(String version) {
        String versionedPath = INSTALL_PATH_PREFIX + "-" + version;
        String nodeExecutable = platform.isWindows()
                ? versionedPath.replaceAll("/", "\\\\") + "\\node.exe"
                : versionedPath + "/bin/node";
        return new File(installDirectory + nodeExecutable);
    }

    private void installNode(InstallData data) throws InstallationException {
        try {

            downloadFileIfMissing(data.getDownloadUrl(), data.getArchive(),
                    userName, password);

            extractFile(data.getArchive(), data.getTmpDirectory());
        } catch (DownloadException e) {
            throw new InstallationException(
                    "Node.js download failed. This may be due to loss of internet connection.\n"
                            + "If you are behind a proxy server you should configure your proxy settings.\n"
                            + "Verify connection and proxy settings or follow the https://nodejs.org/en/download/ guide to install Node.js globally.",
                    e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException(
                    "Could not extract the Node archive", e);
        }

        try {
            if (platform.isWindows()) {
                installNodeWindows(data);
            } else {
                installNodeUnix(data);
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Node", e);
        }

        getLogger().info("Local node installation successful.");
        activeNodeVersion = nodeVersion;
    }

    private void installNodeUnix(InstallData data)
            throws InstallationException, IOException {

        // Search for the node binary
        File nodeBinary = new File(data.getTmpDirectory(),
                data.getNodeFilename() + File.separator + "bin" + File.separator
                        + data.getNodeExecutable());

        if (!nodeBinary.exists()) {
            throw new FileNotFoundException(
                    "Could not find the downloaded Node.js binary in "
                            + nodeBinary);
        }

        File destinationDirectory = getNodeBinaryInstallDirectory();

        File destination = new File(destinationDirectory,
                data.getNodeExecutable());

        copyNodeBinaryToDestination(nodeBinary, destination);

        if (!destination.setExecutable(true, false)) {
            throw new InstallationException(
                    "Could not install Node: Was not allowed to make "
                            + destination + " executable.");
        }

        createSymbolicLinkToBinary(destination, data.getNodeExecutable());

        if (npmProvided()) {
            extractUnixNpm(data, getNodeInstallDirectory());
        }

        deleteTempDirectory(data.getTmpDirectory());
    }

    private void extractUnixNpm(InstallData data, File destinationDirectory)
            throws IOException {
        getLogger().info("Extracting npm");
        File tmpNodeModulesDir = new File(data.getTmpDirectory(),
                data.getNodeFilename() + File.separator + "lib" + File.separator
                        + FrontendUtils.NODE_MODULES);
        File nodeModulesDirectory = new File(
                destinationDirectory + File.separator + "lib",
                FrontendUtils.NODE_MODULES);
        File npmDirectory = new File(nodeModulesDirectory, "npm");

        // delete old node_modules directory to not end up with corrupted
        // combination of two npm versions in node_modules/npm during upgrade
        if (nodeModulesDirectory.exists()) {
            FileIOUtils.delete(nodeModulesDirectory);
        }
        // delete old/windows type node_modules so it is not messing
        // up the installation
        final File oldNodeModulesDirectory = new File(destinationDirectory
                + File.separator + FrontendUtils.NODE_MODULES);
        if (oldNodeModulesDirectory.exists()) {
            FileIOUtils.delete(oldNodeModulesDirectory);
        }

        FileIOUtils.copyDirectory(tmpNodeModulesDir, nodeModulesDirectory);
        // create a copy of the npm scripts next to the node executable
        for (String script : Arrays.asList("npm", "npm.cmd")) {
            File scriptFile = new File(npmDirectory,
                    "bin" + File.separator + script);
            if (scriptFile.exists()) {
                boolean success = scriptFile.setExecutable(true);
                if (!success) {
                    getLogger().debug("Failed to make '{}' executable.",
                            scriptFile.toPath());
                }
            }
        }
    }

    private void installNodeWindows(InstallData data)
            throws InstallationException, IOException {
        // Search for the node binary
        File nodeBinary = new File(data.getTmpDirectory(),
                data.getNodeFilename() + File.separator
                        + data.getNodeExecutable());
        if (!nodeBinary.exists()) {
            throw new FileNotFoundException(
                    "Could not find the downloaded Node.js binary in "
                            + nodeBinary);
        }

        File destinationDirectory = getNodeInstallDirectory();

        File destination = new File(destinationDirectory,
                data.getNodeExecutable());

        copyNodeBinaryToDestination(nodeBinary, destination);

        if (npmProvided()) {
            getLogger().info("Extracting npm");
            File tmpNodeModulesDir = new File(data.getTmpDirectory(),
                    data.getNodeFilename() + File.separator
                            + FrontendUtils.NODE_MODULES);
            File nodeModulesDirectory = new File(destinationDirectory,
                    FrontendUtils.NODE_MODULES);
            // delete old node_modules directory to not end up with corrupted
            // combination of two npm versions in node_modules/npm during
            // upgrade
            if (nodeModulesDirectory.exists()) {
                FileIOUtils.delete(nodeModulesDirectory);
            }
            FileIOUtils.copyDirectory(tmpNodeModulesDir, nodeModulesDirectory);
        }
        deleteTempDirectory(data.getTmpDirectory());
    }

    private void copyNodeBinaryToDestination(File nodeBinary, File destination)
            throws InstallationException {
        getLogger().info("Copying node binary from {} to {}", nodeBinary,
                destination);
        if (destination.exists() && !destination.delete()) {
            throw new InstallationException(
                    "Could not install Node: Was not allowed to delete "
                            + destination);
        }
        try {
            Files.move(nodeBinary.toPath(), destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            getLogger().debug("Renaming failed.", e);
            throw new InstallationException(
                    "Could not install Node: Was not allowed to rename "
                            + nodeBinary + " to " + destination);
        }
    }

    public String getInstallDirectory() {
        return getInstallDirectoryFile().getPath();
    }

    /**
     * Returns the path to the node executable after installation or resolution.
     *
     * @return the absolute path to the node executable, or null if not
     *         installed
     */
    public String getNodeExecutablePath() {
        if (activeNodeVersion == null) {
            return null;
        }
        File executable = getNodeExecutableForVersion(activeNodeVersion);
        return executable.exists() ? executable.getAbsolutePath() : null;
    }

    /**
     * Returns the active node version being used. This may differ from the
     * requested version if a compatible fallback version was found.
     *
     * @return the active node version, or null if not resolved yet
     */
    public String getActiveNodeVersion() {
        return activeNodeVersion;
    }

    /**
     * Returns the path to the npm-cli.js script after installation or
     * resolution.
     *
     * @return the absolute path to npm-cli.js, or null if not installed
     */
    public String getNpmCliScriptPath() {
        if (activeNodeVersion == null) {
            return null;
        }
        File npmCliScript = new File(getInstallDirectoryFile(),
                FrontendUtils.NODE_MODULES + "/npm/bin/npm-cli.js");
        return npmCliScript.exists() ? npmCliScript.getAbsolutePath() : null;
    }

    private File getInstallDirectoryFile() {
        return new File(installDirectory, getVersionedInstallPath());
    }

    private String getVersionedInstallPath() {
        // Use activeNodeVersion if set (fallback version), otherwise use
        // requested nodeVersion
        String version = activeNodeVersion != null ? activeNodeVersion
                : nodeVersion;
        if (version == null || PROVIDED_VERSION.equals(version)) {
            return INSTALL_PATH_PREFIX;
        }
        return INSTALL_PATH_PREFIX + "-" + version;
    }

    private File getNodeInstallDirectory() {
        File nodeInstallDirectory = getInstallDirectoryFile();
        if (!nodeInstallDirectory.exists()) {
            getLogger().debug("Creating install directory {}",
                    nodeInstallDirectory);
            boolean success = nodeInstallDirectory.mkdirs();
            if (!success) {
                getLogger().debug("Failed to create install directory");
            }
        }
        return nodeInstallDirectory;
    }

    private File getNodeBinaryInstallDirectory() {
        File nodeInstallDirectory = new File(getInstallDirectoryFile(), "bin");
        if (!nodeInstallDirectory.exists()) {
            getLogger().debug("Creating install directory {}",
                    nodeInstallDirectory);
            boolean success = nodeInstallDirectory.mkdirs();
            if (!success) {
                getLogger().debug("Failed to create install directory");
            }
        }
        return nodeInstallDirectory;
    }

    private void createSymbolicLinkToBinary(File destination,
            String nodeExecutable) throws InstallationException, IOException {
        final File symLink = new File(getInstallDirectory(), nodeExecutable);
        if (symLink.exists()) {
            FileIOUtils.delete(symLink);
        }
        try {
            Files.createSymbolicLink(symLink.toPath(), destination.toPath());
        } catch (IOException e) {
            throw new InstallationException(String.format(
                    "Could not install Node: Was not allowed to create symbolic link %s for %s",
                    symLink, destination), e);
        }
    }

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            getLogger().debug("Deleting temporary directory {}", tmpDirectory);
            FileIOUtils.delete(tmpDirectory);
        }
    }

    private void extractFile(File archive, File destinationDirectory)
            throws ArchiveExtractionException {
        long size;
        try {
            size = Files.size(archive.toPath());
        } catch (IOException e) {
            throw new ArchiveExtractionException(
                    "Error determining archive size", e);
        }
        try {
            getLogger().info("Unpacking {} ({} bytes) into {}", archive, size,
                    destinationDirectory);
            archiveExtractor.extract(archive, destinationDirectory);
        } catch (ArchiveExtractionException e) {
            if (e.getCause() instanceof EOFException) {
                // https://github.com/eirslett/frontend-maven-plugin/issues/794
                // The downloading was probably interrupted and archive file is
                // incomplete:
                // delete it to retry from scratch
                getLogger().error(
                        "The archive file {} is corrupted and will be deleted. "
                                + "Please run the application again.",
                        archive.getPath());
                removeArchiveFile(archive);
                try {
                    FileIOUtils.delete(destinationDirectory);
                } catch (IOException ioe) {
                    getLogger().error("Failed to remove target directory '{}'",
                            destinationDirectory, ioe);
                }
            } else {
                removeArchiveFile(archive);
            }

            throw e;
        }
    }

    private static void removeArchiveFile(File archive) {
        if (!archive.delete()) {
            getLogger().error("Failed to remove archive file {}. "
                    + "Please remove it manually and run the application.",
                    archive.getPath());
        }
    }

    private void downloadFileIfMissing(URI downloadUrl, File destination,
            String userName, String password) throws DownloadException {
        if (!destination.exists()) {
            getLogger().info("Downloading {} to {}", downloadUrl, destination);
            for (int i = 0; i < MAX_DOWNLOAD_ATTEMPS; i++) {
                try {
                    fileDownloader.download(downloadUrl, destination, userName,
                            password, null);

                    verifyArchive(destination);
                    return;
                } catch (DownloadException e) {
                    if (i == MAX_DOWNLOAD_ATTEMPS - 1) {
                        removeArchiveFile(destination);
                        throw e;
                    }

                    getLogger().debug("Error during downloading " + downloadUrl,
                            e);
                    getLogger().warn("Download failed, retrying in "
                            + DOWNLOAD_ATTEMPT_DELAY + "s...");
                    try {
                        Thread.sleep(DOWNLOAD_ATTEMPT_DELAY * 1000);
                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    }
                } catch (VerificationException ve) {
                    getLogger().warn(
                            "SHA256 verification of downloaded node archive failed.");
                    if (i == MAX_DOWNLOAD_ATTEMPS - 1) {
                        removeArchiveFile(destination);
                        throw new DownloadException(
                                "Failed to download node matching SHA256.");
                    }
                }
            }
        } else {
            try {
                verifyArchive(destination);
            } catch (VerificationException de) {
                removeArchiveFile(destination);
                downloadFileIfMissing(downloadUrl, destination, userName,
                        password);
            }
        }
    }

    private void verifyArchive(File archive)
            throws DownloadException, VerificationException {
        try {
            URI shaSumsURL = nodeDownloadRoot
                    .resolve(nodeVersion + "/" + SHA_SUMS_FILE);
            if ("file".equalsIgnoreCase(shaSumsURL.getScheme())) {
                // The file is local so it can't be expected to have a SHA file
                return;
            }

            File shaSums = new File(installDirectory, "node-" + SHA_SUMS_FILE);

            getLogger().debug("Downloading {} to {}", shaSumsURL, shaSums);

            try {
                fileDownloader.download(shaSumsURL, shaSums, userName, password,
                        null);
            } catch (DownloadException e) {
                if (Boolean.getBoolean(ACCEPT_MISSING_SHA)) {
                    getLogger().warn(
                            "Could not verify SHA256 sum of downloaded node in {}. Accepting missing checksum verification as set in '{}' system property.",
                            archive, ACCEPT_MISSING_SHA);
                    return;
                } else {
                    getLogger().info(
                            "Download of {} failed. If failure persists, use system property '{}' to skip verification or download node manually.",
                            SHA_SUMS_FILE, ACCEPT_MISSING_SHA);
                    throw e;
                }
            }

            String archiveSHA256 = MessageDigestUtil
                    .sha256Hex(Files.readAllBytes(archive.toPath()));

            List<String> sha256sums = Files.readAllLines(shaSums.toPath());
            String archiveTargetSHA256 = sha256sums.stream()
                    .filter(sum -> sum
                            .endsWith(archive.getName()))
                    .map(sum -> sum
                            .substring(0,
                                    sum.length() - archive.getName().length())
                            .trim())
                    .findFirst().orElse("-1");

            shaSums.delete();

            if (!archiveSHA256.equals(archiveTargetSHA256)) {
                getLogger().error(
                        "Expected SHA256 [{}] for downloaded node archive, got [{}]",
                        archiveTargetSHA256, archiveSHA256);
                throw new VerificationException(
                        "SHA256 sums did not match for downloaded node");
            }
        } catch (IOException e) {
            throw new VerificationException("Failed to validate archive hash.",
                    e);
        }
    }

    private static FrontendVersion getVersion(String tool,
            List<String> versionCommand) throws InstallationException {
        try {
            Process process = FrontendUtils.createProcessBuilder(versionCommand)
                    .start();
            CompletableFuture<Pair<String, String>> streamConsumer = FrontendUtils
                    .consumeProcessStreams(process);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Process exited with non 0 exit code. ("
                        + exitCode + ")");
            }
            String version;
            try {
                version = streamConsumer.get(1, TimeUnit.SECONDS).getFirst();
            } catch (ExecutionException | TimeoutException e) {
                getLogger().debug("Cannot read {} version", tool, e);
                version = "";
            }
            return FrontendUtils.parseFrontendVersion(version);
        } catch (InterruptedException | IOException e) {
            throw new InstallationException(String.format(
                    "Unable to detect version of %s. %s", tool,
                    "Using command " + String.join(" ", versionCommand)), e);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger("NodeInstaller");
    }

    private class InstallData {
        String nodeFilename;
        URI downloadUrl;
        File tmpDirectory;
        File archive;
        String nodeExecutable;

        InstallData(String nodeVersion, URI nodeDownloadRoot,
                Platform platform) {
            nodeFilename = getLongNodeFilename(nodeVersion);
            downloadUrl = nodeDownloadRoot
                    .resolve(getNodeDownloadFilename(nodeVersion));
            tmpDirectory = getTempDirectory();
            archive = resolveArchive("node", nodeVersion,
                    platform.getNodeClassifier(
                            new FrontendVersion(nodeVersion)),
                    platform.getArchiveExtension());
            nodeExecutable = platform.isWindows() ? "node.exe" : "node";
        }

        public String getNodeFilename() {
            return nodeFilename;
        }

        public URI getDownloadUrl() {
            return downloadUrl;
        }

        public File getTmpDirectory() {
            return tmpDirectory;
        }

        public File getArchive() {
            return archive;
        }

        public String getNodeExecutable() {
            return nodeExecutable;
        }

        private File getTempDirectory() {
            File temporaryDirectory = new File(getNodeInstallDirectory(),
                    "tmp");
            if (!temporaryDirectory.exists()) {
                getLogger().debug("Creating temporary directory {}",
                        temporaryDirectory);
                boolean success = temporaryDirectory.mkdirs();
                if (!success) {
                    getLogger().debug("Failed to create temporary directory");
                }
            }
            return temporaryDirectory;
        }

        private String getNodeDownloadFilename(String nodeVersion) {
            return nodeVersion + "/" + getLongNodeFilename(nodeVersion) + "."
                    + platform.getOs().getArchiveExtension();
        }

        private String getLongNodeFilename(String nodeVersion) {
            return "node-" + nodeVersion + "-" + platform
                    .getNodeClassifier(new FrontendVersion(nodeVersion));
        }

        /**
         * Build archive file name and return archive file target location.
         *
         * @param name
         *            archive name
         * @param nodeVersion
         *            node version
         * @param classifier
         *            optional classifier
         * @param archiveExtension
         *            archive extension
         * @return archive {@link File} for archive
         */
        private File resolveArchive(String name, String nodeVersion,
                String classifier, String archiveExtension) {
            if (!installDirectory.exists()) {
                installDirectory.mkdirs();
            }

            StringBuilder filename = new StringBuilder().append(name)
                    .append("-").append(nodeVersion);
            if (classifier != null) {
                filename.append("-").append(classifier);
            }
            filename.append(".").append(archiveExtension);
            return new File(installDirectory, filename.toString());
        }
    }
}
