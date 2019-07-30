/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.migration;

import java.io.File;
import java.util.Objects;

import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Configuration for migration.
 *
 * @author Vaadin Ltd
 *
 */
public class Configuration implements Cloneable {
    private File tempMigrationFolder;

    private File[] resourceDirectories;

    private File targetDirectory;

    private boolean keepOriginalFiles;

    private boolean ignoreModulizerErrors = true;

    private final File baseDirectory;

    private ClassFinder classFinder;

    private File[] javaSourceDirectories;

    private File compiledClassDirectories;

    private Configuration(File baseDir) {
        baseDirectory = baseDir;
    }

    /**
     * @return the tempMigrationFolder
     */
    public File getTempMigrationFolder() {
        return tempMigrationFolder;
    }

    /**
     * @return the resourceDirectories
     */
    public File[] getResourceDirectories() {
        return resourceDirectories;
    }

    /**
     * @return the targetDirectory
     */
    public File getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * @return the keepOriginalFiles
     */
    public boolean isKeepOriginalFiles() {
        return keepOriginalFiles;
    }

    /**
     * @return the ignoreModulizerErrors
     */
    public boolean isIgnoreModulizerErrors() {
        return ignoreModulizerErrors;
    }

    /**
     * @return the baseDirectory
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * @return the compiledClassDirectories
     */
    public File getCompiledClassDirectories() {
        return compiledClassDirectories;
    }

    /**
     * @return the classFinder
     */
    public ClassFinder getClassFinder() {
        return classFinder;
    }

    public File[] getJavaSourceDirectories() {
        return javaSourceDirectories;
    }

    private Configuration copy() {
        try {
            return (Configuration) clone();
        } catch (CloneNotSupportedException exception) {
            // this may not happen
            throw new RuntimeException(exception);
        }
    }

    /**
     * A builder for {@link Configuration}. Allows to set all required
     * parameters via setters fluent API.
     *
     * @author Vaadin Ltd
     *
     */
    public static class Builder {

        private final Configuration config;

        public Builder(File baseDir) {
            config = new Configuration(baseDir);
        }

        public Builder setTemporaryMigrationFolder(File file) {
            config.tempMigrationFolder = Objects.requireNonNull(file);
            return this;
        }

        public Builder setResourceDirectories(File[] resourceDirs) {
            config.resourceDirectories = Objects.requireNonNull(resourceDirs);
            return this;
        }

        public Builder setTargetDirectory(File targetDir) {
            config.targetDirectory = Objects.requireNonNull(targetDir);
            return this;
        }

        public Builder setKeepOriginalFiles(boolean keepOriginal) {
            config.keepOriginalFiles = keepOriginal;
            return this;
        }

        public Builder setIgnoreModulizerErrors(boolean ignoreModulizerErrors) {
            config.ignoreModulizerErrors = ignoreModulizerErrors;
            return this;
        }

        public Builder setClassFinder(ClassFinder finder) {
            config.classFinder = Objects.requireNonNull(finder);
            return this;
        }

        public Builder setJavaSourceRoots(File[] sourceRoots) {
            config.javaSourceDirectories = Objects.requireNonNull(sourceRoots);
            return this;
        }

        public Builder setCompiledClassDirectory(File classDirectory) {
            config.compiledClassDirectories = classDirectory;
            return this;
        }

        public Configuration build() {
            // return an immutable instance
            return config.copy();
        }
    }

}
