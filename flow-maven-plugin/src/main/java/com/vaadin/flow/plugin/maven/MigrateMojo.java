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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.plugin.common.FlowPluginFrontendUtils;
import com.vaadin.flow.plugin.migration.CopyMigratedResourcesStep;
import com.vaadin.flow.plugin.migration.CopyResourcesStep;
import com.vaadin.flow.plugin.migration.CreateMigrationJsonsStep;
import com.vaadin.flow.plugin.migration.RewriteHtmlImportsStep;
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
@Mojo(name = "migrate-to-p3", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class MigrateMojo extends AbstractMojo {

    private static final String DEPENDENCIES = "dependencies";

    public static enum HtmlImportsRewriteStrategy {
        ALWAYS, SKIP, SKIP_ON_ERROR;
    }

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

    /**
     * Allows to specify the strategy to use to rewrite {@link HtmlImport}
     * annotations in Java files.
     * <p>
     * Three values are available:
     * <ul>
     * <li>ALWAYS : if chosen then {@link HtmlImport} will be always rewritten
     * regardless of migration of the import files content
     * <li>SKIP : if chosen then neither {@link HtmlImport} annotation will be
     * rewritten
     * <li>SKIP_ON_ERROR : if chosen then {@link HtmlImport} annotations will be
     * rewritten only if there are no errors during migration of imported files
     * content
     * </ul>
     */
    @Parameter(defaultValue = "ALWAYS")
    private HtmlImportsRewriteStrategy htmlImportsRewrite;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        prepareMigrationDirectory();

        List<String> bowerCommands = FrontendUtils
                .getBowerExecutable(migrateFolder.getPath());
        boolean needInstallBower = bowerCommands.isEmpty();
        if (!ensureTools(needInstallBower)) {
            throw new MojoExecutionException(
                    "Could not install tools required for migration (bower or modulizer)");
        }
        if (needInstallBower) {
            bowerCommands = FrontendUtils
                    .getBowerExecutable(migrateFolder.getPath());
        }

        if (bowerCommands.isEmpty()) {
            throw new MojoExecutionException(
                    "Could not locate bower. Install it manually on your system and re-run migration goal.");
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
                            + Arrays.asList(getResources())
                            + " to the target directory " + migrateFolder,
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

        if (!saveBowerComponents(bowerCommands, externalComponents)) {
            throw new MojoFailureException(
                    "Could not install bower components");
        }

        installNpmPackages();

        boolean modulizerHasErrors = false;
        if (!runModulizer()) {
            modulizerHasErrors = true;
            if (ignoreModulizerErrors) {
                getLog().info("Modulizer has exited with error");
            } else {
                throw new MojoFailureException(
                        "Modulizer has exited with error. Unable to proceed.");
            }
        }

        // copy the result JS files into "frontend"
        if (!frontendDirectory.exists()) {
            try {
                FileUtils.forceMkdir(frontendDirectory);
            } catch (IOException exception) {
                throw new UncheckedIOException(
                        "Unable to create a target folder for migrated files: '"
                                + frontendDirectory + "'",
                        exception);
            }
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
            getLog().debug("Couldn't remove " + migrateFolder.getPath(),
                    exception);
        }

        if (!modulizerHasErrors && !keepOriginal) {
            removeOriginalResources(paths);
        }

        switch (htmlImportsRewrite) {
        case SKIP:
            break;
        case ALWAYS:
            rewrite();
            break;
        case SKIP_ON_ERROR:
            if (!modulizerHasErrors) {
                rewrite();
            }
            break;
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
        try {
            FileUtils.forceMkdir(migrateFolder);
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Unable to create a folder for migration: '" + migrateFolder
                            + "'",
                    exception);
        }
    }

    private void removeOriginalResources(Map<String, List<String>> paths) {
        for (Entry<String, List<String>> entry : paths.entrySet()) {
            File resourceFolder = new File(entry.getKey());
            entry.getValue()
                    .forEach(path -> new File(resourceFolder, path).delete());
        }
    }

    private void installNpmPackages() throws MojoFailureException {
        List<String> npmExec = FrontendUtils
                .getNpmExecutable(project.getBasedir().getPath());
        List<String> npmInstall = new ArrayList<>(npmExec.size());
        npmInstall.addAll(npmExec);
        npmInstall.add("i");

        if (!executeProcess(npmInstall, "Couldn't install packages using npm",
                "Packages successfully installed",
                "Error when running `npm install`")) {
            throw new MojoFailureException(
                    "Error during package installation via npm");
        }
    }

    private boolean runModulizer() {
        Collection<String> depMapping = makeDependencyMapping();

        List<String> command = new ArrayList<>();
        command.add(FrontendUtils
                .getNodeExecutable(project.getBasedir().getPath()));
        command.add("node_modules/polymer-modulizer/bin/modulizer.js");
        command.add("--force");
        command.add("--out");
        command.add(".");
        command.add("--import-style=name");
        if (!depMapping.isEmpty()) {
            command.add("--dependency-mapping");
            command.addAll(depMapping);
        }

        return executeProcess(command, "Migration has finished with errors",
                "Modulizer has completed successfully",
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
                Stream.of(deps.keys()).filter(key -> key.startsWith("vaadin-"))
                        .forEach(key -> result
                                .add(makeVaadinDependencyMapping(deps, key)));
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

    private boolean saveBowerComponents(List<String> bowerCommands,
            Collection<String> components) {
        List<String> command = new ArrayList<>();
        command.addAll(bowerCommands);
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
                "All components are installed and saved successfully",
                "Error when running `bower install`");
    }

    private boolean ensureTools(boolean needInstallBower) {
        List<String> npmExecutable = FrontendUtils
                .getNpmExecutable(project.getBasedir().getPath());
        List<String> command = new ArrayList<>();
        command.addAll(npmExecutable);
        command.add("install");
        if (needInstallBower) {
            command.add("bower");
        }
        command.add("polymer-modulizer");

        return executeProcess(command, "Couldn't install migration tools",
                "Bower is installed successfully",
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
        FileUtils.forceDelete(dir);
    }

    private String[] getResources() {
        if (resources == null) {
            File webApp = new File(project.getBasedir(), "src/main/webapp");
            resources = new String[] { webApp.getPath() };
        }
        return resources;
    }

    private void rewrite() {
        RewriteHtmlImportsStep step = new RewriteHtmlImportsStep(
                new File(project.getBuild().getOutputDirectory()),
                FlowPluginFrontendUtils.getClassFinder(project),
                project.getCompileSourceRoots().stream().map(File::new)
                        .collect(Collectors.toList()));
        step.rewrite();
    }

}
