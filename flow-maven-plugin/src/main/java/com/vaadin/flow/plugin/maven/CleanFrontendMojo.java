/*
 * Copyright 2000-2021 Vaadin Ltd.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.impl.JsonUtil;

/**
 * Goal that cleans the frontend files to a clean state.
 * <p>
 * Deletes Vaadin dependencies from package.json, the generated frontend folder
 * and the npm/pnpm-related files and folders:
 * <ul>
 * <li>node_modules
 * <li>pnpm-lock.yaml
 * <li>package-lock.json
 * </ul>
 *
 * @since 9.0
 */
@Mojo(name = "clean-frontend", defaultPhase = LifecyclePhase.CLEAN)
public class CleanFrontendMojo extends FlowModeAbstractMojo {

    public static final String VAADIN = "vaadin";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEV_DEPENDENCIES = "devDependencies";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File npmFolder;

    /**
     * Build directory for the project.
     */
    @Parameter(property = "build.folder", defaultValue = "${project.build.directory}")
    private String projectBuildDir;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File projectBasedir;

    @Override
    public void execute() throws MojoFailureException {
        removeNodeModules();

        // Cleanup (p)npm lock file.
        File lockFile = new File(npmFolder, "pnpm-lock.yaml");
        if (!lockFile.exists()) {
            lockFile = new File(npmFolder, "package-lock.json");
        }
        if (lockFile.exists()) {
            lockFile.delete();
        }

        // clean up generated files from frontend
        File generatedFrontendFolder = frontendDirectory.toPath().resolve("generated").toFile();
        if (generatedFrontendFolder.exists()) {
            try {
                FileUtils.deleteDirectory(generatedFrontendFolder);
            } catch (IOException exception) {
                throw new MojoFailureException(
                        "Failed to remove folder'"
                                + generatedFrontendFolder.getAbsolutePath() + "'",
                        exception);
            }
        }

        try {
            // Clean up package json framework managed versions.
            File packageJsonFile = new File(npmFolder, "package.json");
            if (packageJsonFile.exists()) {
                JsonObject packageJson = Json.parse(FileUtils.readFileToString(
                        packageJsonFile, StandardCharsets.UTF_8.name()));

                cleanupPackage(packageJson);

                FileUtils.write(packageJsonFile,
                        JsonUtil.stringify(packageJson, 2) + "\n",
                        StandardCharsets.UTF_8.name());
            }
        } catch (IOException e) {
            throw new MojoFailureException(
                    "Failed to clean 'package.json' file", e);
        }
    }

    @Override
    boolean isDefaultCompatibility() {
        return false;
    }

    /**
     * Try to remove the node_modules folder.
     * <p>
     * Log a warning if there was an issue removing the folder.
     */
    private void removeNodeModules() {
        // Remove node_modules folder
        File nodeModules = new File(npmFolder, "node_modules");
        if (nodeModules.exists()) {
            try {
                FileUtils.deleteDirectory(nodeModules);
            } catch (IOException exception) {
                getLog().debug("Exception removing node_modules", exception);
                getLog().error(
                        "Failed to remove '" + nodeModules.getAbsolutePath()
                                + "'. Please remove it manually.");
            }
        }
    }

    private void cleanupPackage(JsonObject packageJson) {
        JsonObject dependencies = packageJson.getObject(DEPENDENCIES);
        JsonObject devDependencies = packageJson.getObject(DEV_DEPENDENCIES);

        if (packageJson.hasKey(VAADIN)) {
            JsonObject vaadin = packageJson.getObject(VAADIN);
            JsonObject vaadinDependencies = vaadin.getObject(DEPENDENCIES);
            JsonObject vaadinDevDependencies = vaadin
                    .getObject(DEV_DEPENDENCIES);

            // Remove all
            cleanObject(dependencies, vaadinDependencies);
            cleanObject(devDependencies, vaadinDevDependencies);
            packageJson.remove(VAADIN);
        }

        cleanFrameworkBuildDependenices(dependencies);
        cleanFrameworkBuildDependenices(devDependencies);

        // Remove the hash to get a npm install executed
        packageJson.remove("hash");

    }

    private void cleanObject(JsonObject target, JsonObject reference) {
        if (target == null) {
            return;
        }
        Set<String> removeKeys = new HashSet<>();

        for (String key : target.keys()) {
            if (reference.hasKey(key)
                    && target.getString(key).equals(reference.getString(key))) {
                removeKeys.add(key);
            }
        }

        for (String key : removeKeys) {
            target.remove(key);
        }
    }

    /**
     * Clean any dependencies that target the build folder in the given json
     * object.
     * <p>
     * With default settings it would mean all starting with {@code ./target}.
     *
     * @param dependencyObject
     *            json object to clean
     */
    private void cleanFrameworkBuildDependenices(JsonObject dependencyObject) {
        if (dependencyObject == null) {
            return;
        }
        String buildFolder = projectBuildDir;
        if (projectBuildDir.startsWith(projectBasedir.toString())) {
            buildFolder = projectBasedir.toPath().relativize(Paths.get(projectBuildDir))
                    .toString();
        }
        String buildTargetFolder = "./" + buildFolder;

        Set<String> removeKeys = new HashSet<>();
        for (String key : dependencyObject.keys()) {
            if (dependencyObject.getString(key).startsWith(buildTargetFolder)) {
                removeKeys.add(key);
            }
        }

        for (String key : removeKeys) {
            dependencyObject.remove(key);
        }
    }

}
