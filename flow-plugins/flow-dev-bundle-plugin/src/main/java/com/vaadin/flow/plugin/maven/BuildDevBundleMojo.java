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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.plugin.base.BuildFrontendUtil;
import com.vaadin.flow.plugin.base.PluginAdapterBase;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.installer.Platform;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.plugin.base.PluginAdapterBuild;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.theme.Theme;

/**
 * Goal that builds the dev frontend bundle to be used in Express Build mode.
 *
 * It performs the following actions when creating a package:
 * <ul>
 * <li>Update {@link Constants#PACKAGE_JSON} file with the {@link NpmPackage}
 * annotations defined in the classpath,</li>
 * <li>Copy resource files used by flow from `.jar` files to the `node_modules`
 * folder</li>
 * <li>Install dependencies by running <code>npm install</code></li>
 * <li>Update the {@link FrontendUtils#IMPORTS_NAME} file imports with the
 * {@link JsModule} {@link Theme} and {@link JavaScript} annotations defined in
 * the classpath,</li>
 * <li>Update {@link FrontendUtils#VITE_CONFIG} file.</li>
 * </ul>
 *
 * @since 2.0
 */
@Mojo(name = "build-dev-bundle", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.PROCESS_CLASSES)
public class BuildDevBundleMojo extends AbstractMojo
        implements PluginAdapterBuild, PluginAdapterBase {

    /**
     * Whether to generate embeddable web components from WebComponentExporter
     * inheritors.
     */
    @Parameter(defaultValue = "true")
    private boolean generateEmbeddableWebComponents;

    /**
     * Additionally include compile-time-only dependencies matching the pattern.
     */
    public static final String INCLUDE_FROM_COMPILE_DEPS_REGEX = ".*(/|\\\\)(portlet-api|javax\\.servlet-api)-.+jar$";

    /**
     * Whether or not insert the initial Uidl object in the bootstrap index.html
     */
    @Parameter(defaultValue = "${vaadin."
            + InitParameters.SERVLET_PARAMETER_INITIAL_UIDL + "}")
    private boolean eagerServerLoad;

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
     * installed 'node'.
     */
    @Parameter(property = InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, defaultValue = ""
            + Constants.DEFAULT_REQUIRE_HOME_NODE_EXECUTABLE)
    private boolean requireHomeNodeExec;

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

    @Override
    public void execute() throws MojoFailureException {
        long start = System.nanoTime();

        try {
            BuildFrontendUtil.runDevBuildNodeUpdater(this);
            BuildFrontendUtil.removeBuildFile(this);
        } catch (ExecutionFailedException | IOException
                | URISyntaxException exception) {
            throw new MojoFailureException(
                    "Could not execute build-dev-bundle goal", exception);
        }

        long ms = (System.nanoTime() - start) / 1000000;
        getLog().info("Dev-bundle build completed in " + ms + " ms.");
    }

    @Override
    public File frontendResourcesDirectory() {
        return new File(projectBasedir,
                Constants.LOCAL_FRONTEND_RESOURCES_PATH);
    }

    @Override
    public boolean generateBundle() {
        return true;
    }

    @Override
    public boolean generateEmbeddableWebComponents() {

        return generateEmbeddableWebComponents;
    }

    @Override
    public boolean optimizeBundle() {
        return false;
    }

    @Override
    public boolean runNpmInstall() {
        return true;
    }

    @Override
    public boolean ciBuild() {
        return false; // ci build not applicable for dev mode
    }

    @Override
    public boolean forceProductionBuild() {
        return false; // not applicable for dev bundle generation.
    }

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
        return new File(projectBasedir,
                "/src/main/resources/application.properties");
    }

    @Override
    public boolean eagerServerLoad() {

        return eagerServerLoad;
    }

    @Override
    public File frontendDirectory() {
        return new File(projectBasedir, FRONTEND);
    }

    @Override
    public File generatedTsFolder() {
        return new File(projectBasedir, FRONTEND + "/generated");
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
        return new File(projectBasedir, "/src/main/java");
    }

    @Override
    public File javaResourceFolder() {
        return new File(projectBasedir, "/src/main/resources");
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
        return projectBasedir;
    }

    @Override
    public File openApiJsonFile() {
        return new File(projectBuildDir, "/generated-resources/openapi.json");
    }

    @Override
    public boolean pnpmEnable() {
        return false;
    }

    @Override
    public boolean useGlobalPnpm() {
        return false;
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
        return new File(project.getBuild().getOutputDirectory(),
                VAADIN_SERVLET_RESOURCES);
    }

    @Override
    public File webpackOutputDirectory() {
        return new File(project.getBuild().getOutputDirectory(),
                VAADIN_WEBAPP_RESOURCES);
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
        return false;
    }

    @Override
    public boolean skipDevBundleBuild() {
        // Explicitly building dev bundle so no skipping allowed here.
        return false;
    }

    @Override
    public boolean isPrepareFrontendCacheDisabled() {
        return false;
    }
}
