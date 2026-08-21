/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.FrontendUtils.UnknownVersionException;
import com.vaadin.flow.internal.FrontendVersion;

/**
 * A single Node.js version installed by Flow into the alternative install
 * directory, i.e. a {@code node-v24.10.0} directory under {@code ~/.vaadin}.
 * <p>
 * Knows where the executables of the installation are and which version it is.
 * The installation directory does not have to exist.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
final class NodeInstallation {

    /**
     * Prefix shared by all installation directories, and by the archives they
     * were unpacked from.
     */
    static final String DIRECTORY_PREFIX = "node-v";

    private final File directory;

    /**
     * The canonical form of {@link #directory}, resolved once so that equality
     * is not affected by symbolic links or by how the path was spelled.
     */
    private final File canonicalDirectory;

    /**
     * Creates a handle for the installation in the given directory. The
     * directory does not have to exist.
     *
     * @param directory
     *            the installation directory, named {@code node-<version>}
     */
    NodeInstallation(File directory) {
        this.directory = Objects.requireNonNull(directory);
        this.canonicalDirectory = canonicalize(directory);
    }

    private static File canonicalize(File directory) {
        try {
            return directory.getCanonicalFile();
        } catch (IOException e) {
            getLogger().debug("Could not canonicalize {}", directory, e);
            return directory.getAbsoluteFile();
        }
    }

    /**
     * Creates a handle for the given version in the given install directory.
     *
     * @param installDirectory
     *            the directory holding the installations, typically
     *            {@code ~/.vaadin}
     * @param version
     *            the Node.js version, with or without a leading {@code v}
     * @return a handle for the installation, which does not have to exist
     */
    static NodeInstallation forVersion(File installDirectory, String version) {
        return new NodeInstallation(new File(installDirectory,
                DIRECTORY_PREFIX + normalizeVersion(version)));
    }

    /**
     * Gets the directory the installation lives in.
     *
     * @return the installation directory
     */
    File getDirectory() {
        return directory;
    }

    /**
     * Gets the version this installation is named after, e.g. {@code v24.10.0}.
     *
     * @return the version of the installation
     */
    String getVersion() {
        return "v" + directory.getName().substring(DIRECTORY_PREFIX.length());
    }

    /**
     * Gets the Node.js binary of the installation, which does not have to
     * exist.
     *
     * @return the node executable
     */
    File getNodeExecutable() {
        return new File(directory,
                FrontendUtils.isWindows() ? "node.exe" : "bin/node");
    }

    /**
     * Gets the npm CLI script of the installation, which does not have to
     * exist.
     *
     * @return the npm-cli.js script
     */
    File getNpmCliScript() {
        return new File(directory,
                FrontendUtils.isWindows() ? "node_modules/npm/bin/npm-cli.js"
                        : "lib/node_modules/npm/bin/npm-cli.js");
    }

    /**
     * Checks whether this installation has a Node.js binary in place.
     *
     * @return {@code true} if the node executable exists
     */
    boolean hasNodeExecutable() {
        return getNodeExecutable().exists();
    }

    /**
     * Runs the Node.js binary of this installation to find out which version it
     * really is, which is not necessarily the version the directory is named
     * after.
     *
     * @return the version the installed binary reports
     * @throws UnknownVersionException
     *             if the version cannot be determined
     */
    FrontendVersion getInstalledVersion() throws UnknownVersionException {
        return FrontendUtils.getVersion("node",
                List.of(getNodeExecutable().getAbsolutePath(), "--version"));
    }

    /**
     * Two installations are the same when they live in the same directory,
     * which also means they are the same Node.js version, as the directory is
     * named after it.
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof NodeInstallation other
                && canonicalDirectory.equals(other.canonicalDirectory);
    }

    @Override
    public int hashCode() {
        return canonicalDirectory.hashCode();
    }

    @Override
    public String toString() {
        return directory.toString();
    }

    /**
     * Removes the leading {@code v} of a version string, if present.
     *
     * @param version
     *            the version to normalize
     * @return the version without a leading {@code v}
     */
    static String normalizeVersion(String version) {
        return version != null && version.startsWith("v") ? version.substring(1)
                : version;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(NodeInstallation.class);
    }
}
