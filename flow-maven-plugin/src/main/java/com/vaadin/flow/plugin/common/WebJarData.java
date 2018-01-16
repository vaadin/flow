/*
 * Copyright 2000-2017 Vaadin Ltd.
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

package com.vaadin.flow.plugin.common;

import java.io.File;
import java.util.Objects;

/**
 * All information required to properly unpack WebJar.
 *
 * @author Vaadin Ltd.
 */
public final class WebJarData {
    public static final String WEB_JAR_FILES_BASE = "META-INF/resources/webjars/";

    private final File jarFile;
    private final String artifactId;
    private final String version;

    /**
     * Create a new WebJarData.
     *
     * @param jarFile    corresponding jar file, not {@code null}
     * @param artifactId jar file's artifact id, not {@code null}
     * @param version    jar file's artifact version, not {@code null}
     * @throws IllegalArgumentException if jar file specified is not a file or does not exists
     * @throws NullPointerException     if any of the constructor parameters are {@code null}
     */
    public WebJarData(File jarFile, String artifactId, String version) {
        if (!Objects.requireNonNull(jarFile).isFile()) {
            throw new IllegalArgumentException(String.format("File '%s' does not exist or is not a file", jarFile));
        }
        this.jarFile = jarFile;
        this.artifactId = Objects.requireNonNull(artifactId);
        this.version = Objects.requireNonNull(version);
    }

    /**
     * Gets corresponding jar file.
     *
     * @return the jar file
     */
    public File getJarFile() {
        return jarFile;
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
        WebJarData webJar = (WebJarData) o;
        return Objects.equals(jarFile, webJar.jarFile) &&
                Objects.equals(artifactId, webJar.artifactId) &&
                Objects.equals(version, webJar.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jarFile, artifactId, version);
    }

    @Override
    public String toString() {
        return "WebJarData{" +
                "file=" + jarFile +
                ", artifactId='" + artifactId + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
