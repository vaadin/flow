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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.internal.StringUtil;
import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBase;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;

import static com.vaadin.flow.server.frontend.FrontendUtils.GENERATED;

/**
 * The base class of Flow plugin tasks.
 * <p>
 * </p>
 * Subclasses that requires additional constructor arguments must specify them
 * after the base class parameters. The task class must duplicate all the
 * {@code @Parameter} annotated fields from the companion Mojo.
 *
 * @since 24.6
 */
public abstract class FlowModeAbstractTask implements PluginAdapterBase, Mojo {

    /**
     * Additionally include compile-time-only dependencies matching the pattern.
     */
    public static final String INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";

    /**
     * Application properties file in Spring project.
     */
    private File applicationProperties;

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html
     */
    private boolean eagerServerLoad;

    /**
     * A directory with project's frontend source files.
     */
    private File frontendDirectory;

    /**
     * The folder where flow will put TS API files for client projects.
     */
    private File generatedTsFolder;

    /**
     * Java source folders for scanning.
     */
    private File javaSourceFolder;

    /**
     * Java resource folder.
     */
    private File javaResourceFolder;

    /**
     * Download node.js from this URL. Handy in heavily firewalled corporate
     * environments where the node.js download can be provided from an intranet
     * mirror. Defaults to null which will cause the downloader to use
     * {@link NodeInstaller#DEFAULT_NODEJS_DOWNLOAD_ROOT}.
     * <p>
     * </p>
     * Example: <code>"https://nodejs.org/dist/"</code>.
     */
    private String nodeDownloadRoot;

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v16.0.0"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     */
    private String nodeVersion;

    /**
     * Setting defining if the automatically installed node version may be
     * updated to the default Vaadin node version.
     */
    private boolean nodeAutoUpdate;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    private File npmFolder;

    /**
     * Default generated path of the OpenAPI json.
     */
    private File openApiJsonFile;

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     */
    private boolean pnpmEnable;

    /**
     * Instructs to use bun for installing npm frontend resources.
     */
    private boolean bunEnable;

    /**
     * Instructs to use globally installed pnpm tool or the default supported
     * pnpm version.
     */
    private boolean useGlobalPnpm;

    /**
     * Whether or not we are running in productionMode.
     */
    protected Boolean productionMode;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    private File projectBasedir;

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * {@code true} then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    private boolean requireHomeNodeExec;

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file.
     */
    private File resourceOutputDirectory;

    /**
     * The folder where the frontend build tool should output index.js and other
     * generated files.
     */
    private File webpackOutputDirectory;

    /**
     * Build directory for the project.
     */
    private String projectBuildDir;

    /**
     * Additional npm packages to run post install scripts for.
     * <p>
     * Post install is automatically run for internal dependencies which rely on
     * post install scripts to work, e.g. esbuild.
     */
    private List<String> postinstallPackages;

    /**
     * Parameter to control if frontend development server should be used in
     * development mode or not.
     * <p>
     * By default, the frontend server is not used.
     */
    private Boolean frontendHotdeploy;

    private boolean skipDevBundleRebuild;

    private Boolean reactEnable;

    private boolean npmExcludeWebComponents;

    /**
     * Parameter for adding file extensions to handle during frontend tasks.
     * <p>
     * From the commandline use comma separated list
     * {@code -Ddevmode.frontendExtraFileExtensions="svg,ico"}
     * <p>
     * In plugin configuration use comma separated values
     *
     * <configuration>
     * <frontendExtraFileExtensions>svg,ico</frontendExtraFileExtensions>
     * </configuration>
     *
     */
    private List<String> frontendExtraFileExtensions;

    /**
     * Identifier for the application.
     * <p>
     *
     */
    private String applicationIdentifier;

    /**
     * Default Identifier for the application, computed based on current project
     * as 'app-{@literal groupId:artifactId}'.
     */
    private String defaultApplicationIdentifier;

    private final ClassFinder classFinder;
    private final Log logger;
    private final MavenProject project;

    protected FlowModeAbstractTask(MavenProject project,
            ClassFinder classFinder, Log logger) {
        this.project = project;
        this.classFinder = classFinder;
        this.logger = logger;
        this.defaultApplicationIdentifier = "app-" + StringUtil.getHash(
                project.getGroupId() + ":" + project.getArtifactId(),
                StandardCharsets.UTF_8);
    }

    protected MavenProject getProject() {
        return project;
    }

    public abstract void execute()
            throws MojoFailureException, MojoExecutionException;

    @Override
    public void setLog(Log log) {
        throw new UnsupportedOperationException("Logger cannot be changed");
    }

    @Override
    public Log getLog() {
        return logger;
    }

    /**
     * Checks if Hilla is available based on the Maven project's classpath.
     *
     * @return true if Hilla is available, false otherwise
     */
    public boolean isHillaAvailable() {
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
        return classFinder;
    }

    @Override
    public Set<File> getJarFiles() {
        return project.getArtifacts().stream()
                .filter(artifact -> "jar".equals(artifact.getType()))
                .map(Artifact::getFile).collect(Collectors.toSet());
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
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
        logger.debug(debugMessage);
    }

    @Override
    public void logDebug(CharSequence debugMessage, Throwable e) {
        logger.debug(debugMessage, e);
    }

    @Override
    public void logInfo(CharSequence infoMessage) {
        logger.info(infoMessage);
    }

    @Override
    public void logWarn(CharSequence warning) {
        logger.warn(warning);
    }

    @Override
    public void logError(CharSequence error) {
        logger.error(error);
    }

    @Override
    public void logWarn(CharSequence warning, Throwable e) {
        logger.warn(warning, e);
    }

    @Override
    public void logError(CharSequence error, Throwable e) {
        logger.error(error, e);
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
        File frontendDirectory = BuildFrontendUtil.getFrontendDirectory(this);
        return isHillaUsed(frontendDirectory);
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
        File frontendDirectory = BuildFrontendUtil.getFrontendDirectory(this);
        return FrontendUtils.isReactRouterRequired(frontendDirectory);
    }

    @Override
    public String applicationIdentifier() {
        if (applicationIdentifier != null && !applicationIdentifier.isBlank()) {
            return applicationIdentifier;
        }
        return defaultApplicationIdentifier;
    }

    @Override
    public List<String> frontendExtraFileExtensions() {
        if (frontendExtraFileExtensions != null) {
            return frontendExtraFileExtensions;
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isNpmExcludeWebComponents() {
        return npmExcludeWebComponents;
    }

}
