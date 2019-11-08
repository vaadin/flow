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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.internal.ExportsWebComponent;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.ExecutionFailedException;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.NodeTasks.Builder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.startup.ServletDeployer.StubServletConfig;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_FRONTEND_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.PARAM_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_GENERATED;

/**
 * Servlet initializer starting node updaters as well as the webpack-dev-mode
 * server.
 *
 * @since 2.0
 */
@HandlesTypes({ Route.class, UIInitListener.class,
        VaadinServiceInitListener.class, ExportsWebComponent.class,
        NpmPackage.class, NpmPackage.Container.class, JsModule.class,
        JsModule.Container.class, CssImport.class, CssImport.Container.class,
        JavaScript.class, JavaScript.Container.class, Theme.class,
        NoTheme.class, HasErrorParameter.class })
@WebListener
public class DevModeInitializer implements ServletContainerInitializer,
        Serializable, ServletContextListener {

    static class DevModeClassFinder extends DefaultClassFinder {

        private static final Set<String> APPLICABLE_CLASS_NAMES = Collections
                .unmodifiableSet(calculateApplicableClassNames());

        public DevModeClassFinder(Set<Class<?>> classes) {
            super(classes);
        }

        @Override
        public Set<Class<?>> getAnnotatedClasses(
                Class<? extends Annotation> annotation) {
            ensureImplementation(annotation);
            return super.getAnnotatedClasses(annotation);
        }

        @Override
        public <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
            ensureImplementation(type);
            return super.getSubTypesOf(type);
        }

        private void ensureImplementation(Class<?> clazz) {
            if (!APPLICABLE_CLASS_NAMES.contains(clazz.getName())) {
                throw new IllegalArgumentException("Unexpected class name "
                        + clazz + ". Implementation error: the class finder "
                        + "instance is not aware of this class. "
                        + "Fix @HandlesTypes annotation value for "
                        + DevModeInitializer.class.getName());
            }
        }

        private static Set<String> calculateApplicableClassNames() {
            HandlesTypes handlesTypes = DevModeInitializer.class
                    .getAnnotation(HandlesTypes.class);
            return Stream.of(handlesTypes.value()).map(Class::getName)
                    .collect(Collectors.toSet());
        }
    }

    private static final Pattern JAR_FILE_REGEX = Pattern
            .compile(".*file:(.+\\.jar).*");

    private static final Pattern VFS_FILE_REGEX = Pattern
            .compile("(vfs:/.+\\.jar).*");

    private static final Pattern VFS_DIRECTORY_REGEX = Pattern
            .compile("vfs:/.+");

    // allow trailing slash
    private static final Pattern DIR_REGEX_FRONTEND_DEFAULT = Pattern.compile(
            "^(?:file:0)?(.+)" + Constants.RESOURCES_FRONTEND_DEFAULT + "/?$");

    // allow trailing slash
    private static final Pattern DIR_REGEX_COMPATIBILITY_FRONTEND_DEFAULT = Pattern
            .compile("^(?:file:)?(.+)"
                    + Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                    + "/?$");

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext context)
            throws ServletException {
        Collection<? extends ServletRegistration> registrations = context
                .getServletRegistrations().values();

        if (registrations.isEmpty()) {
            return;
        }

        DeploymentConfiguration config = StubServletConfig
                .createDeploymentConfiguration(context,
                        registrations.iterator().next(), VaadinServlet.class);

        initDevModeHandler(classes, context, config);
    }

    /**
     * Initialize the devmode server if not in production mode or compatibility
     * mode.
     *
     * @param classes
     *            classes to check for npm- and js modules
     * @param context
     *            servlet context we are running in
     * @param config
     *            deployment configuration
     *
     * @throws ServletException
     *             if dev mode can't be initialized
     */
    public static void initDevModeHandler(Set<Class<?>> classes,
            ServletContext context, DeploymentConfiguration config)
            throws ServletException {
        if (config.isProductionMode()) {
            log().debug("Skipping DEV MODE because PRODUCTION MODE is set.");
            return;
        }
        if (config.isCompatibilityMode()) {
            log().debug("Skipping DEV MODE because BOWER MODE is set.");
            return;
        }
        if (!config.enableDevServer()) {
            log().debug(
                    "Skipping DEV MODE because dev server shouldn't be enabled.");
            return;
        }

        String baseDir = config.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                System.getProperty("user.dir", "."));
        String generatedDir = System.getProperty(PARAM_GENERATED_DIR,
                DEFAULT_GENERATED_DIR);
        String frontendFolder = config.getStringProperty(PARAM_FRONTEND_DIR,
                System.getProperty(PARAM_FRONTEND_DIR, DEFAULT_FRONTEND_DIR));

        Builder builder = new NodeTasks.Builder(new DevModeClassFinder(classes),
                new File(baseDir), new File(generatedDir),
                new File(frontendFolder));

        log().info("Starting dev-mode updaters in {} folder.",
                builder.npmFolder);

        if (!builder.generatedFolder.exists()) {
            try {
                FileUtils.forceMkdir(builder.generatedFolder);
            } catch (IOException e) {
                throw new UncheckedIOException(
                        String.format("Failed to create directory '%s'",
                                builder.generatedFolder),
                        e);
            }
        }

        File generatedPackages = new File(builder.generatedFolder,
                PACKAGE_JSON);

        // If we are missing the generated webpack configuration then generate
        // webpack configurations
        if (!new File(builder.npmFolder, WEBPACK_GENERATED).exists()) {
            builder.withWebpack(builder.npmFolder, FrontendUtils.WEBPACK_CONFIG,
                    FrontendUtils.WEBPACK_GENERATED);
        }

        // If we are missing either the base or generated package json files
        // generate those
        if (!new File(builder.npmFolder, PACKAGE_JSON).exists()
                || !generatedPackages.exists()) {
            builder.createMissingPackageJson(true);
        }

        Set<File> frontendLocations = getFrontendLocationsFromClassloader(
                DevModeInitializer.class.getClassLoader());

        boolean useByteCodeScanner = config.getBooleanProperty(
                SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                Boolean.parseBoolean(System.getProperty(
                        SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                        Boolean.FALSE.toString())));

        String polymerVersion = config.getStringProperty(
                Constants.SERVLET_PARAMETER_DEVMODE_POLYMER_VERSION, null);

        VaadinContext vaadinContext = new VaadinServletContext(context);
        JsonObject tokenFileData = Json.createObject();
        try {
            builder.enablePackagesUpdate(true)
                    .useByteCodeScanner(useByteCodeScanner)
                    .copyResources(frontendLocations)
                    .copyLocalResources(new File(baseDir,
                            Constants.LOCAL_FRONTEND_RESOURCES_PATH))
                    .enableImportsUpdate(true).runNpmInstall(true)
                    .withEmbeddableWebComponents(true)
                    .populateTokenFileData(tokenFileData)
                    .withPolymerVersion(polymerVersion).build().execute();

            FallbackChunk chunk = FrontendUtils
                    .readFallbackChunk(tokenFileData);
            if (chunk != null) {
                vaadinContext.setAttribute(chunk);
            }
        } catch (ExecutionFailedException exception) {
            log().debug(
                    "Could not initialize dev mode handler. One of the node tasks failed",
                    exception);
            throw new ServletException(exception);
        }

        try {
            DevModeHandler.start(config, builder.npmFolder);
        } catch (IllegalStateException exception) {
            // wrap an ISE which can be caused by inability to find tools like
            // node, npm into a servlet exception
            throw new ServletException(exception);
        }
    }

    private static Logger log() {
        return LoggerFactory.getLogger(DevModeInitializer.class);
    }

    @Override
    public void contextInitialized(ServletContextEvent ctx) {
        // No need to do anything on init
    }

    @Override
    public void contextDestroyed(ServletContextEvent ctx) {
        DevModeHandler handler = DevModeHandler.getDevModeHandler();
        if (handler != null && !handler.reuseDevServer()) {
            handler.stop();
        }
    }

    /*
     * This method returns all folders of jar files having files in the
     * META-INF/resources/frontend folder. We don't use URLClassLoader because
     * will fail in Java 9+
     */
    static Set<File> getFrontendLocationsFromClassloader(
            ClassLoader classLoader) throws ServletException {
        Set<File> frontendFiles = new HashSet<>();
        frontendFiles.addAll(getFrontendLocationsFromClassloader(classLoader,
                Constants.RESOURCES_FRONTEND_DEFAULT));
        frontendFiles.addAll(getFrontendLocationsFromClassloader(classLoader,
                Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT));
        return frontendFiles;
    }

    private static Set<File> getFrontendLocationsFromClassloader(
            ClassLoader classLoader, String resourcesFolder)
            throws ServletException {
        Set<File> frontendFiles = new HashSet<>();
        try {
            Enumeration<URL> en = classLoader.getResources(resourcesFolder);
            if (en == null) {
                return frontendFiles;
            }
            Set<String> vfsJars = new HashSet<>();
            while (en.hasMoreElements()) {
                URL url = en.nextElement();
                String urlString = url.toString();

                String path = URLDecoder.decode(url.getPath(),
                        StandardCharsets.UTF_8.name());
                Matcher jarMatcher = JAR_FILE_REGEX.matcher(path);
                Matcher dirMatcher = DIR_REGEX_FRONTEND_DEFAULT.matcher(path);
                Matcher dirCompatibilityMatcher = DIR_REGEX_COMPATIBILITY_FRONTEND_DEFAULT
                        .matcher(path);
                Matcher jarVfsMatcher = VFS_FILE_REGEX.matcher(urlString);
                if (jarMatcher.find()) {
                    frontendFiles.add(new File(jarMatcher.group(1)));
                } else if (dirMatcher.find()) {
                    frontendFiles.add(new File(dirMatcher.group(1)));
                } else if (dirCompatibilityMatcher.find()) {
                    frontendFiles
                            .add(new File(dirCompatibilityMatcher.group(1)));
                } else if (jarVfsMatcher.find()) {
                    String vfsJar = jarVfsMatcher.group(1);
                    if (vfsJars.add(vfsJar))
                        frontendFiles.add(
                                getPhysicalFileOfJBossVfsJar(new URL(vfsJar)));
                } else if (VFS_DIRECTORY_REGEX.matcher(urlString).find()) {
                    URL vfsDirUrl = new URL(urlString.substring(0,
                            urlString.lastIndexOf(resourcesFolder)));
                    frontendFiles
                            .add(getPhysicalFileOfJBossVfsDirectory(vfsDirUrl));
                } else {
                    log().warn(
                            "Resource {} not visited because does not meet supported formats.",
                            url.getPath());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return frontendFiles;
    }

    private static File getPhysicalFileOfJBossVfsDirectory(URL url)
            throws IOException, ServletException {
        try {
            Object virtualFile = url.openConnection().getContent();
            Class virtualFileClass = virtualFile.getClass();

            // Reflection as we cannot afford a dependency to WildFly or JBoss
            Method getChildrenRecursivelyMethod = virtualFileClass
                    .getMethod("getChildrenRecursively");
            Method getPhysicalFileMethod = virtualFileClass
                    .getMethod("getPhysicalFile");

            // By calling getPhysicalFile, we make sure that the corresponding
            // physical files/directories of the root directory and its children
            // are created. Later, these physical files are scanned to collect
            // their resources.
            List virtualFiles = (List) getChildrenRecursivelyMethod
                    .invoke(virtualFile);
            File rootDirectory = (File) getPhysicalFileMethod
                    .invoke(virtualFile);
            for (Object child : virtualFiles) {
                // side effect: create real-world files
                getPhysicalFileMethod.invoke(child);
            }
            return rootDirectory;
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException exc) {
            throw new ServletException("Failed to invoke JBoss VFS API.", exc);
        }
    }

    private static File getPhysicalFileOfJBossVfsJar(URL url)
            throws IOException, ServletException {
        try {
            Object jarVirtualFile = url.openConnection().getContent();

            // Creating a temporary jar file out of the vfs files
            String vfsJarPath = url.toString();
            String fileNamePrefix = vfsJarPath.substring(
                    vfsJarPath.lastIndexOf('/') + 1,
                    vfsJarPath.lastIndexOf(".jar"));
            Path tempJar = Files.createTempFile(fileNamePrefix, ".jar");

            generateJarFromJBossVfsFolder(jarVirtualFile, tempJar);

            File tempJarFile = tempJar.toFile();
            tempJarFile.deleteOnExit();
            return tempJarFile;
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException exc) {
            throw new ServletException("Failed to invoke JBoss VFS API.", exc);
        }
    }

    private static void generateJarFromJBossVfsFolder(Object jarVirtualFile,
            Path tempJar) throws IOException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        // We should use reflection to use JBoss VFS API as we cannot afford a
        // dependency to WildFly or JBoss
        Class virtualFileClass = jarVirtualFile.getClass();
        Method getChildrenRecursivelyMethod = virtualFileClass
                .getMethod("getChildrenRecursively");
        Method openStreamMethod = virtualFileClass.getMethod("openStream");
        Method isFileMethod = virtualFileClass.getMethod("isFile");
        Method getPathNameRelativeToMethod = virtualFileClass
                .getMethod("getPathNameRelativeTo", virtualFileClass);

        List jarVirtualChildren = (List) getChildrenRecursivelyMethod
                .invoke(jarVirtualFile);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempJar))) {
            for (Object child : jarVirtualChildren) {
                if (!(Boolean) isFileMethod.invoke(child))
                    continue;

                String relativePath = (String) getPathNameRelativeToMethod
                        .invoke(child, jarVirtualFile);
                InputStream inputStream = (InputStream) openStreamMethod
                        .invoke(child);
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipOutputStream.putNextEntry(zipEntry);
                IOUtils.copy(inputStream, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }
}
