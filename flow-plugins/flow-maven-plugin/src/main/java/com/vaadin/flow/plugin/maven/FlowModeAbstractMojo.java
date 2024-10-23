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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.ContextEnabled;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendTools;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.scanner.ReflectionsClassFinder;
import com.vaadin.flow.utils.FlowFileUtils;

import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.frontend.FrontendUtils.FRONTEND;

/**
 * The base class of Flow Mojos in order to compute correctly the modes.
 *
 * @since 2.0
 */
public abstract class FlowModeAbstractMojo extends AbstractMojo {

    /**
     * Additionally include compile-time-only dependencies matching the pattern.
     *
     * @deprecated use {@link Reflector#INCLUDE_FROM_COMPILE_DEPS_REGEX}
     */
    @Deprecated(forRemoval = true)
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
    @Parameter(defaultValue = "${project.basedir}/src/main/" + FRONTEND)
    private File frontendDirectory;

    /**
     * The folder where flow will put TS API files for client projects.
     */
    @Parameter(defaultValue = "${null}")
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
     * Instructs to use bun for installing npm frontend resources.
     */
    @Parameter(property = InitParameters.SERVLET_PARAMETER_ENABLE_BUN, defaultValue = ""
            + Constants.ENABLE_BUN_DEFAULT)
    private boolean bunEnable;

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

    @Parameter(defaultValue = "${mojoExecution}")
    MojoExecution mojoExecution;

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
    @Parameter(property = InitParameters.FRONTEND_HOTDEPLOY, defaultValue = "${null}")
    private Boolean frontendHotdeploy;

    @Parameter(property = InitParameters.SKIP_DEV_BUNDLE_REBUILD, defaultValue = "false")
    private boolean skipDevBundleRebuild;

    @Parameter(property = InitParameters.REACT_ENABLE, defaultValue = "${null}")
    private Boolean reactEnable;

    @Parameter(property = InitParameters.NPM_EXCLUDE_WEB_COMPONENTS, defaultValue = "false")
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
    @Parameter(property = InitParameters.FRONTEND_EXTRA_EXTENSIONS, defaultValue = "${null}")
    private List<String> frontendExtraFileExtensions;

    /**
     * Identifier for the application.
     * <p>
     * If not specified, defaults to '{@literal groupId:artifactId}'.
     */
    @Parameter(property = InitParameters.APPLICATION_IDENTIFIER)
    private String applicationIdentifier;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        PluginDescriptor pluginDescriptor = mojoExecution.getMojoDescriptor()
                .getPluginDescriptor();
        checkFlowCompatibility(pluginDescriptor);

        Reflector reflector = getOrCreateReflector();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread()
                .setContextClassLoader(reflector.getTaskClassLoader());
        try {
            List<Class<?>> paramTypes = new ArrayList<>();
            List<Object> paramValues = new ArrayList<>();
            taskParameters(reflector, paramTypes, paramValues);

            Mojo task = reflector.createTask(this, paramTypes, paramValues);
            task.execute();
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    private Reflector getOrCreateReflector() {
        return getOrCreateReflector(this, project, mojoExecution);
    }

    private static Reflector getOrCreateReflector(Mojo mojo,
            MavenProject project, MojoExecution mojoExecution) {
        Map<String, Object> pluginContext = (mojo instanceof ContextEnabled ctx)
                ? ctx.getPluginContext()
                : null;
        String pluginKey = mojoExecution.getPlugin().getKey();
        String reflectorKey = Reflector.class.getName() + "-" + pluginKey + "-"
                + mojoExecution.getLifecyclePhase();
        if (pluginContext != null && pluginContext
                .get(reflectorKey) instanceof Reflector cachedReflector) {

            mojo.getLog().debug("Using cached Reflector for plugin " + pluginKey
                    + " and phase " + mojoExecution.getLifecyclePhase());
            return cachedReflector;
        }
        Reflector reflector = Reflector.of(project, mojoExecution);
        if (pluginContext != null) {
            pluginContext.put(reflectorKey, reflector);
            mojo.getLog().debug("Cached Reflector for plugin " + pluginKey
                    + " and phase " + mojoExecution.getLifecyclePhase());
        }
        return reflector;
    }

    protected Class<?> taskClass(Reflector reflector)
            throws ClassNotFoundException {
        String taskClassName = getClass().getName().replace("Mojo", "Task");
        return reflector.loadClass(taskClassName);
    }

    /**
     * Provides additional types and values to lookup and invoke task
     * constructor.
     * <p>
     * </p>
     * {@link FlowModeAbstractTask} subclasses defining a constructor with
     * additional parameters should fill the {@code paramTypes} and
     * {@code values} accordingly. Parameter and values for the base
     * {@link FlowModeAbstractTask(MavenProject, URLClassLoader, System.Logger)}
     * constructor are provided automatically and must not be added by this
     * method.
     *
     * @param reflector
     *            Reflector instance to load classes from the task classloader.
     * @param paramTypes
     *            mutable list of constructor parameter types.
     * @param values
     *            mutable list of constructor parameter values.
     * @throws ClassNotFoundException
     *             if a required class is not available in the task classloader.
     */
    protected void taskParameters(Reflector reflector,
            List<Class<?>> paramTypes, List<Object> values)
            throws ClassNotFoundException {
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
     * @param mavenProject
     *            Target Maven project
     * @return true if Hilla is available, false otherwise
     */
    public static boolean isHillaAvailable(MavenProject mavenProject) {
        return Reflector.of(mavenProject, null).getResource(
                // return createClassFinder(mavenProject).getResource(
                "com/vaadin/hilla/EndpointController.class") != null;
    }

    /**
     * Checks if Hilla is available and Hilla views are used in the Maven
     * project based on what is in routes.ts or routes.tsx file.
     *
     * @param mavenProject
     *            Target Maven project
     * @param frontendDirectory
     *            Target frontend directory.
     * @return {@code true} if Hilla is available and Hilla views are used,
     *         {@code false} otherwise
     */
    public static boolean isHillaUsed(MavenProject mavenProject,
            File frontendDirectory) {
        return isHillaAvailable(mavenProject)
                && FrontendUtils.isHillaViewsUsed(frontendDirectory);
    }

    private static ClassFinder createClassFinder(MavenProject project) {
        List<String> classpathElements = getClasspathElements(project);
        URL[] urls = classpathElements.stream().distinct().map(File::new)
                .map(FlowFileUtils::convertToUrl).toArray(URL[]::new);

        // A custom class loader that reverts the order for resources lookup
        // by first searching self and then delegating to the parent.
        // This hack is required to prevent resources being loaded from the
        // flow-server artifact the plugin depends on, giving priority to the
        // flow-server version defined by the project.
        // On the contrary, classes will be loaded first from the parent class
        // loader, that should be the maven plugin class loader, but augmented
        // with the project artifacts. This prevents class cast exceptions at
        // runtime caused by classes loaded both from the plugin class loader
        // and by the class loader used by Lookup (e.g.
        // EndpointGeneratorTaskFactoryImpl from Hilla could not be cast to
        // EndpointGeneratorTaskFactory because the interface is loaded by both
        // the classloaders)
        ClassLoader classLoader = new URLClassLoader(urls,
                Thread.currentThread().getContextClassLoader()) {
            @Override
            public URL getResource(String name) {
                URL resource = findResource(name);
                if (resource == null) {
                    resource = super.getResource(name);
                }
                return resource;
            }

        };
        return new ReflectionsClassFinder(classLoader, urls);
    }

    private void checkFlowCompatibility(PluginDescriptor pluginDescriptor) {
        Predicate<Artifact> isFlowServer = artifact -> "com.vaadin"
                .equals(artifact.getGroupId())
                && "flow-server".equals(artifact.getArtifactId());
        String projectFlowVersion = project.getArtifacts().stream()
                .filter(isFlowServer).map(Artifact::getVersion).findFirst()
                .orElse(null);
        String pluginFlowVersion = pluginDescriptor.getArtifacts().stream()
                .filter(isFlowServer).map(Artifact::getVersion).findFirst()
                .orElse(null);
        if (!Objects.equals(projectFlowVersion, pluginFlowVersion)) {
            getLog().warn(
                    "Vaadin Flow used in project does not match the version expected by the Vaadin plugin. "
                            + "Flow version for project is "
                            + projectFlowVersion
                            + ", Vaadin plugin is built for Flow version "
                            + pluginFlowVersion + ".");
        }
    }

}
