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

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * Configuration for migration.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class MigrationConfiguration {

    private File tempMigrationFolder;

    private File[] resourceDirectories;

    private File targetDirectory;

    private boolean keepOriginalFiles;

    private boolean ignoreModulizerErrors = true;

    private AnnotationsRewriteStrategy annotationRewriteStrategy = AnnotationsRewriteStrategy.ALWAYS;

    private final File baseDirectory;

    private ClassFinder classFinder;

    private File[] javaSourceDirectories;

    private File compiledClassDirectory;

    private MigrationConfiguration(File baseDir) {
        baseDirectory = baseDir;
    }

    private MigrationConfiguration(MigrationConfiguration configuration) {
        this.tempMigrationFolder = configuration.getTempMigrationFolder();
        if (configuration.getResourceDirectories() != null) {
            this.resourceDirectories = configuration.getResourceDirectories()
                    .clone();
        }
        this.targetDirectory = configuration.getTargetDirectory();
        this.keepOriginalFiles = configuration.isKeepOriginalFiles();
        this.ignoreModulizerErrors = configuration.isIgnoreModulizerErrors();
        this.annotationRewriteStrategy = configuration
                .getAnnotationRewriteStrategy();
        this.baseDirectory = configuration.getBaseDirectory();
        this.classFinder = configuration.getClassFinder();
        if (configuration.getJavaSourceDirectories() != null) {
            this.javaSourceDirectories = configuration
                    .getJavaSourceDirectories().clone();
        }
        this.compiledClassDirectory = configuration.getCompiledClassDirectory();
    }

    /**
     * Gets the migration folder.
     *
     * @return the migration folder
     */
    public File getTempMigrationFolder() {
        return tempMigrationFolder;
    }

    /**
     * Gets the resource directories.
     *
     * @return the resource directories
     */
    public File[] getResourceDirectories() {
        return resourceDirectories;
    }

    /**
     * Gets the target directory.
     *
     * @return the target directory
     */
    public File getTargetDirectory() {
        return targetDirectory;
    }

    /**
     * Checks whether the original resource files should be preserved.
     *
     * @return whether the original resource files should be preserved
     */
    public boolean isKeepOriginalFiles() {
        return keepOriginalFiles;
    }

    /**
     * Checks whether Modulizer errors should be ignored.
     *
     * @return whether Modulizer errors should be ignored
     */
    public boolean isIgnoreModulizerErrors() {
        return ignoreModulizerErrors;
    }

    /**
     * Gets the base directory
     *
     * @return the base directory
     */
    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Gets the compiled classes directory.
     *
     * @return the compiled classes directory
     */
    public File getCompiledClassDirectory() {
        return compiledClassDirectory;
    }

    /**
     * Gets the class finder.
     *
     * @return the class finder
     */
    public ClassFinder getClassFinder() {
        return classFinder;
    }

    /**
     * Gets the java source roots.
     *
     * @return the java source roots
     */
    public File[] getJavaSourceDirectories() {
        return javaSourceDirectories;
    }

    /**
     * Gets the annotation rewrite strategy.
     *
     * @return the annotation rewrite strategy
     */
    public AnnotationsRewriteStrategy getAnnotationRewriteStrategy() {
        return annotationRewriteStrategy;
    }

    /**
     * A builder for {@link MigrationConfiguration}. Allows to set all required
     * parameters via setters fluent API.
     *
     * @author Vaadin Ltd
     *
     */
    public static class Builder {

        private final MigrationConfiguration config;

        /**
         * Creates a new instance of the builded with provided {@code baseDir}.
         *
         * @param baseDir
         *            base project directory
         */
        public Builder(File baseDir) {
            config = new MigrationConfiguration(baseDir);
        }

        /**
         * Sets temporary migration folder where the migration happens.
         * <p>
         * The folder value is not required. It it's not set then temporary
         * folder will be used. This folder will be removed once the migration
         * happens regardless of result.
         *
         * @param file
         *            the migration folder
         * @return this builder
         */
        public Builder setTemporaryMigrationFolder(File file) {
            config.tempMigrationFolder = Objects.requireNonNull(file);
            return this;
        }

        /**
         * Sets the resource files directories.
         * <p>
         * There can be several resource roots. They contains HTML tempate files
         * and stylesheet files. The hierarchical structure will be transfered
         * into the target directory (see {@link #setTargetDirectory(File)} as
         * is (except "frontend" directory whose files and subdirs are moved
         * ignoring the "frontend" dir).
         *
         * @param resourceDirs
         *            resource roots
         * @return this builder
         */
        public Builder setResourceDirectories(File[] resourceDirs) {
            config.resourceDirectories = Objects.requireNonNull(resourceDirs);
            return this;
        }

        /**
         * Sets the target directory where the resource files will be stored
         * after the migration.
         * <p>
         * This directory is usually is "frontend" folder inside base dir
         *
         * @param targetDir
         *            the target directory
         * @return this builder
         */
        public Builder setTargetDirectory(File targetDir) {
            config.targetDirectory = Objects.requireNonNull(targetDir);
            return this;
        }

        /**
         * Sets whether the original resources should be preserved.
         * <p>
         * By default the original resources are removed if there were no errors
         * during migration.
         *
         * @param keepOriginal
         *            whether the original resources should be preserved
         * @return this builder
         */
        public Builder setKeepOriginalFiles(boolean keepOriginal) {
            config.keepOriginalFiles = keepOriginal;
            return this;
        }

        /**
         * Sets whether the build should be considered successful even if there
         * are Modulizer errors.
         * <p>
         * Modulizer is an external tool which is used internally for converting
         * resource files. Modulizer process exit status may be non zero even if
         * all files have been converted correctly. By default the exit status
         * code is ignored.
         *
         * @param ignoreModulizerErrors
         *            whether the tool should fail is Modulizer fails
         * @return this builder
         */
        public Builder setIgnoreModulizerErrors(boolean ignoreModulizerErrors) {
            config.ignoreModulizerErrors = ignoreModulizerErrors;
            return this;
        }

        /**
         * Sets the class finder instance.
         *
         * @param finder
         *            a class finder
         * @return this builder
         */
        public Builder setClassFinder(ClassFinder finder) {
            config.classFinder = Objects.requireNonNull(finder);
            return this;
        }

        /**
         * Sets java source roots.
         * <p>
         * Java source roots contain java files which should be rewritten to use
         * {@link JsModule} annotation instead of {@link HtmlImport} and
         * {@link StyleSheet}.
         *
         * @param sourceRoots
         *            java source roots
         * @return this builder
         */
        public Builder setJavaSourceRoots(File[] sourceRoots) {
            config.javaSourceDirectories = Objects.requireNonNull(sourceRoots);
            return this;
        }

        /**
         * Sets java binary (compiled) classes directory.
         * <p>
         * The java files from source roots ({@link #setJavaSourceRoots(File[])}
         * must be compiled when the migration is done into this directory.
         *
         * @param classDirectory
         *            compiled java classes directory
         * @return this builder
         */
        public Builder setCompiledClassDirectory(File classDirectory) {
            config.compiledClassDirectory = Objects
                    .requireNonNull(classDirectory);
            return this;
        }

        /**
         * Sets the annotation rewrite strategy.
         * <p>
         * This strategy is used to control annotation ( {@link HtmlImport} and
         * {@link StyleSheet} rewrite logic. By default the annotation are
         * always rewritten regardless of conversation status.
         *
         * @param strategy
         *            the annotation rewrite strategy
         * @return this builder
         */
        public Builder setAnnotationRewriteStrategy(
                AnnotationsRewriteStrategy strategy) {
            config.annotationRewriteStrategy = Objects.requireNonNull(strategy);
            return this;

        }

        /**
         * Builds the immutable configuration based on the builder.
         *
         * @return the resulting configuration
         */
        public MigrationConfiguration build() {
            // return an immutable instance
            return  new MigrationConfiguration(config);
        }
    }

}
