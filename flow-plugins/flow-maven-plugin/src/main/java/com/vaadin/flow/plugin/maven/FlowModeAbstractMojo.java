/*
 * Copyright 2000-2023 Vaadin Ltd.
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

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBase;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

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
    private File applicationProperties;

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html
     */
    @Parameter(defaultValue = "${vaadin."
            + InitParameters.SERVLET_PARAMETER_INITIAL_UIDL + "}")
    private boolean eagerServerLoad;

    /**
     * A directory with project's frontend source files.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FRONTEND)
    private File frontendDirectory;

    /**
     * The folder where flow will put TS API files for client projects.
     */
    @Parameter(defaultValue = "${project.basedir}/" + FRONTEND + "/generated")
    private File generatedTsFolder;

    /**
     * Java source folders for scanning.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java")
    private File javaSourceFolder;

    /**
     * Java resource folder.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources")
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
    @Parameter(property = InitParameters.NODE_DOWNLOAD_ROOT)
    private String nodeDownloadRoot;

    /**
     * The node.js version to be used when node.js is installed automatically by
     * Vaadin, for example `"v16.0.0"`. Defaults to null which uses the
     * Vaadin-default node version - see {@link FrontendTools} for details.
     */
    @Parameter(property = InitParameters.NODE_VERSION, defaultValue = FrontendTools.DEFAULT_NODE_VERSION)
    private String nodeVersion;

    /**
     * Setting defining if the automatically installed node version may be
     * updated to the default Vaadin node version.
     */
    @Parameter(property = InitParameters.NODE_AUTO_UPDATE, defaultValue = ""
            + Constants.DEFAULT_NODE_AUTO_UPDATE)
    private boolean nodeAutoUpdate;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File npmFolder;

    /**
     * Default generated path of the OpenAPI json.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-resources/openapi.json")
    private File openApiJsonFile;

    /**
     * Instructs to use pnpm for installing npm frontend resources.
     */
    @Parameter(property = InitParameters.SERVLET_PARAMETER_ENABLE_PNPM, defaultValue = ""
            + Constants.ENABLE_PNPM_DEFAULT)
    private boolean pnpmEnable;

    /**
     * Instructs to use globally installed pnpm tool or the default supported
     * pnpm version.
     */
    @Parameter(property = InitParameters.SERVLET_PARAMETER_GLOBAL_PNPM, defaultValue = ""
            + Constants.GLOBAL_PNPM_DEFAULT)
    private boolean useGlobalPnpm;

    /**
     * Whether or not we are running in productionMode.
     */
    @Parameter(defaultValue = "${null}")
    protected Boolean productionMode;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    /**
     * The folder where `package.json` file is located. Default is project root
     * dir.
     */
    @Parameter(defaultValue = "${project.basedir}")
    private File projectBasedir;

    /**
     * Whether vaadin home node executable usage is forced. If it's set to
     * {@code true} then vaadin home 'node' is checked and installed if it's
     * absent. Then it will be used instead of globally 'node' or locally
     * installed installed 'node'.
     */
    @Parameter(property = InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, defaultValue = ""
            + Constants.DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE)
    private boolean requireHomeNodeExec;

    /**
     * Defines the output directory for generated non-served resources, such as
     * the token file.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_SERVLET_RESOURCES)
    private File resourceOutputDirectory;

    /**
     * The folder where the frontend build tool should output index.js and other
     * generated files.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/"
            + VAADIN_WEBAPP_RESOURCES)
    private File webpackOutputDirectory;

    /**
     * Build directory for the project.
     */
    @Parameter(property = "build.folder", defaultValue = "${project.build.directory}")
    private String projectBuildDir;

    /**
     * Additional npm packages to run post install scripts for.
     * <p>
     * Post install is automatically run for internal dependencies which rely on
     * post install scripts to work, e.g. esbuild.
     */
    @Parameter(property = "npm.postinstallPackages", defaultValue = "")
    private List<String> postinstallPackages;

    /**
     * Parameter to control if frontend development server should be used in
     * development mode or not.
     * <p>
     * By default, the frontend server is not used.
     */
    @Parameter(property = InitParameters.FRONTEND_HOTDEPLOY, defaultValue = "false")
    private boolean frontendHotdeploy;

    @Parameter(property = InitParameters.SKIP_DEV_BUNDLE_REBUILD, defaultValue = "false")
    private boolean skipDevBundleRebuild;

    /**
     * Generates a List of ClasspathElements (Run and CompileTime) from a
     * MavenProject.
     *
     * @param project
     *            a given MavenProject
     * @return List of ClasspathElements
     */
    public static List<String> getClasspathElements(MavenProject project) {

        try {
            final Stream<String> classpathElements = Stream.concat(
                    project.getRuntimeClasspathElements().stream(),
                    project.getCompileClasspathElements().stream().filter(
                            s -> s.matches(INCLUDE_FROM_COMPILE_DEPS_REGEX)));
            return classpathElements.collect(Collectors.toList());
        } catch (DependencyResolutionRequiredException e) {
            throw new IllegalStateException(String.format(
                    "Failed to retrieve runtime classpath elements from project '%s'",
                    project), e);
        }
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

        return generatedTsFolder;
    }

    @Override
    public ClassFinder getClassFinder() {

        List<String> classpathElements = getClasspathElements(project);

        return BuildFrontendUtil.getClassFinder(classpathElements);

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
    public void logInfo(CharSequence infoMessage) {

        getLog().info(infoMessage);

    }

    @Override
    public void logWarn(CharSequence warning) {

        getLog().warn(warning);
    }

    @Override
    public void logWarn(CharSequence warning, Throwable e) {

        getLog().warn(warning, e);

    }

    @Override
    public void logError(CharSequence warning, Throwable e) {

        getLog().error(warning, e);

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
        return frontendHotdeploy;
    }

    @Override
    public boolean skipDevBundleBuild() {
        return skipDevBundleRebuild;
    }

    @Override
    public boolean isPrepareFrontendCacheDisabled() {
        return false;
    }
}
