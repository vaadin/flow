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
            NodeInstaller.INSTALL_PATH.replaceAll("/", "\\\\") + "\\node.exe";
    private static final String NODE_DEFAULT =
            NodeInstaller.INSTALL_PATH + "/node";
    private static final String NPM =
            NodeInstaller.INSTALL_PATH + "/node_modules/npm/bin/npm-cli.js";

    private static final Object LOCK = new Object();

    private String npmVersion = "provided";
    private String nodeVersion, nodeDownloadRoot, userName, password;

    private final Logger logger = LoggerFactory.getLogger(getClass());

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
    public NodeInstaller setNodeDownloadRoot(String nodeDownloadRoot) {
        this.nodeDownloadRoot = nodeDownloadRoot;
        return this;
    }

    public NodeInstaller setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public NodeInstaller setPassword(String password) {
        this.password = password;
        return this;
    }

    private boolean npmProvided() throws InstallationException {
        if ("provided".equals(npmVersion)) {
            if (Integer.parseInt(nodeVersion.replace("v", "").split("[.]")[0])
                    < 4) {
                throw new InstallationException("NPM version is '" + npmVersion
                        + "' but Node didn't include NPM prior to v4.0.0");
            }
            return true;
        }
        return false;
    }

    public void install() throws InstallationException {
        // use static lock object for a synchronized block
        synchronized (LOCK) {
            if (nodeDownloadRoot == null || nodeDownloadRoot.isEmpty()) {
                nodeDownloadRoot = DEFAULT_NODEJS_DOWNLOAD_ROOT;
            }
            if (!nodeIsAlreadyInstalled()) {
                logger.info("Installing node version {}", nodeVersion);
                if (!nodeVersion.startsWith("v")) {
                    logger.warn(
                            "Node version does not start with naming convention 'v'.");
                }
                if (platform.isWindows()) {
                    installNodeWithNpmForWindows();
                } else {
                    installNodeDefault();
                }
            }
        }
    }

    private boolean nodeIsAlreadyInstalled() {
        try {
            File nodeFile = getNodePath();
            if (nodeFile.exists()) {

                List<String> nodeVersionCommand = new ArrayList<>();
                nodeVersionCommand.add(nodeFile.toString());
                nodeVersionCommand.add("--version");
                String version = getVersion("Node", nodeVersionCommand)
                        .getFullVersion();

                if (version.equals(this.nodeVersion)) {
                    this.logger.info("Node {} is already installed.", version);
                    return true;
                } else {
                    this.logger
                            .info("Node {} was installed, but we need version {}",
                                    version, this.nodeVersion);
                    return false;
                }
            } else {
                return false;
            }
        } catch (FrontendUtils.UnknownVersionException e) {
            return false;
        }
    }

    private void installNodeDefault() throws InstallationException {
        try {
            final String longNodeFilename = getLongNodeFilename(
                    this.nodeVersion, false);
            String downloadUrl =
                    this.nodeDownloadRoot + getNodeDownloadFilename(
                            this.nodeVersion, false);
            String classifier = platform.getNodeClassifier();

            File tmpDirectory = getTempDirectory();

            File archive = resolveArchive("node", this.nodeVersion, classifier,
                    platform.getArchiveExtension());

            downloadFileIfMissing(downloadUrl, archive, this.userName,
                    this.password);

            try {
                extractFile(archive, tmpDirectory);
            } catch (ArchiveExtractionException e) {
                if (e.getCause() instanceof EOFException) {
                    // https://github.com/eirslett/frontend-maven-plugin/issues/794
                    // The downloading was probably interrupted and archive file is incomplete:
                    // delete it to retry from scratch
                    this.logger
                            .error("The archive file {} is corrupted and will be deleted. "
                                            + "Please try the build again.",
                                    archive.getPath());
                    archive.delete();
                    FileUtils.deleteDirectory(tmpDirectory);
                }

                throw e;
            }

            // Search for the node binary
            File nodeBinary = new File(tmpDirectory,
                    longNodeFilename + File.separator + "bin" + File.separator
                            + "node");
            if (!nodeBinary.exists()) {
                throw new FileNotFoundException(
                        "Could not find the downloaded Node.js binary in "
                                + nodeBinary);
            } else {
                File destinationDirectory = getNodeInstallDirectory();

                File destination = new File(destinationDirectory, "node");
                this.logger
                        .info("Copying node binary from {} to {}", nodeBinary,
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
                    throw new InstallationException(
                            "Could not install Node: Was not allowed to rename "
                                    + nodeBinary + " to " + destination);
                }

                if (!destination.setExecutable(true, false)) {
                    throw new InstallationException(
                            "Could not install Node: Was not allowed to make "
                                    + destination + " executable.");
                }

                if (npmProvided()) {
                    File tmpNodeModulesDir = new File(tmpDirectory,
                            longNodeFilename + File.separator + "lib"
                                    + File.separator + "node_modules");
                    File nodeModulesDirectory = new File(destinationDirectory,
                            "node_modules");
                    File npmDirectory = new File(nodeModulesDirectory, "npm");
                    FileUtils.copyDirectory(tmpNodeModulesDir,
                            nodeModulesDirectory);
                    this.logger.info("Extracting NPM");
                    // create a copy of the npm scripts next to the node executable
                    for (String script : Arrays.asList("npm", "npm.cmd")) {
                        File scriptFile = new File(npmDirectory,
                                "bin" + File.separator + script);
                        if (scriptFile.exists()) {
                            scriptFile.setExecutable(true);
                        }
                    }
                }

                deleteTempDirectory(tmpDirectory);

                this.logger.info("Installed node locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Node", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException(
                    "Could not extract the Node archive", e);
        }
    }

    private void installNodeWithNpmForWindows() throws InstallationException {
        try {
            final String longNodeFilename = getLongNodeFilename(
                    this.nodeVersion, true);
            String downloadUrl =
                    this.nodeDownloadRoot + getNodeDownloadFilename(
                            this.nodeVersion, true);
            String classifier = platform.getNodeClassifier();

            File tmpDirectory = getTempDirectory();

            File archive = resolveArchive("node", this.nodeVersion, classifier,
                    platform.getArchiveExtension());

            downloadFileIfMissing(downloadUrl, archive, this.userName,
                    this.password);

            extractFile(archive, tmpDirectory);

            // Search for the node binary
            File nodeBinary = new File(tmpDirectory,
                    longNodeFilename + File.separator + "node.exe");
            if (!nodeBinary.exists()) {
                throw new FileNotFoundException(
                        "Could not find the downloaded Node.js binary in "
                                + nodeBinary);
            } else {
                File destinationDirectory = getNodeInstallDirectory();

                File destination = new File(destinationDirectory, "node.exe");
                this.logger
                        .info("Copying node binary from {} to {}", nodeBinary,
                                destination);
                try {
                    Files.move(nodeBinary.toPath(), destination.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new InstallationException(
                            "Could not install Node: Was not allowed to rename "
                                    + nodeBinary + " to " + destination);
                }

                if ("provided".equals(this.npmVersion)) {
                    File tmpNodeModulesDir = new File(tmpDirectory,
                            longNodeFilename + File.separator + "node_modules");
                    File nodeModulesDirectory = new File(destinationDirectory,
                            "node_modules");
                    FileUtils.copyDirectory(tmpNodeModulesDir,
                            nodeModulesDirectory);
                }
                deleteTempDirectory(tmpDirectory);

                this.logger.info("Installed node locally.");
            }
        } catch (IOException e) {
            throw new InstallationException("Could not install Node", e);
        } catch (DownloadException e) {
            throw new InstallationException("Could not download Node.js", e);
        } catch (ArchiveExtractionException e) {
            throw new InstallationException(
                    "Could not extract the Node archive", e);
        }

    }

    private File getTempDirectory() {
        File tmpDirectory = new File(getNodeInstallDirectory(), "tmp");
        if (!tmpDirectory.exists()) {
            logger.debug("Creating temporary directory {}", tmpDirectory);
            tmpDirectory.mkdirs();
        }
        return tmpDirectory;
    }

    public String getInstallDirectory() {
        return new File(installDirectory, INSTALL_PATH).getPath();
    }

    private File getNodeInstallDirectory() {
        File nodeInstallDirectory = new File(installDirectory, INSTALL_PATH);
        if (!nodeInstallDirectory.exists()) {
            logger.debug("Creating install directory {}", nodeInstallDirectory);
            nodeInstallDirectory.mkdirs();
        }
        return nodeInstallDirectory;
    }

    private void deleteTempDirectory(File tmpDirectory) throws IOException {
        if (tmpDirectory != null && tmpDirectory.exists()) {
            logger.debug("Deleting temporary directory {}", tmpDirectory);
            FileUtils.deleteDirectory(tmpDirectory);
        }
    }

    private void extractFile(File archive, File destinationDirectory)
            throws ArchiveExtractionException {
        this.logger.info("Unpacking {} into {}", archive, destinationDirectory);
        this.archiveExtractor
                .extract(archive.getPath(), destinationDirectory.getPath());
    }

    private void downloadFileIfMissing(String downloadUrl, File destination,
            String userName, String password) throws DownloadException {
        if (!destination.exists()) {
            this.logger.info("Downloading {} to {}", downloadUrl, destination);
            this.fileDownloader
                    .download(downloadUrl, destination, userName, password);
        }
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
     * @return archive {@link File}
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

    public File getNodePath() {
        String nodeExecutable = platform.isWindows() ?
                NODE_WINDOWS :
                NODE_DEFAULT;
        return new File(installDirectory + nodeExecutable);
    }

    private String getLongNodeFilename(String nodeVersion,
            boolean archiveOnWindows) {
        if (platform.isWindows() && !archiveOnWindows) {
            return "node.exe";
        } else {
            return "node-" + nodeVersion + "-" + platform.getNodeClassifier();
        }
    }

    private String getNodeDownloadFilename(String nodeVersion,
            boolean archiveOnWindows) {
        if (platform.isWindows() && !archiveOnWindows) {
            if (platform.getArchitecture() == Platform.Architecture.x64) {
                if (nodeVersion.startsWith("v0.")) {
                    return nodeVersion + "/x64/node.exe";
                } else {
                    return nodeVersion + "/win-x64/node.exe";
                }
            } else {
                if (nodeVersion.startsWith("v0.")) {
                    return nodeVersion + "/node.exe";
                } else {
                    return nodeVersion + "/win-x86/node.exe";
                }
            }
        } else {
            return nodeVersion + "/" + getLongNodeFilename(nodeVersion,
                    archiveOnWindows) + "." + platform.getOs()
                    .getArchiveExtension();
        }
    }

    private static FrontendVersion getVersion(String tool,
            List<String> versionCommand)
            throws FrontendUtils.UnknownVersionException {
        try {
            Process process = FrontendUtils.createProcessBuilder(versionCommand)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new FrontendUtils.UnknownVersionException(tool,
                        "Using command " + String.join(" ", versionCommand));
            }
            return FrontendUtils.parseFrontendVersion(
                    FrontendUtils.streamToString(process.getInputStream()));
        } catch (InterruptedException | IOException e) {
            throw new FrontendUtils.UnknownVersionException(tool,
                    "Using command " + String.join(" ", versionCommand), e);
        }
    }
}
