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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.migration.CopyResourcesStep;
import com.vaadin.flow.plugin.migration.CreateMigrationJsonsStep;

/**
 * This goal migrates project from compatibility mode to NPM mode.
 *
 * @author Vaadin Ltd
 *
 */
@Mojo(name = "migrate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class MigrateMojo extends AbstractMojo {

    @Parameter
    private String[] resources;

    @Parameter(defaultValue = "${project.build.directory}/migration")
    private String migrateFolder;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    private static class RemoveVisitor extends SimpleFileVisitor<Path> {

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                throws IOException {
            Files.delete(file);
            return super.visitFile(file, attrs);
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                throws IOException {
            Files.delete(dir);
            return super.postVisitDirectory(dir, exc);
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File targetDir = new File(migrateFolder);
        try {
            cleanUp(targetDir);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to clean up directory '" + migrateFolder + "'",
                    exception);
        }

        CopyResourcesStep copyStep = new CopyResourcesStep(targetDir,
                getResources());
        List<String> paths;
        try {
            paths = copyStep.copyResources();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Couldn't copy resources from source directories "
                            + getResources() + " to the target directory "
                            + targetDir,
                    exception);
        }

        try {
            new CreateMigrationJsonsStep(targetDir).createJsons(paths);
        } catch (IOException exception) {
            throw new UncheckedIOException("Couldn't generate json files",
                    exception);
        }

        /*
         * 4. run Modulizer (after npm install -g bower, npm install -g
         * polymer-modulizer, npm install -g polymer-modulizer, bower i , npm i)
         *
         * 5. copy the result JS files into "frontend"
         *
         * 6. Removes original resources
         */
    }

    private void cleanUp(File dir) throws IOException {
        Files.walkFileTree(dir.toPath(), new RemoveVisitor());
    }

    private String[] getResources() {
        if (resources == null) {
            resources = new String[] {
                    project.getBasedir() + "/src/main/webapp" };
        }
        return resources;
    }

}
