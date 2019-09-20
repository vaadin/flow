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
package com.vaadin.flow.plugin.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.migration.AnnotationsRewriteStrategy;
import com.vaadin.flow.migration.MigrationConfiguration;
import com.vaadin.flow.migration.MigrationConfiguration.Builder;
import com.vaadin.flow.migration.Migration;
import com.vaadin.flow.migration.MigrationFailureException;
import com.vaadin.flow.migration.MigrationToolsException;
import com.vaadin.flow.plugin.common.FlowPluginFrontendUtils;

/**
 * This goal migrates project from compatibility mode to NPM mode.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
@Mojo(name = "migrate-to-p3", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class MigrateMojo extends AbstractMojo {

    /**
     * A list of directories with files to migrate.
     */
    @Parameter
    private File[] resources;

    /**
     * A temporary directory where migration is performed.
     */
    @Parameter(defaultValue = "${project.build.directory}/migration")
    private File migrateFolder;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * A directory with project's frontend source files. The target folder for
     * migrated files.
     */
    @Parameter(defaultValue = "${project.basedir}/frontend")
    private File frontendDirectory;

    /**
     * Whether the original resource files should be preserved or removed.
     */
    @Parameter(defaultValue = "false")
    private boolean keepOriginal;

    /**
     * Stops the goal execution with error if modulizer has exited with not 0
     * status.
     * <p>
     * By default the errors are not fatal and migration is not stopped.
     */
    @Parameter(defaultValue = "true")
    private boolean ignoreModulizerErrors;

    /**
     * Allows to specify the strategy to use to rewrite
     * {@link HtmlImport}/{@link StyleSheet} annotations in Java files.
     * <p>
     * Three values are available:
     * <ul>
     * <li>ALWAYS : if chosen then annotation will be always rewritten
     * regardless of migration of the import files content
     * <li>SKIP : if chosen then neither annotation will be rewritten
     * <li>SKIP_ON_ERROR : if chosen then annotation will be rewritten only if
     * there are no errors during migration of imported files content
     * </ul>
     */
    @Parameter(defaultValue = "ALWAYS")
    private AnnotationsRewriteStrategy annotationsRewrite;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Builder builder = new Builder(project.getBasedir());

        builder.setAnnotationRewriteStrategy(annotationsRewrite);
        builder.setClassFinder(FlowPluginFrontendUtils.getClassFinder(project));
        builder.setCompiledClassDirectory(
                new File(project.getBuild().getOutputDirectory()));
        builder.setIgnoreModulizerErrors(ignoreModulizerErrors);
        builder.setJavaSourceRoots(project.getCompileSourceRoots().stream()
                .map(File::new).toArray(File[]::new));
        builder.setKeepOriginalFiles(keepOriginal);
        builder.setResourceDirectories(getResources());
        builder.setTargetDirectory(frontendDirectory);
        builder.setTemporaryMigrationFolder(migrateFolder);

        MigrationConfiguration configuration = builder.build();
        Migration migration = new Migration(configuration);
        try {
            migration.migrate();
        } catch (MigrationToolsException exception) {
            throw new MojoExecutionException(exception.getMessage(), exception);
        } catch (MigrationFailureException exception) {
            throw new MojoFailureException(exception.getMessage(), exception);
        }
    }

    private File[] getResources() {
        if (resources == null) {
            File webApp = new File(project.getBasedir(), "src/main/webapp");
            resources = new File[] { webApp };
        }
        return resources;
    }

}
