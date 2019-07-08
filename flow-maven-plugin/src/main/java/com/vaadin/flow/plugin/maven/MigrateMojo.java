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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.migration.CopyMigratedResourcesStep;
import com.vaadin.flow.plugin.migration.CopyResourcesStep;
import com.vaadin.flow.plugin.migration.CreateMigrationJsonsStep;
import com.vaadin.flow.server.frontend.FrontendUtils;

import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * This goal migrates project from compatibility mode to NPM mode.
 *
 * @author Vaadin Ltd
 *
 */
@Mojo(name = "migrate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class MigrateMojo extends AbstractMojo {

    private static final String DEPENDENCIES = "dependencies";

    /**
     * A list of directories with files to migrate.
     */
    @Parameter
    private String[] resources;

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
        prepareMigrationDirectory();

        File file = FrontendUtils.getBowerExecutable();
        if (!ensureTools(file == null)) {
            throw new MojoExecutionException(
                    "Could not install tools required for migration (bower or modulizer)");
        }

        Set<String> externalComponents;
        CopyResourcesStep copyStep = new CopyResourcesStep(migrateFolder,
                getResources());
        Map<String, List<String>> paths;
        try {
            paths = copyStep.copyResources();
            externalComponents = copyStep.getBowerComponents();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Couldn't copy resources from source directories "
                            + getResources() + " to the target directory "
                            + migrateFolder,
                    exception);
        }

        List<String> allPaths = new ArrayList<>();
        paths.values().stream().forEach(allPaths::addAll);
        try {
            new CreateMigrationJsonsStep(migrateFolder).createJsons(allPaths);
        } catch (IOException exception) {
            throw new UncheckedIOException("Couldn't generate json files",
                    exception);
        }

        if (!saveBowerComponents(file, externalComponents)) {
            throw new MojoFailureException(
                    "Could not install bower components");
        }

        installNpmPackages();

        if (!runModulizer()) {
            if (ignoreModulizerErrors) {
                getLog().info("Modulizer has exited with error");
            } else {
                throw new MojoFailureException(
                        "Modulizer has exited with error. Unable to proceed.");
            }
        }

        // copy the result JS files into "frontend"
        if (!frontendDirectory.exists()) {
            frontendDirectory.mkdir();
        }
        CopyMigratedResourcesStep copyMigratedStep = new CopyMigratedResourcesStep(
                frontendDirectory, migrateFolder);
        try {
            copyMigratedStep.copyResources();
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Couldn't copy migrated resources  to the target directory "
                            + frontendDirectory,
                    exception);
        }

        try {
            cleanUp(migrateFolder);
        } catch (IOException exception) {
            getLog().debug("Couldn't remove ");
        }

        if (!keepOriginal) {
            removeOriginalResources(paths);
        }
    }

    private void prepareMigrationDirectory() {
        if (migrateFolder.exists()) {
            try {
                cleanUp(migrateFolder);
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "Unable to clean up directory '" + migrateFolder + "'",
                        exception);
            }
        }
        migrateFolder.mkdirs();
    }

    private void removeOriginalResources(Map<String, List<String>> paths) {
        for (Entry<String, List<String>> entry : paths.entrySet()) {
            File resourceFolder = new File(entry.getKey());
            List<String> resources = entry.getValue();
            resources.forEach(path -> new File(resourceFolder, path).delete());
        }
    }

    private void installNpmPackages() throws MojoFailureException {
        List<String> npmExec = FrontendUtils
                .getNpmExecutable(project.getBasedir().getPath());
        List<String> npmInstall = new ArrayList<>(npmExec.size());
        npmInstall.addAll(npmExec);
        npmInstall.add("i");

        if (!executeProcess(npmInstall, "Couln't install packages using npm",
                "Packages sucessfully installed",
                "Error when running `npm install`")) {
            throw new MojoFailureException(
                    "Error during package installation via npm");
        }
    }

    private boolean runModulizer() {
        Collection<String> depMapping = makeDependencyMapping();

        List<String> command = new ArrayList<>();
        command.add("node_modules/.bin/modulizer");
        command.add("--force");
        command.add("--out");
        command.add(".");
        command.add("--import-style=name");
        if (!depMapping.isEmpty()) {
            command.add("--dependency-mapping");
            command.addAll(depMapping);
        }

        return executeProcess(command, "Migration has finished with errors",
                "Modulizer has completed sucessfully",
                "Error when running moulizer");
    }

    private Collection<String> makeDependencyMapping() {
        File bower = new File(migrateFolder, "bower.json");

        try {
            Set<String> result = new HashSet<>();
            String content = Files.readAllLines(bower.toPath()).stream()
                    .collect(Collectors.joining("\n"));
            JsonObject object = Json.parse(content);
            if (object.hasKey(DEPENDENCIES)) {
                JsonObject deps = object.getObject(DEPENDENCIES);
                for (String key : deps.keys()) {
                    if (key.startsWith("vaadin-")) {
                        result.add(makeVaadinDependencyMapping(deps, key));
                    }
                }
            }
            return result;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read bower.json",
                    exception);
        }
    }

    private String makeVaadinDependencyMapping(JsonObject deps, String key) {
        JsonValue version = deps.get(key);
        StringBuilder builder = new StringBuilder(key);
        builder.append(',');
        builder.append("@vaadin/");
        builder.append(key).append(',');
        builder.append(version.asString());
        return builder.toString();
    }

    private boolean saveBowerComponents(File bowerExecutable,
            Collection<String> components) throws MojoExecutionException {
        List<String> command = new ArrayList<>();
        command.add(bowerExecutable.getAbsolutePath());
        // install
        command.add("i");
        // -F option means: Force latest version on conflict
        command.add("-F");
        // disable interactive mode
        command.add("--config.interactive=false");
        // -S option means: Save installed packages into the project’s
        // bower.json dependencies
        command.add("-S");

        // add all extracted bower components to install them and save

        // the latest polymer version which is chosen by bower has only JS
        // module file. It won't be resolved from the import properly. So we
        // have to force 2.x.x version which is P2 based.
        command.add("polymer#2.8.0");
        components.stream().filter(component -> !component.equals("polymer"))
                .forEach(command::add);

        return executeProcess(command,
                "Couldn't install and save bower components",
                "All components are installed and saved sucessfully",
                "Error when running `bower install`");
    }

    private boolean ensureTools(boolean needInstallBower) {
        List<String> npmExecutable = FrontendUtils
                .getNpmExecutable(project.getBasedir().getPath());
        List<String> command = new ArrayList<>();
        command.addAll(npmExecutable);
        command.add("install");
        command.add("-g");
        if (needInstallBower) {
            command.add("bower");
        }
        command.add("polymer-modulizer");

        return executeProcess(command, "Couldn't install migration tools",
                "Bower is installed sucessfully",
                "Error when running `npm install`");
    }

    private boolean executeProcess(List<String> command, String errorMsg,
            String successMsg, String exceptionMsg) {
        ProcessBuilder builder = FrontendUtils.createProcessBuilder(command);
        builder.directory(migrateFolder);

        Process process = null;
        try {
            process = builder.inheritIO().start();
            int errorCode = process.waitFor();
            if (errorCode != 0) {
                getLog().error(errorMsg);
                return false;
            } else {
                getLog().debug(successMsg);
            }
        } catch (InterruptedException | IOException e) {
            getLog().error(exceptionMsg, e);
            return false;
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }

        return true;
    }

    private void cleanUp(File dir) throws IOException {
        Files.walkFileTree(dir.toPath(), new RemoveVisitor());
    }

    private String[] getResources() {
        if (resources == null) {
            File webApp = new File(project.getBasedir() + "/src/main/webapp");
            File frontend = new File(webApp, "frontend");
            if (frontend.exists() && webApp.listFiles().length == 1) {
                resources = new String[] { frontend.getPath() };
            } else {
                resources = new String[] { webApp.getPath() };
            }
        }
        return resources;
    }

}
