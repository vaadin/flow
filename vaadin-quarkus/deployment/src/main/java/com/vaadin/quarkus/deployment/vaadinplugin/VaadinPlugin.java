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
package com.vaadin.quarkus.deployment.vaadinplugin;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;

import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.BundleValidationUtil;
import com.vaadin.flow.server.frontend.ExecutionFailedException;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TaskCleanFrontendFiles;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner;
import com.vaadin.flow.theme.Theme;
import com.vaadin.pro.licensechecker.LicenseChecker;
import com.vaadin.pro.licensechecker.MissingLicenseKeyException;

/**
 * Implementation of the Vaadin plugin.
 * <p>
 * </p>
 * This class is a porting of Vaadin Maven prepare-frontend and build-frontend
 * mojos.
 */
public final class VaadinPlugin {

    private final QuarkusPluginAdapter pluginAdapter;
    private final TaskCleanFrontendFiles cleanTask;

    /**
     * Creates a new instance of Quarkus Vaadin plugin for the given build
     * configuration and application.
     *
     * @param vaadinConfig
     *            the Vaadin build configuration.
     * @param applicationModel
     *            the application model.
     * @param workspaceModule
     *            the workspace module.
     */
    private VaadinPlugin(VaadinBuildTimeConfig vaadinConfig,
            ApplicationModel applicationModel,
            WorkspaceModule workspaceModule) {
        this.pluginAdapter = new QuarkusPluginAdapter(vaadinConfig,
                applicationModel, workspaceModule);
        this.cleanTask = createCleanFrontendFilesTask(this.pluginAdapter);
    }

    /**
     * Creates a new instance of the VaadinPlugin based on the provided
     * configuration and application model. If necessary, it attempts to load
     * workspace information for the plugin.
     *
     * @param vaadinConfig
     *            the Vaadin build time configuration.
     * @param applicationModel
     *            the application model representing the current application.
     * @param outputTarget
     *            the target directory for output operations.
     * @return an instance of VaadinPlugin initialized with the given
     *         parameters.
     * @throws BuildException
     *             if workspace information cannot be loaded or an error occurs
     *             during the process.
     */
    public static VaadinPlugin of(VaadinBuildTimeConfig vaadinConfig,
            ApplicationModel applicationModel, Path outputTarget)
            throws BuildException {
        WorkspaceModule module = applicationModel.getApplicationModule();
        if (module == null) {
            try {
                module = WorkspaceInfo.load(outputTarget);
            } catch (Exception e) {
                throw new BuildException(
                        "Cannot load workspace information for Vaadin plugin. quarkus.bootstrap.workspace-discovery=true might be required.",
                        e, List.of());
            }
            if (module == null) {
                // Nothing was saved either, which is the same situation as a
                // failed read and has the same remedy. Without this the plugin
                // is built around a null module and fails later with a bare
                // NullPointerException that says none of it.
                throw new BuildException(
                        "Cannot load workspace information for Vaadin plugin. quarkus.bootstrap.workspace-discovery=true might be required.",
                        List.of());
            }
        }
        return new VaadinPlugin(vaadinConfig, applicationModel, module);
    }

    /**
     * Checks that node and npm tools are installed and creates or updates
     * `package.json` and the frontend build tool configuration files.
     * <p>
     * </p>
     * Copies frontend resources available inside `.jar` dependencies to
     * `node_modules` when building a jar package.
     *
     * @throws BuildException
     *             if any error occurs.
     */
    public void prepareFrontend() throws BuildException {
        // propagate info via System properties and token file
        BuildFrontendUtil.propagateBuildInfo(pluginAdapter);

        try {
            BuildFrontendUtil.prepareFrontend(pluginAdapter);
        } catch (Exception exception) {
            throw new BuildException("Could not execute prepare-frontend goal.",
                    exception, List.of());
        }
    }

    /**
     * Builds the frontend bundle.
     * <p>
     * </p>
     * It performs the following actions when creating a package:
     * <ul>
     * <li>Update {@link Constants#PACKAGE_JSON} file with the
     * {@link NpmPackage} annotations defined in the classpath,</li>
     * <li>Copy resource files used by flow from `.jar` files to the
     * `node_modules` folder</li>
     * <li>Install dependencies by running <code>npm install</code></li>
     * <li>Update the {@link FrontendUtils#IMPORTS_NAME} file imports with the
     * {@link JsModule} {@link Theme} and {@link JavaScript} annotations defined
     * in the classpath,</li>
     * <li>Update {@link FrontendUtils#VITE_CONFIG} file.</li>
     * </ul>
     * <p>
     * Once the build is done the generated files are handed to the emitter and
     * the build info token file is deleted from the build output directory.
     * Nothing is deleted before the files that have to reach the application
     * whichever way it is packaged are known to have reached it: a file that
     * cannot be read fails the build, and so does one that was not emitted.
     * Deleting the token file after it failed to reach the application would
     * package an application without a {@literal flow-build-info.json} while
     * the build reports success.
     *
     * @param emitter
     *            generated files emitter.
     * @throws BuildException
     *             if any error occurs.
     */
    public void buildFrontend(BiConsumer<String, byte[]> emitter)
            throws BuildException {
        long start = System.nanoTime();

        FrontendDependenciesScanner frontendDependencies = createFrontendScanner();
        try {
            BuildFrontendUtil.runNodeUpdater(pluginAdapter,
                    frontendDependencies);
        } catch (ExecutionFailedException | URISyntaxException exception) {
            throw new BuildException("Could not execute build-frontend goal",
                    exception, List.of());
        }

        if (pluginAdapter.generateBundle()
                && BundleValidationUtil.needsBundleBuild(
                        pluginAdapter.servletResourceOutputDirectory())) {
            try {
                BuildFrontendUtil.runFrontendBuild(pluginAdapter);
            } catch (URISyntaxException exception) {
                throw new BuildException(exception.getMessage(), exception,
                        List.of());
            }
        }
        LicenseChecker.setStrictOffline(true);
        boolean licenseRequired;
        boolean commercialBannerRequired;
        try {
            licenseRequired = BuildFrontendUtil.validateLicenses(pluginAdapter,
                    frontendDependencies);
            commercialBannerRequired = false;
        } catch (MissingLicenseKeyException ex) {
            licenseRequired = true;
            commercialBannerRequired = true;
            pluginAdapter.logInfo(ex.getMessage());
        }

        BuildFrontendUtil.updateBuildFile(pluginAdapter, licenseRequired,
                commercialBannerRequired);

        long ms = (System.nanoTime() - start) / 1000000;
        pluginAdapter.logInfo("Build frontend completed in " + ms + " ms.");

        emitGeneratedFiles(emitter);
        verifyAlwaysEmitted(emitter);
        removeTokenFile();
    }

    /**
     * Fails the build when a file that has to reach the application whichever
     * way it is packaged did not.
     * <p>
     * {@link #removeTokenFile()} runs right after this and deletes the build
     * info token file from the build output directory, so a token file that did
     * not reach the application is gone for good: the application is packaged
     * without a {@literal flow-build-info.json}, Flow cannot find the
     * production bundle at run time, and the build reports success. Stopping
     * here leaves the file where it is and says what happened.
     * <p>
     * Telling which generated file is the token file is
     * {@link GeneratedResourceEmitter}'s job, and it asks the file system so
     * that the answer holds wherever the build runs. This is the check that the
     * answer was what it had to be, so that a file system behaving in a way
     * nobody anticipated fails the build instead of shipping an application
     * that cannot start in production mode.
     * <p>
     * Only the emitter this plugin creates keeps track of what it emitted. A
     * caller passing its own emitter decides for itself what reaches the
     * application, and nothing is checked.
     *
     * @param emitter
     *            the emitter the generated files were handed to.
     * @throws BuildException
     *             if a file that has to be emitted was not.
     */
    void verifyAlwaysEmitted(BiConsumer<String, byte[]> emitter)
            throws BuildException {
        if (!(emitter instanceof GeneratedResourceEmitter generatedResources)) {
            return;
        }
        Set<Path> notEmitted = generatedResources.notEmitted();
        if (!notEmitted.isEmpty()) {
            throw new BuildException(
                    "The Vaadin build produced files that have to be added to the application because the build deletes them from the build output directory, but they were not added: "
                            + notEmitted
                            + ". The application would be packaged without them and would not start in production mode.",
                    List.of());
        }
    }

    /**
     * Creates the emitter that registers the generated Vaadin files with the
     * application.
     * <p>
     * The emitter skips the files that packaging already copies into the
     * artifact by itself, so that they are not added a second time.
     *
     * @param packagedRootDirectories
     *            the directories Quarkus packages the application from, as
     *            reported by
     *            {@link io.quarkus.deployment.builditem.ArchiveRootBuildItem#getRootDirectories()}.
     * @param producer
     *            the producer registering the files with the application.
     * @return the emitter to pass to {@link #buildFrontend(BiConsumer)}.
     * @see GeneratedResourceEmitter
     */
    public BiConsumer<String, byte[]> createGeneratedResourceEmitter(
            Iterable<Path> packagedRootDirectories,
            BuildProducer<GeneratedResourceBuildItem> producer) {
        return GeneratedResourceEmitter.of(packagedRootDirectories,
                pluginAdapter.servletResourceOutputDirectory().toPath()
                        .normalize(),
                pluginAdapter.buildDir().normalize(), producer);
    }

    /**
     * Deletes the build info token file from the build output directory.
     * <p>
     * The token file has already been added to the application as a generated
     * resource, so the copy on disk is not needed to package it. Leaving it
     * there adds the same file to the artifact twice, which makes Flow log a
     * warning that it cannot tell which {@literal flow-build-info.json} is the
     * correct one. It would also be picked up by a later Quarkus dev mode run
     * from the same output directory, starting the application in production
     * mode. The Vaadin Maven plugin deletes the file for the same reasons.
     * <p>
     * Deleting it here is why {@link GeneratedResourceEmitter} emits the token
     * file whichever way the application is packaged: once it is gone from the
     * output directory, emitting is the only way it reaches the application.
     * <p>
     * A failure to delete it is reported as a warning rather than failing the
     * build. By the time this runs the token file has already been added to the
     * application, so the copy left behind costs the duplicate warning and the
     * stale dev mode read described above, which are a nuisance and not a
     * broken artifact. Deleting a file that another process holds open fails on
     * Windows, where locking is mandatory, so a build whose frontend build
     * fully succeeded would otherwise fail there for a leftover file.
     */
    void removeTokenFile() {
        try {
            BuildFrontendUtil.removeBuildFile(pluginAdapter);
        } catch (IOException e) {
            pluginAdapter.logWarn(
                    "Failed to delete the Vaadin build info token file from the build output directory. "
                            + "It is packaged with the application anyway, but the copy left behind is packaged as well, "
                            + "so Flow may warn that it cannot tell which flow-build-info.json is the correct one, "
                            + "and a later dev mode run on the same output directory starts in production mode. "
                            + "Another process holding the file open, such as a virus scanner, is a likely cause.",
                    e);
        }
    }

    /**
     * Hands every file the Vaadin build produced to the emitter.
     * <p>
     * A file that cannot be read fails the build instead of being skipped with
     * a warning. The emitter is the only way some of those files reach the
     * application, the build info token file in particular, which
     * {@link #removeTokenFile()} deletes from the build output directory right
     * after this method returns. Skipping one would package an application
     * missing it, with nothing but a warning to say so.
     *
     * @param emitter
     *            generated files emitter.
     * @throws BuildException
     *             if the generated resources directory is outside the build
     *             output directory, cannot be walked, or one of the files in it
     *             cannot be read or handed to the emitter.
     */
    void emitGeneratedFiles(BiConsumer<String, byte[]> emitter)
            throws BuildException {
        Path vaadinMetaInfDir = pluginAdapter.servletResourceOutputDirectory()
                .toPath().normalize();
        Path buildFolder = pluginAdapter.buildDir().normalize();

        if (!Files.exists(vaadinMetaInfDir)) {
            pluginAdapter.logInfo(
                    "No META-INF/VAADIN directory found, skipping resource addition");
            return;
        }

        // Files are added to the application under their path relative to the
        // build output directory, which only names a resource when they are
        // below it. Path.startsWith is false for a path of another file
        // system, and on Windows for a path on another drive, where
        // Path.relativize would throw. On a file system that can express the
        // relative path anyway, it would come out prefixed with '..' and name
        // no resource the application can load.
        if (!vaadinMetaInfDir.startsWith(buildFolder)) {
            throw new BuildException("The Vaadin build writes into "
                    + vaadinMetaInfDir
                    + ", which is outside the build output directory "
                    + buildFolder
                    + ". The files it produces cannot be added to the application from there. "
                    + "Set 'vaadin.build.generated-resource-output-directory' to a directory inside the build output directory.",
                    List.of());
        }

        List<Path> generatedFiles;
        // Files.walk reports a failure to walk the tree as an
        // UncheckedIOException thrown by the terminal operation, so the files
        // are collected inside the try block.
        try (var stream = Files.walk(vaadinMetaInfDir)) {
            generatedFiles = stream.filter(Files::isRegularFile).toList();
        } catch (IOException | UncheckedIOException e) {
            throw new BuildException(
                    "Failed to list the files produced by the Vaadin build in "
                            + vaadinMetaInfDir
                            + ". The generated META-INF/VAADIN resources cannot be added to the application.",
                    e, List.of());
        }

        for (Path filePath : generatedFiles) {
            // Calculate relative path from target/classes
            Path relativePath = buildFolder.relativize(filePath);
            byte[] content;
            try {
                content = Files.readAllBytes(filePath);
            } catch (IOException e) {
                throw new BuildException("Failed to read " + filePath
                        + ", a file produced by the Vaadin build in the META-INF/VAADIN directory. The application would be packaged without it.",
                        e, List.of());
            }
            try {
                emitter.accept(relativePath.toString().replace('\\', '/'),
                        content);
            } catch (UncheckedIOException e) {
                // The emitter asks the file system which of the generated
                // files it has to add whichever way the application is
                // packaged, and cannot answer that here.
                throw new BuildException("Failed to add " + filePath
                        + ", a file produced by the Vaadin build, to the application. "
                        + e.getMessage(), e, List.of());
            }

            pluginAdapter.logDebug("Added Vaadin resource: " + relativePath);
        }

        pluginAdapter.logInfo(
                "Added Vaadin frontend resources from META-INF/VAADIN to artifact");
    }

    /**
     * Cleans up generated frontend files if the corresponding configuration
     * setting is enabled. The process involves creating a new cleaning task
     * with specific options derived from the plugin adapter's configuration and
     * executing it. If an error occurs during the execution of the clean task,
     * it is logged for debugging purposes.
     * <p>
     * The cleanup operation ensures the following: - Deletes the generated
     * frontend files in the `node_modules` folder. - Utilizes the directory
     * configurations such as frontend directory, npm folder, and generated
     * TypeScript folder to locate the files. - Makes use of the
     * `TaskCleanFrontendFiles` for cleanup operations.
     * <p>
     * Unlike the Vaadin Maven plugin, which only cleans when a frontend bundle
     * has been built, this runs after every successful build. This plugin
     * always reports frontend hotdeploy as enabled, so even a build that reuses
     * a prebuilt production bundle writes {@literal package.json}, the Vite
     * configuration files, {@literal tsconfig.json} and {@literal types.d.ts}
     * into the project directory. Skipping the cleanup for those builds would
     * leave the files behind.
     * <p>
     * Errors are logged rather than propagated, including unchecked ones. This
     * method runs as a Quarkus build closeable, which cannot fail the build and
     * logs anything thrown out of here at debug level only, so an error that is
     * not caught here goes unnoticed.
     */
    public void clean() {
        if (cleanTask != null) {
            try {
                cleanTask.execute();
            } catch (ExecutionFailedException | RuntimeException exception) {
                pluginAdapter.logError("Error cleaning frontend files",
                        exception);
            }
        }
    }

    private TaskCleanFrontendFiles createCleanFrontendFilesTask(
            QuarkusPluginAdapter pluginAdapter) {
        if (pluginAdapter.cleanFrontendFiles()) {
            Options options = new Options(null, pluginAdapter.getClassFinder(),
                    pluginAdapter.npmFolder())
                    .withFrontendDirectory(pluginAdapter.frontendDirectory())
                    .withFrontendGeneratedFolder(
                            pluginAdapter.generatedTsFolder());
            return new TaskCleanFrontendFiles(options);
        }
        return null;
    }

    private FrontendDependenciesScanner createFrontendScanner() {
        boolean reactEnabled = pluginAdapter.isReactEnabled()
                && FrontendUtils.isReactRouterRequired(
                        BuildFrontendUtil.getFrontendDirectory(pluginAdapter));
        ClassFinder classFinder = pluginAdapter.getClassFinder();
        FeatureFlags featureFlags = new FeatureFlags(
                pluginAdapter.createLookup(classFinder));
        if (pluginAdapter.javaResourceFolder() != null) {
            featureFlags
                    .setPropertiesLocation(pluginAdapter.javaResourceFolder());
        }
        return new FrontendDependenciesScanner.FrontendDependenciesScannerFactory()
                .createScanner(!pluginAdapter.optimizeBundle(), classFinder,
                        pluginAdapter.generateEmbeddableWebComponents(),
                        featureFlags, reactEnabled);
    }

}
