/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import javax.inject.Inject;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.build.BuildContext;

import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBase;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;
import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo
        implements PluginAdapterBase {

    /**
     * Additionally include compile-time-only dependencies matching the pattern.
     */
    public static final String INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";

    /**
     * Application properties file in Spring project.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/application.properties")
    protected File applicationProperties;

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html
     */
    @Parameter(defaultValue = "${vaadin."
            + InitParameters.SERVLET_PARAMETER_INITIAL_UIDL + "}")
    protected boolean eagerServerLoad;

    /**
     * A directory with project's frontend source files.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/" + FRONTEND)
    protected File frontendDirectory;

    /**
     * The folder where flow will put TS API files for client projects.
     */
    @Parameter(defaultValue = "${null}")
    protected File generatedTsFolder;

    /**
     * Java source folders for scanning.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    protected File javaSourceFolder;

    /**
     * Java resource folder.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
    protected File javaResourceFolder;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     * <p>
     * </p>
     * Example: <code>"https://nodejs.org/dist/"</code>.
     */
    @Parameter(property = InitParameters.NODE_DOWNLOAD_ROOT)
    protected String nodeDownloadRoot;

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v16.0.0"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     */
    @Parameter(property = InitParameters.NODE_VERSION, defaultValue = FrontendTools.DEFAULT_NODE_VERSION)
    protected String nodeVersion;

    /**
     * Setting defining if the automatically installed node version may be
     * updated to the default Vaadin node version.
     */
    @Parameter(property = InitParameters.NODE_AUTO_UPDATE, defaultValue = ""
            + Constants.DEFAULT_NODE_AUTO_UPDATE)
    protected boolean nodeAutoUpdate;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File npmFolder;

    /**
     * Default generated path of the OpenAPI json.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/openapi.json")
    protected File openApiJsonFile;

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     */
    @Parameter(property = InitParameters.SERVLET_PARAMETER_ENABLE_PNPM, defaultValue = ""
            + Constants.ENABLE_PNPM_DEFAULT)
    protected boolean pnpmEnable;

    /**
     * Instructs to use bun for installing npm frontend resources.
     */
    @Parameter(property = InitParameters.SERVLET_PARAMETER_ENABLE_BUN, defaultValue = ""
            + Constants.ENABLE_BUN_DEFAULT)
    protected boolean bunEnable;

    /**
     * Instructs to use globally installed pnpm tool or the default supported
     * pnpm version.
     */
    @Parameter(property = InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM, defaultValue = ""
            + Constants.GLOBAL_PNPM_DEFAULT)
    protected boolean useGlobalPnpm;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    @Parameter(defaultValue = "${mojoExecution}")
    MojoExecution mojoExecution;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    protected File projectBasedir;

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * {@code true} then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    @Parameter(property = InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, defaultValue = ""
            + Constants.DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE)
    protected boolean requireHomeNodeExec;

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_SERVLET_RESOURCES)
    protected File resourceOutputDirectory;

    /**
     * The folder where the frontend build tool should output index.js and other
     * generated files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_WEBAPP_RESOURCES)
    protected File webpackOutputDirectory;

    /**
     * Build directory for the project.
     */
    @Parameter(property = "build.folder", defaultValue = "${project.build.directory}")
    protected String projectBuildDir;

    /**
     * Additional npm packages to run post install scripts for.
     * <p>
     * Post install is automatically run for internal dependencies which rely on
     * post install scripts to work, e.g. esbuild.
     */
    @Parameter(property = "npm.postinstallPackages", defaultValue = "")
    protected List<String> postinstallPackages;

    /**
     * Parameter to control if frontend development server should be used in
     * development mode or not.
     * <p>
     * By default, the frontend server is not used.
     */
    @Parameter(property = InitParameters.FRONTEND_HOTDEPLOY, defaultValue = "${null}")
    protected Boolean frontendHotdeploy;

    @Parameter(property = InitParameters.SKIP_DEV_BUNDLE_REBUILD, defaultValue = "false")
    protected boolean skipDevBundleRebuild;

    @Parameter(property = InitParameters.REACT_ENABLE, defaultValue = "${null}")
    protected Boolean reactEnable;

    @Parameter(property = InitParameters.NPM_EXCLUDE_WEB_COMPONENTS, defaultValue = "false")
    protected boolean npmExcludeWebComponents;

    /**
     * Parameter for adding file extensions to handle when generating bundles.
     * Hashes are calculated for these files as part of detecting if a new
     * bundle should be generated.
     * <p>
     * From the commandline use comma separated list
     * {@code -Ddevmode.frontendExtraFileExtensions="svg,ico"}
     * <p>
     * In plugin configuration use comma separated values
     *
     * <configuration>
     * <frontendExtraFileExtensions>svg,ico</frontendExtraFileExtensions>
     * </configuration>
     */
    @Parameter(property = InitParameters.FRONTEND_EXTRA_EXTENSIONS, defaultValue = "${null}")
    protected List<String> frontendExtraFileExtensions;

    /**
     * Identifier for the application.
     * <p>
     * If not specified, defaults to '{@literal groupId:artifactId}'.
     */
    @Parameter(property = InitParameters.APPLICATION_IDENTIFIER)
    protected String applicationIdentifier;

    /**
     * If set to <code>false</code> the version of frontend executables like Node, NPM, ... will not be checked.
     * <p>
     * Usually the checks can be ignored and are only required after a Vaadin version update.
     * </p>
     */
    @Parameter(defaultValue = "${null}")
    protected Boolean frontendIgnoreVersionChecks;

    @Parameter
    protected FastReflectorIsolationConfig fastReflectorIsolation;

    /**
     * If this is set to a non-null value, the plugin will not use classpath-scanning to detect
     * if Hilla is present or not.
     */
    @Parameter(defaultValue = "${null}")
    protected Boolean hillaAvailable;

    /**
     * If this is set to <code>false</code> the compatibility between the Flow dependencies
     * of the project and the plugin will not be checked.
     * <p>
     * Usually the check is only required after a Vaadin version update.
     * </p>
     */
    @Parameter(defaultValue = "true")
    protected boolean checkPluginFlowCompatibility;

    /**
     * Should support for
     * <a href="https://vaadin.com/docs/latest/flow/configuration/licenses/daily-active-users">Vaadin
     * DAU</a> be enabled.
     * <p>
     * This includes:
     *     <ul>
     *         <li>Shipping the application name with the flow-build-info.json, hashed as SHA256</li>
     *     </ul>
     * </p>
     */
    @Parameter(defaultValue = "true")
    protected boolean supportDAU;

    static final String CLASSFINDER_FIELD_NAME = "classFinder";
    protected ClassFinder classFinder;

    protected ReflectorController getNewReflectorController() {
        return new DefaultReflectorController(fastReflectorIsolation, getLog());
    }

    /**
     * Field names specified here will not be copied over to the isolated mojo.
     */
    protected Set<String> isolatedMojoIgnoreFields() {
        return Set.of("fastReflectorIsolation");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final String goal = mojoExecution.getMojoDescriptor().getGoal();
        getLog().info("Running " + goal);
        final long start = System.nanoTime();

        applyFrontendIgnoreVersionChecks();

        checkFlowCompatibility();

        final long prepareIsolatedStart = System.nanoTime();

        Reflector reflector = getOrCreateReflector(getNewReflectorController());
        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(reflector.getIsolatedClassLoader());
        try {
            Mojo task = reflector.createIsolatedMojo(this, isolatedMojoIgnoreFields());
            Method mExec = ReflectTools.findMethodAndMakeAccessible(task.getClass(), "executeInternal");

            getLog().info("Preparations for isolated execution finished, took "
                    + msSince(prepareIsolatedStart) + " ms");

            final long isolatedStart = System.nanoTime();
            mExec.invoke(task);

            getLog().info("Isolated execution finished, took " + msSince(isolatedStart) + " ms");
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }

        getLog().info(goal + " finished, took " + msSince(start) + " ms");
    }

    /**
     * Perform whatever build-process behavior this <code>Mojo</code>
     * implements.<br>
     * This is the main trigger for the <code>Mojo</code> inside the
     * <code>Maven</code> system, and allows the <code>Mojo</code> to
     * communicate errors.
     *
     * @throws MojoExecutionException
     *             if an unexpected problem occurs. Throwing this exception
     *             causes a "BUILD ERROR" message to be displayed.
     * @throws MojoFailureException
     *             if an expected problem (such as a compilation failure)
     *             occurs. Throwing this exception causes a "BUILD FAILURE"
     *             message to be displayed.
     */
    protected abstract void executeInternal()
            throws MojoExecutionException, MojoFailureException;

    /**
     * Generates a List of ClasspathElements (Run and CompileTime) from a
     * MavenProject.
     *
     * @param project
     *            a given MavenProject
     * @return List of ClasspathElements
     * @deprecated will be removed without replacement.
     */
    @Deprecated(forRemoval = true)
    public static List<String> getClasspathElements(MavenProject project) {
        try {
            final Stream<String> classpathElements = Stream
                    .of(project.getRuntimeClasspathElements().stream(),
                            project.getSystemClasspathElements().stream(),
                            project.getCompileClasspathElements().stream()
                                    .filter(s -> s.matches(
                                            INCLUDE_FROM_COMPILE_DEPS_REGEX)))
                    .flatMap(Function.identity());
            return classpathElements.collect(Collectors.toList());
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
    }

    /**
     * Checks if Hilla is available based on the Maven project's classpath.
     *
     * @return true if Hilla is available, false otherwise
     */
    public boolean isHillaAvailable() {
        if (hillaAvailable != null) {
            return hillaAvailable;
        }

        return getClassFinder().getResource(
                "com/vaadin/hilla/EndpointController.class") != null;
    }

    /**
     * Checks if Hilla is available and Hilla views are used in the Maven
     * project based on what is in routes.ts or routes.tsx file.
     *
     * @param frontendDirectory
     *            Target frontend directory.
     * @return {@code true} if Hilla is available and Hilla views are used,
     *         {@code false} otherwise
     */
    public boolean isHillaUsed(File frontendDirectory) {
        return isHillaAvailable()
                && FrontendUtils.isHillaViewsUsed(frontendDirectory);
    }

    @Override
    public File applicationProperties() {
        return applicationProperties;
    }

    @Override
    public boolean eagerServerLoad() {
        return eagerServerLoad;
    }

    @Override
    public File frontendDirectory() {
        return frontendDirectory;
    }

    @Override
    public File generatedTsFolder() {
        if (generatedTsFolder != null) {
            return generatedTsFolder;
        }
        return new File(frontendDirectory(), GENERATED);
    }

    @Override
    public ClassFinder getClassFinder() {
        return Objects.requireNonNull(classFinder, "ClassFinder is null. Ensure that you are in the isolated Mojo");
    }

    @Override
    public Set<File> getJarFiles() {
        return project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .map(Artifact::getFile).collect(Collectors.toSet());
    }

    @Override
    public boolean isDebugEnabled() {

        return getLog().isDebugEnabled();
    }

    @Override
    public File javaSourceFolder() {
        return javaSourceFolder;
    }

    @Override
    public File javaResourceFolder() {
        return javaResourceFolder;
    }

    @Override
    public void logDebug(CharSequence debugMessage) {
        getLog().debug(debugMessage);
    }

    @Override
    public void logDebug(CharSequence debugMessage, Throwable e) {
        getLog().debug(debugMessage, e);
    }

    @Override
    public void logInfo(CharSequence infoMessage) {
        getLog().info(infoMessage);
    }

    @Override
    public void logWarn(CharSequence warning) {

        getLog().warn(warning);
    }

    @Override
    public void logError(CharSequence error) {
        getLog().error(error);
    }

    @Override
    public void logWarn(CharSequence warning, Throwable e) {
        getLog().warn(warning, e);
    }

    @Override
    public void logError(CharSequence error, Throwable e) {
        getLog().error(error, e);
    }

    @Override
    public URI nodeDownloadRoot() throws URISyntaxException {
        if (nodeDownloadRoot == null) {
            nodeDownloadRoot = Platform.guess().getNodeDownloadRoot();
        }
        try {
            return new URI(nodeDownloadRoot);
        } catch (URISyntaxException e) {
            logError("Failed to parse nodeDownloadRoot uri", e);
            throw new URISyntaxException(nodeDownloadRoot,
                    "Failed to parse nodeDownloadRoot uri");
        }
    }

    @Override
    public boolean nodeAutoUpdate() {
        return nodeAutoUpdate;
    }

    @Override
    public String nodeVersion() {
        return nodeVersion;
    }

    @Override
    public File npmFolder() {
        return npmFolder;
    }

    @Override
    public File openApiJsonFile() {
        return openApiJsonFile;
    }

    @Override
    public boolean pnpmEnable() {
        return pnpmEnable;
    }

    @Override
    public boolean bunEnable() {
        return bunEnable;
    }

    @Override
    public boolean useGlobalPnpm() {
        return useGlobalPnpm;
    }

    @Override
    public Path projectBaseDirectory() {
        return projectBasedir.toPath();
    }

    @Override
    public boolean requireHomeNodeExec() {
        return requireHomeNodeExec;
    }

    @Override
    public File servletResourceOutputDirectory() {
        return resourceOutputDirectory;
    }

    @Override
    public File webpackOutputDirectory() {
        return webpackOutputDirectory;
    }

    @Override
    public boolean isJarProject() {
        return "jar".equals(project.getPackaging());
    }

    @Override
    public String buildFolder() {
        if (projectBuildDir.startsWith(projectBasedir.toString())) {
            return projectBaseDirectory().relativize(Paths.get(projectBuildDir))
                    .toString();
        }
        return projectBuildDir;
    }

    @Override
    public List<String> postinstallPackages() {
        return postinstallPackages;
    }

    @Override
    public boolean isFrontendHotdeploy() {
        if (frontendHotdeploy != null) {
            return frontendHotdeploy;
        }
        return isHillaUsed(BuildFrontendUtil.getFrontendDirectory(this));
    }

    @Override
    public boolean skipDevBundleBuild() {
        return skipDevBundleRebuild;
    }

    @Override
    public boolean isPrepareFrontendCacheDisabled() {
        return false;
    }

    @Override
    public boolean isReactEnabled() {
        if (reactEnable != null) {
            return reactEnable;
        }
        return FrontendUtils.isReactRouterRequired(BuildFrontendUtil.getFrontendDirectory(this));
    }

    @Override
    public String applicationIdentifier() {
        if (applicationIdentifier != null && !applicationIdentifier.isBlank()) {
            return applicationIdentifier;
        }
        return supportDAU
                ? "app-" + StringUtil.getHash(
                project.getGroupId() + ":" + project.getArtifactId(),
                StandardCharsets.UTF_8)
                : "-";
    }

    @Override
    public List<String> frontendExtraFileExtensions() {
        return Objects.requireNonNullElse(frontendExtraFileExtensions, Collections.emptyList());
    }

    @Override
    public boolean isNpmExcludeWebComponents() {
        return npmExcludeWebComponents;
    }

    protected void checkFlowCompatibility() {
        if (!checkPluginFlowCompatibility) {
            getLog().info("Vaadin flow compatibility between plugin and project is not checked");
            return;
        }

        Predicate<Artifact> isFlowServer = artifact -> "com.vaadin".equals(artifact.getGroupId())
                && "flow-server".equals(artifact.getArtifactId());
        String projectFlowVersion = project.getArtifacts().stream()
                .filter(isFlowServer)
                .map(Artifact::getBaseVersion)
                .findFirst()
                .orElse(null);
        String pluginFlowVersion = this.mojoExecution.getMojoDescriptor()
                .getPluginDescriptor()
                .getArtifacts()
                .stream()
                .filter(isFlowServer)
                .map(Artifact::getBaseVersion)
                .findFirst()
                .orElse(null);
        if (projectFlowVersion != null
                && !Objects.equals(projectFlowVersion, pluginFlowVersion)) {
            getLog().warn(
                    "Vaadin Flow used in project does not match the version expected by the Vaadin plugin. "
                            + "Flow version for project is "
                            + projectFlowVersion
                            + ", Vaadin plugin is built for Flow version "
                            + pluginFlowVersion + ".");
        }
    }

    protected void applyFrontendIgnoreVersionChecks() {
        Optional.ofNullable(this.frontendIgnoreVersionChecks)
                .ifPresent(ignore -> {
                    this.getLog().info("Set " + FrontendUtils.PARAM_IGNORE_VERSION_CHECKS + " to " + ignore);
                    System.setProperty(FrontendUtils.PARAM_IGNORE_VERSION_CHECKS, String.valueOf(ignore));
                });
    }

    protected Reflector getOrCreateReflector(final ReflectorController reflectorController) {
        final Map<String, Object> pluginContext = getPluginContext();
        final String pluginKey = mojoExecution.getPlugin().getKey();
        final String reflectorKey = reflectorController.getReflectorClassIdentifier() + "-" + pluginKey + "-"
                + mojoExecution.getLifecyclePhase();
        if (pluginContext != null && pluginContext.containsKey(reflectorKey)) {
            getLog().debug("Using cached Reflector for plugin " + pluginKey
                    + " and phase " + mojoExecution.getLifecyclePhase());
            try {
                final long start = System.nanoTime();

                final Reflector reused = reflectorController.adaptFrom(pluginContext.get(reflectorKey));

                getLog().info("Adapted from cached Reflector, took " + msSince(start) + "ms");

                return reused;
            } catch (final RuntimeException rex) {
                getLog().warn("Failed to reuse cached reflector", rex);
            }
        }

        final long start = System.nanoTime();

        final Reflector reflector = reflectorController.of(project, mojoExecution);
        getLog().info("Created new Reflector[urlsOnIsolatedClassLoader="
                + reflector.getIsolatedClassLoader().getURLs().length
                + "x], took " + msSince(start) + "ms");

        if (pluginContext != null) {
            pluginContext.put(reflectorKey, reflector);
            getLog().debug("Cached Reflector for plugin " + pluginKey
                    + " and phase " + mojoExecution.getLifecyclePhase());
        }
        return reflector;
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    protected static long msSince(final long startNanos) {
        return (System.nanoTime() - startNanos) / 1000000;
    }
}
