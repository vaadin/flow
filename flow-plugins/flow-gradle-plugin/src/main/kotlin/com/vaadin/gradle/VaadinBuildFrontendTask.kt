/**
 *    Copyright 2000-2026 Vaadin Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.flow.gradle

import com.vaadin.experimental.FeatureFlags
import com.vaadin.flow.plugin.base.BuildFrontendUtil
import com.vaadin.flow.server.Constants
import com.vaadin.flow.server.InitParameters
import com.vaadin.flow.server.frontend.BundleValidationUtil
import com.vaadin.flow.server.frontend.FrontendBuildUtils
import com.vaadin.flow.server.frontend.Options
import com.vaadin.flow.server.frontend.TaskCleanFrontendFiles
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner
import com.vaadin.flow.server.frontend.scanner.FrontendDependenciesScanner.FrontendDependenciesScannerFactory
import com.vaadin.flow.internal.FrontendUtils
import com.vaadin.pro.licensechecker.LicenseChecker
import com.vaadin.pro.licensechecker.MissingLicenseKeyException
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

/**
 * Task that builds the frontend bundle.
 *
 * It performs the following actions when creating a package:
 * * Update [Constants.PACKAGE_JSON] file with the [com.vaadin.flow.component.dependency.NpmPackage]
 * annotations defined in the classpath,
 * * Copy resource files used by flow from `.jar` files to the `node_modules`
 * folder
 * * Install dependencies by running `npm install`
 * * Update the [FrontendUtils.IMPORTS_NAME] file imports with the
 * [com.vaadin.flow.component.dependency.JsModule] [com.vaadin.flow.theme.Theme] and [com.vaadin.flow.component.dependency.JavaScript] annotations defined in
 * the classpath,
 * * Update [FrontendUtils.WEBPACK_CONFIG] file.
 *
 * Uses Gradle incremental builds feature, i.e. Gradle skips this task if
 * all the inputs (config parameters, classpath, frontend sources) and outputs
 * (production bundle) are up-to-date and have the same values as for previous
 * build.
 */
@CacheableTask
public abstract class VaadinBuildFrontendTask : DefaultTask() {

    @get:Internal
    internal abstract val adapter: Property<GradlePluginAdapter>

    @ServiceReference
    internal abstract fun getSvc(): Property<FrontendToolService>

    /**
     * The project's own compiled classes. Tracked with [Classpath] for
     * content-based change detection, since project classes are small and
     * change frequently.
     */
    @get:Classpath
    internal abstract val projectClassesDirs: ConfigurableFileCollection

    /**
     * User-written frontend source files, excluding the `generated/`
     * subdirectory. The `generated/` directory is excluded because it is
     * an output of [VaadinPrepareFrontendTask] and also modified by this
     * task's [vaadinBuildFrontend] action, which would make the inputs
     * unstable across builds.
     */
    @get:InputFiles
    @get:Optional
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal abstract val frontendSourceFiles: ConfigurableFileCollection

    /**
     * A lightweight fingerprint of the dependency JARs on the classpath.
     * Using a fingerprint (name + size + last modified) instead of
     * [Classpath] content hashing avoids reading hundreds of JAR files
     * into memory, which can cause OOM on large projects (e.g. Spring Boot
     * with 200+ transitive dependencies).
     */
    @get:Internal
    internal abstract val dependencyJarFiles: ConfigurableFileCollection

    @get:Input
    internal val dependencyJarFingerprint: Provider<String>
        get() = project.provider {
            dependencyJarFiles.files
                .sortedBy { it.name }
                .joinToString("\n") { "${it.name}:${it.length()}" }
        }

    /**
     * Defines an object containing all the scalar/config inputs of this task.
     */
    @get:Nested
    internal val inputProperties = adapter.zip(getSvc()) { adp, svc ->
        BuildFrontendInputProperties(adp, svc)
    }

    /**
     * Defines an object containing all the outputs of this task.
     */
    @get:Nested
    internal val outputProperties =
        adapter.map { BuildFrontendOutputProperties(it) }

    init {
        group = "Vaadin"
        description = "Builds the frontend bundle with webpack"

        // we need the flow-build-info.json to be created, which is what the vaadinPrepareFrontend task does
        dependsOn("vaadinPrepareFrontend")
        // Maven's task run in the LifecyclePhase.PROCESS_CLASSES phase

        // We need access to the produced classes, to be able to analyze e.g.
        // @CssImport annotations used by the project.
        dependsOn("classes")

        // Make sure to run this task before the `war`/`jar` tasks, so that
        // webpack bundle will end up packaged in the war/jar archive. The inclusion
        // rule itself is configured in the VaadinPlugin class.
        project.tasks.withType(Jar::class.java) { task: Jar ->
            task.mustRunAfter("vaadinBuildFrontend")
        }
    }

    internal fun configure(config: PluginEffectiveConfiguration) {
        adapter.set(GradlePluginAdapter(this, config, false))

        // Track user-written frontend source files, excluding the
        // generated/ subdirectory which is modified by this task.
        frontendSourceFiles.from(
            config.effectiveFrontendDirectory.map { frontendDir ->
                project.fileTree(frontendDir) {
                    it.exclude("generated/**")
                }
            }
        )

        // Set up classpath for incremental build tracking.
        // Project classes are tracked with @Classpath (content-based) since
        // they are small and change frequently.
        val sourceSetName = config.sourceSetName.get()
        projectClassesDirs.from(
            project.getSourceSet(sourceSetName).output.classesDirs
        )

        // Dependency JARs are tracked with a lightweight fingerprint
        // (name + size) to avoid the memory cost of content-hashing
        // hundreds of JARs on large classpaths.
        val dependencyConfiguration =
            project.configurations.getByName(config.dependencyScope.get())
        dependencyJarFiles.from(
            dependencyConfiguration.incoming.files.filter {
                it.name.endsWith(".jar", true)
            }
        )
    }

    @TaskAction
    public fun vaadinBuildFrontend() {
        val config = adapter.get().config
        logger.info("Running the vaadinBuildFrontend task with effective configuration $config")
        val tokenFile = BuildFrontendUtil.getTokenFile(adapter.get())
        if (!tokenFile.exists()) {
            // if prepare-frontend token file doesn't exist, propagate build info
            // to token file
            logger.info("Token file does not exist, propagating build info")
            BuildFrontendUtil.propagateBuildInfo(adapter.get())
        }

        val options = Options(null, adapter.get().classFinder, config.npmFolder.get())
            .withFrontendDirectory(BuildFrontendUtil.getFrontendDirectory(adapter.get()))
            .withFrontendGeneratedFolder(config.generatedTsFolder.get())
        val cleanTask = TaskCleanFrontendFiles(options)

        val reactEnabled: Boolean = adapter.get().isReactEnabled()
                && FrontendUtils.isReactRouterRequired(
            BuildFrontendUtil.getFrontendDirectory(adapter.get())
        )
        val featureFlags: FeatureFlags = FeatureFlags(
            adapter.get().createLookup(adapter.get().getClassFinder())
        )
        if (adapter.get().javaResourceFolder() != null) {
            featureFlags.setPropertiesLocation(adapter.get().javaResourceFolder())
        }
        val frontendDependencies: FrontendDependenciesScanner = FrontendDependenciesScannerFactory()
            .createScanner(
                !adapter.get().optimizeBundle(),  adapter.get().getClassFinder(),
                adapter.get().generateEmbeddableWebComponents(), featureFlags,
                reactEnabled
            )

        BuildFrontendUtil.runNodeUpdater(adapter.get(), frontendDependencies)

        if (adapter.get().generateBundle() && BundleValidationUtil.needsBundleBuild
                (adapter.get().servletResourceOutputDirectory())) {
            BuildFrontendUtil.runFrontendBuild(adapter.get())
            if (cleanFrontendFiles()) {
                cleanTask.execute()
            }
        }
        LicenseChecker.setStrictOffline(true)
        val (licenseRequired: Boolean, commercialBannerRequired: Boolean) = try {
            Pair(
                BuildFrontendUtil.validateLicenses(
                    adapter.get(),
                    frontendDependencies
                ), false
            )
        } catch (e: MissingLicenseKeyException) {
            logger.info(e.message)
            Pair(true, true)
        }

        BuildFrontendUtil.updateBuildFile(adapter.get(), licenseRequired, commercialBannerRequired
        )

        // Write marker file for Gradle up-to-date tracking
        val markerFile = java.io.File(config.resourceOutputDirectory.get(), "build-frontend.marker")
        markerFile.parentFile.mkdirs()
        markerFile.writeText("Build completed at ${System.currentTimeMillis()}")
    }


    /**
     * Define if frontend files generated by bundle build should be cleaned or
     * not.
     *
     * The targeted frontend files are files that do not exist when
     * build-frontend target is executed.
     *
     * Extending mojo can override this method to return false so that any
     * frontend files created for the bundle build are not removed.
     *
     * @return `true` to remove created files, `false` to keep the files
     */
    protected open fun cleanFrontendFiles(): Boolean {
        if (FrontendBuildUtils.isHillaUsed(BuildFrontendUtil.getFrontendDirectory(adapter.get()),
                        adapter.get().classFinder)) {
            /*
             * Override this to not clean generated frontend files after the
             * build. For Hilla, the generated files can still be useful for
             * developers after the build. For example, a developer can use
             * {@code vite.generated.ts} to run tests with vitest in CI.
             */
            return false
        }
        return adapter.get().config.cleanFrontendFiles.get()
    }
}
