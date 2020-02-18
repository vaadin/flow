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

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.FrontendVersion;

/**
 * Node installation class.
 * <p>
 * Derived from eirslett/frontend-maven-plugin
 *
 * @since
 */
public class NodeInstaller {

    public static final String INSTALL_PATH = "/node";

    public static final String DEFAULT_NODEJS_DOWNLOAD_ROOT = "https://nodejs.org/dist/";

    private static final String NODE_WINDOWS =
            INSTALL_PATH.replaceAll("/", "\\\\") + "\\node.exe";
    private static final String NODE_DEFAULT = INSTALL_PATH + "/node";

    public static final String PROVIDED_VERSION = "provided";

    private final Object lock = new Object();

    private String npmVersion = PROVIDED_VERSION;
    private String nodeVersion;
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
     *         installation directory
     * @param proxies
     *         list of proxies
     */
    public NodeInstaller(File installDirectory,
            List<ProxyConfig.Proxy> proxies) {
        this(installDirectory, Platform.guess(), proxies);
    }

    /**
     * Create NoodeInstaller with default extractor and downloader.
     *
     * @param installDirectory
     *         installation directory
     * @param platform
     *         platform information
     * @param proxies
     *         list of proxies
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
     *         installation directory
     * @param platform
     *         platform information
     * @param archiveExtractor
     *         archive extractor
     * @param fileDownloader
     *         file downloader
     */
    public NodeInstaller(File installDirectory, Platform platform,
            ArchiveExtractor archiveExtractor, FileDownloader fileDownloader) {
        this.installDirectory = installDirectory;
        this.platform = platform;
        this.archiveExtractor = archiveExtractor;
        this.fileDownloader = fileDownloader;
    }

    /**
     * Set the node version to install. (given as "v12.16.0")
     *
     * @param nodeVersion
     *         version string
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
     * packages.
     * For instance for v12.16.0 we should have under nodeDownloadRoot:
     * ./v12.6.0/node-v12.16.0-linux-x64.tar.xz
     * ./v12.6.0/node-v12.16.0-darwin-x64.tar.gz
     * ./v12.6.0/node-v12.16.0-win-x64.zip
     * ./v12.6.0/node-v12.16.0-win-x86.zip
     *
     * @param nodeDownloadRoot
     *         custom download root
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
     *         user name
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
     *         password
     * @return this
     */
    public NodeInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    private boolean npmProvided() throws InstallationException {
        if (PROVIDED_VERSION.equals(npmVersion)) {
            if (Integer.parseInt(nodeVersion.replace("v", "").split("[.]")[0])
                    < 4) {
                throw new InstallationException("NPM version is '" + npmVersion
                        + "' but Node didn't include NPM prior to v4.0.0");
            }
            return true;
        }
        return false;
    }

    /**
     * Install node and NPM.
     *
     * @throws InstallationException
     *         exception thrown when installation fails
     */
    public void install() throws InstallationException {
        // use lock object for a synchronized block
        synchronized (lock) {
            // If no download root defined use default root
            if (nodeDownloadRoot == null) {
                nodeDownloadRoot = URI.create(DEFAULT_NODEJS_DOWNLOAD_ROOT);
            }

            if (nodeIsAlreadyInstalled()) {
                return;
            }

            getLogger().info("Installing node version {}", nodeVersion);
            if (!nodeVersion.startsWith("v")) {
                getLogger()
                        .warn("Node version does not start with naming convention 'v'. "
                                + "If download fails please add 'v' to the version string.");
            }
            InstallData data = new InstallData(nodeVersion, nodeDownloadRoot,
                    platform);
            installNode(data);

        }
    }

    private boolean nodeIsAlreadyInstalled() throws InstallationException {
        File nodeFile = getNodeExecutable();
        if (nodeFile.exists()) {

            List<String> nodeVersionCommand = new ArrayList<>();
            nodeVersionCommand.add(nodeFile.toString());
            nodeVersionCommand.add("--version");
            String version = getVersion("Node", nodeVersionCommand)
                    .getFullVersion();

            if (version.equals(nodeVersion)) {
                getLogger().info("Node {} is already installed.", version);
                return true;
            } else {
                getLogger()
                        .info("Node {} was installed, but we need version {}",
                                version, nodeVersion);
                return false;
            }
        }
        return false;
    }

    private void installNode(InstallData data) throws InstallationException {
        try {

            downloadFileIfMissing(data.getDownloadUrl(), data.getArchive(),
                    userName, password);

            extractFile(data.getArchive(), data.getTmpDirectory());
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js", e);
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

        File destinationDirectory = getNodeInstallDirectory();

        File destination = new File(destinationDirectory,
                data.getNodeExecutable());

        copyNodeBinaryToDestination(nodeBinary, destination);

        if (!destination.setExecutable(true, false)) {
            throw new InstallationException(
                    "Could not install Node: Was not allowed to make "
                            + destination + " executable.");
        }

        if (npmProvided()) {
            extractUnixNpm(data, destinationDirectory);
        }

        deleteTempDirectory(data.getTmpDirectory());
    }

    private void extractUnixNpm(InstallData data, File destinationDirectory)
            throws IOException {
        getLogger().info("Extracting NPM");
        File tmpNodeModulesDir = new File(data.getTmpDirectory(),
                data.getNodeFilename() + File.separator + "lib" + File.separator
                        + FrontendUtils.NODE_MODULES);
        File nodeModulesDirectory = new File(destinationDirectory,
                FrontendUtils.NODE_MODULES);
        File npmDirectory = new File(nodeModulesDirectory, "npm");
        FileUtils.copyDirectory(tmpNodeModulesDir, nodeModulesDirectory);
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
                data.getNodeFilename() + File.separator + data
                        .getNodeExecutable());
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
            getLogger().info("Extracting NPM");
            File tmpNodeModulesDir = new File(data.getTmpDirectory(),
                    data.getNodeFilename() + File.separator + FrontendUtils.NODE_MODULES);
            File nodeModulesDirectory = new File(destinationDirectory,
                    FrontendUtils.NODE_MODULES);
            FileUtils.copyDirectory(tmpNodeModulesDir, nodeModulesDirectory);
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
        return new File(installDirectory, INSTALL_PATH).getPath();
    }

    private File getNodeInstallDirectory() {
        File nodeInstallDirectory = new File(installDirectory, INSTALL_PATH);
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

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            getLogger().debug("Deleting temporary directory {}", tmpDirectory);
            FileUtils.deleteDirectory(tmpDirectory);
        }
    }

    private void extractFile(File archive, File destinationDirectory)
            throws ArchiveExtractionException {
        try {
            getLogger().info("Unpacking {} into {}", archive,
                    destinationDirectory);
            archiveExtractor.extract(archive, destinationDirectory);
        } catch (ArchiveExtractionException e) {
            if (e.getCause() instanceof EOFException) {
                // https://github.com/eirslett/frontend-maven-plugin/issues/794
                // The downloading was probably interrupted and archive file is incomplete:
                // delete it to retry from scratch
                getLogger()
                        .error("The archive file {} is corrupted and will be deleted. "
                                        + "Please run the application again.",
                                archive.getPath());
                boolean deleted = archive.delete();
                if (!deleted) {
                    getLogger().error("Failed to remove archive file {}. "
                                    + "Please remove it manually and run the application.",
                            archive.getPath());
                }
                try {
                    FileUtils.deleteDirectory(destinationDirectory);
                } catch (IOException ioe) {
                    getLogger().error("Failed to remove target directory '{}'",
                            destinationDirectory, ioe);
                }
            }

            throw e;
        }
    }

    private void downloadFileIfMissing(URI downloadUrl, File destination,
            String userName, String password) throws DownloadException {
        if (!destination.exists()) {
            getLogger().info("Downloading {} to {}", downloadUrl, destination);
            fileDownloader
                    .download(downloadUrl, destination, userName, password);
        }
    }

    /**
     * Get node executable file.
     *
     * @return node executable
     */
    private File getNodeExecutable() {
        String nodeExecutable = platform.isWindows() ?
                NODE_WINDOWS :
                NODE_DEFAULT;
        return new File(installDirectory + nodeExecutable);
    }


    private static FrontendVersion getVersion(String tool,
            List<String> versionCommand) throws InstallationException {
        try {
            Process process = FrontendUtils.createProcessBuilder(versionCommand)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(
                        "Process exited with non 0 exit code. (" + exitCode
                                + ")");
            }
            return FrontendUtils.parseFrontendVersion(
                    FrontendUtils.streamToString(process.getInputStream()));
        } catch (InterruptedException | IOException e) {
            throw new InstallationException(
                    String.format("Unable to detect version of %s. %s", tool,
                            "Using command " + String
                                    .join(" ", versionCommand)), e);
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
                    platform.getNodeClassifier(),
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
            File temporaryDirectory = new File(getNodeInstallDirectory(), "tmp");
            if (!temporaryDirectory.exists()) {
                getLogger()
                        .debug("Creating temporary directory {}", temporaryDirectory);
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
            return "node-" + nodeVersion + "-" + platform.getNodeClassifier();
        }

        /**
         * Build archive file name and return archive file target location.
         *
         * @param name
         *         archive name
         * @param nodeVersion
         *         node version
         * @param classifier
         *         optional classifier
         * @param archiveExtension
         *         archive extension
         * @return archive {@link File} for archive
         */
        private File resolveArchive(String name, String nodeVersion,
                String classifier, String archiveExtension) {
            if (!installDirectory.exists()) {
                installDirectory.mkdirs();
            }

            StringBuilder filename = new StringBuilder().append(name).append("-")
                    .append(nodeVersion);
            if (classifier != null) {
                filename.append("-").append(classifier);
            }
            filename.append(".").append(archiveExtension);
            return new File(installDirectory, filename.toString());
        }
    }
}
