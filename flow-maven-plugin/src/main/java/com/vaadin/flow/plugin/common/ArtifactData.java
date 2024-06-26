/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.plugin.common;

import java.io.File;
import java.util.Objects;

/**
 * All information required to properly unpack the jar.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public final class ArtifactData {
    private final File fileOrDirectory;
    private final String artifactId;
    private final String version;

    /**
     * Create a new ArtifactData.
     *
     * @param fileOrDirectory
     *            artifact file or directory, not {@code null}
     * @param artifactId
     *            jar file's artifact id, not {@code null}
     * @param version
     *            jar file's artifact version, not {@code null}
     * @throws IllegalArgumentException
     *             if jar file specified is not a file or does not exists
     * @throws NullPointerException
     *             if any of the constructor parameters are {@code null}
     */
    public ArtifactData(File fileOrDirectory, String artifactId,
            String version) {
        this.fileOrDirectory = Objects.requireNonNull(fileOrDirectory);
        this.artifactId = Objects.requireNonNull(artifactId);
        this.version = Objects.requireNonNull(version);
    }

    /**
     * Gets corresponding file or directory.
     *
     * @return the artifact file or directory
     */
    public File getFileOrDirectory() {
        return fileOrDirectory;
    }

    /**
     * Gets corresponding artifact id.
     *
     * @return the artifact version
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Gets corresponding artifact version.
     *
     * @return the artifact version
     */
    public String getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ArtifactData that = (ArtifactData) o;
        return Objects.equals(fileOrDirectory, that.fileOrDirectory)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileOrDirectory, artifactId, version);
    }

    @Override
    public String toString() {
        return "ArtifactData{" + "file=" + fileOrDirectory + ", artifactId='"
                + artifactId + '\'' + ", version='" + version + '\'' + '}';
    }
}
