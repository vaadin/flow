/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.base.devserver.startup;

import jakarta.servlet.annotation.HandlesTypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.DevBundleBuildingHandler;
import com.vaadin.base.devserver.OpenInCurrentIde;
import com.vaadin.base.devserver.ViteHandler;
import com.vaadin.base.devserver.stats.DevModeUsageStatistics;
import com.vaadin.base.devserver.stats.StatisticsSender;
import com.vaadin.base.devserver.stats.StatisticsStorage;
import com.vaadin.base.devserver.viteproxy.ViteWebsocketEndpoint;
import com.vaadin.experimental.FeatureFlags;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.ExecutionFailedException;
import com.vaadin.flow.server.frontend.NodeTasks;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.installer.NodeInstaller;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.VaadinInitializerException;
import com.vaadin.pro.licensechecker.LicenseChecker;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.PROJECT_FRONTEND_GENERATED_DIR_TOKEN;
import static com.vaadin.flow.server.Constants.VAADIN_SERVLET_RESOURCES;
import static com.vaadin.flow.server.Constants.VAADIN_WEBAPP_RESOURCES;
import static com.vaadin.flow.server.InitParameters.NODE_DOWNLOAD_ROOT;
import static com.vaadin.flow.server.InitParameters.NODE_VERSION;
import static com.vaadin.flow.server.InitParameters.NPM_EXCLUDE_WEB_COMPONENTS;
import static com.vaadin.flow.server.InitParameters.REACT_ENABLE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE;
import static com.vaadin.flow.server.frontend.FrontendTools.DEFAULT_NODE_VERSION;

/**
 * Initializer for starting node updaters as well as the dev mode server.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public class DevModeInitializer implements Serializable {

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
                        + DevModeStartupListener.class.getName());
            }
        }

        private static Set<String> calculateApplicableClassNames() {
            HandlesTypes handlesTypes = DevModeStartupListener.class
                    .getAnnotation(HandlesTypes.class);
            return Stream.of(handlesTypes.value()).map(Class::getName)
                    .collect(Collectors.toSet());
        }
    }

    private static final Pattern JAR_FILE_REGEX = Pattern
            .compile(".*file:(.+\\.jar).*");

    // Path of jar files in a URL with zip protocol doesn't start with
    // "zip:"
    // nor "file:". It contains only the path of the file.
    // Weblogic uses zip protocol.
    private static final Pattern ZIP_PROTOCOL_JAR_FILE_REGEX = Pattern
            .compile("(.+\\.jar).*");

    private static final Pattern VFS_FILE_REGEX = Pattern
            .compile("(vfs:/.+\\.jar).*");

    private static final Pattern VFS_DIRECTORY_REGEX = Pattern
            .compile("vfs:/.+");

    // allow trailing slash
    private static final Pattern DIR_REGEX_FRONTEND_DEFAULT = Pattern.compile(
            "^(?:file:0)?(.+)" + Constants.RESOURCES_FRONTEND_DEFAULT + "/?$");

    // allow trailing slash
    private static final Pattern DIR_REGEX_RESOURCES_JAR_DEFAULT = Pattern
            .compile("^(?:file:0)?(.+)" + Constants.RESOURCES_THEME_JAR_DEFAULT
                    + "/?$");

    // allow trailing slash
    private static final Pattern DIR_REGEX_COMPATIBILITY_FRONTEND_DEFAULT = Pattern
            .compile("^(?:file:)?(.+)"
                    + Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT
                    + "/?$");

    /**
     * Initialize the devmode server if not in production mode or compatibility
     * mode.
     *
     * @param classes
     *            classes to check for npm- and js modules
     * @param context
     *            VaadinContext we are running in
     * @param taskExecutor
     *            the executor to use for asynchronous execution
     * @return the initialized dev mode handler or {@code null} if none was
     *         created
     *
     * @throws VaadinInitializerException
     *             if dev mode can't be initialized
     */
    public static DevModeHandler initDevModeHandler(Set<Class<?>> classes,
            VaadinContext context, Executor taskExecutor)
            throws VaadinInitializerException {

        ApplicationConfiguration config = ApplicationConfiguration.get(context);
        if (config.isProductionMode()) {
            log().debug("Skipping DEV MODE because PRODUCTION MODE is set.");
            return null;
        }

        // This needs to be set as there is no "current service" available in
        // this call
        FeatureFlags featureFlags = FeatureFlags.get(context);
        LicenseChecker.setStrictOffline(true);

        featureFlags.setPropertiesLocation(config.getJavaResourceFolder());

        File baseDir = config.getProjectFolder();

        // Initialize the usage statistics if enabled
        if (config.isUsageStatisticsEnabled()) {
            StatisticsStorage storage = new StatisticsStorage();
            DevModeUsageStatistics.init(baseDir, storage,
                    new StatisticsSender(storage));
            DevModeUsageStatistics.collectEvent(
                    "ide_" + OpenInCurrentIde.getIdeAndProcessInfo().ide()
                            .name().toLowerCase(Locale.ENGLISH));
        }

        File frontendFolder = config.getFrontendFolder();

        Lookup lookupFromContext = context.getAttribute(Lookup.class);
        Lookup lookupForClassFinder = Lookup.of(new DevModeClassFinder(classes),
                ClassFinder.class);
        Lookup lookup = Lookup.compose(lookupForClassFinder, lookupFromContext);
        Options options = new Options(lookup, baseDir)
                .withFrontendDirectory(frontendFolder)
                .withFrontendGeneratedFolder(
                        new File(frontendFolder + FrontendUtils.GENERATED))
                .withBuildDirectory(config.getBuildFolder());

        log().info("Starting dev-mode updaters in {} folder.",
                options.getNpmFolder());

        // Regenerate Vite configuration, as it may be necessary to
        // update it
        // TODO: make sure target directories are aligned with build
        // config,
        // see https://github.com/vaadin/flow/issues/9082
        File target = new File(baseDir, config.getBuildFolder());
        options.withBuildResultFolders(
                Paths.get(target.getPath(), "classes", VAADIN_WEBAPP_RESOURCES)
                        .toFile(),
                Paths.get(target.getPath(), "classes", VAADIN_SERVLET_RESOURCES)
                        .toFile());

        // If we are missing either the base or generated package json
        // files generate those
        if (!new File(options.getNpmFolder(), PACKAGE_JSON).exists()) {
            options.createMissingPackageJson(true);
        }

        ResourceProvider resourceProvider = lookup
                .lookup(ResourceProvider.class);
        Set<File> frontendLocations = getFrontendLocationsFromResourceProvider(
                resourceProvider);

        boolean useByteCodeScanner = config.getBooleanProperty(
                SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                Boolean.parseBoolean(System.getProperty(
                        SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                        Boolean.FALSE.toString())));

        boolean enablePnpm = config.isPnpmEnabled();
        boolean enableBun = config.isBunEnabled();

        boolean useGlobalPnpm = config.isGlobalPnpm();

        boolean useHomeNodeExec = config.getBooleanProperty(
                InitParameters.REQUIRE_HOME_NODE_EXECUTABLE, false);

        String[] additionalPostinstallPackages = config
                .getStringProperty(
                        InitParameters.ADDITIONAL_POSTINSTALL_PACKAGES, "")
                .split(",");

        String frontendGeneratedFolderName = config.getStringProperty(
                PROJECT_FRONTEND_GENERATED_DIR_TOKEN,
                Paths.get(frontendFolder.getPath(), FrontendUtils.GENERATED)
                        .toString());
        File frontendGeneratedFolder = new File(frontendGeneratedFolderName);
        File jarFrontendResourcesFolder = new File(frontendGeneratedFolder,
                FrontendUtils.JAR_RESOURCES_FOLDER);
        Mode mode = config.getMode();
        boolean reactEnable = config.getBooleanProperty(REACT_ENABLE,
                FrontendUtils
                        .isReactRouterRequired(options.getFrontendDirectory()));

        boolean npmExcludeWebComponents = config
                .getBooleanProperty(NPM_EXCLUDE_WEB_COMPONENTS, false);

        options.enablePackagesUpdate(true)
                .useByteCodeScanner(useByteCodeScanner)
                .withFrontendGeneratedFolder(frontendGeneratedFolder)
                .withJarFrontendResourcesFolder(jarFrontendResourcesFolder)
                .copyResources(frontendLocations)
                .copyLocalResources(new File(baseDir,
                        Constants.LOCAL_FRONTEND_RESOURCES_PATH))
                .enableImportsUpdate(true)
                .withRunNpmInstall(mode == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD)
                .withEmbeddableWebComponents(true).withEnablePnpm(enablePnpm)
                .withEnableBun(enableBun).useGlobalPnpm(useGlobalPnpm)
                .withHomeNodeExecRequired(useHomeNodeExec)
                .withProductionMode(config.isProductionMode())
                .withPostinstallPackages(
                        Arrays.asList(additionalPostinstallPackages))
                .withFrontendHotdeploy(
                        mode == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD)
                .withBundleBuild(mode == Mode.DEVELOPMENT_BUNDLE)
                .withFrontendExtraFileExtensions(
                        getFrontendExtraFileExtensions(config))
                .withReact(reactEnable)
                .withNpmExcludeWebComponents(npmExcludeWebComponents)
                .withNodeVersion(config.getStringProperty(NODE_VERSION,
                        DEFAULT_NODE_VERSION))
                .withNodeDownloadRoot(
                        URI.create(config.getStringProperty(NODE_DOWNLOAD_ROOT,
                                NodeInstaller.DEFAULT_NODEJS_DOWNLOAD_ROOT)));

        // Do not execute inside runnable thread as static mocking doesn't work.
        NodeTasks tasks = new NodeTasks(options);
        Runnable runnable = () -> {
            runNodeTasks(tasks);
            if (mode == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD) {
                // For Vite, wait until a VaadinServlet is deployed so we know
                // which frontend servlet path to use
                if (VaadinServlet.getFrontendMapping() == null) {
                    log().debug("Waiting for a VaadinServlet to be deployed");
                    while (VaadinServlet.getFrontendMapping() == null) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
            }
        };

        CompletableFuture<Void> nodeTasksFuture = CompletableFuture
                .runAsync(runnable, taskExecutor);

        Lookup devServerLookup = Lookup.compose(lookup,
                Lookup.of(config, ApplicationConfiguration.class));
        int port = Integer
                .parseInt(config.getStringProperty("devServerPort", "0"));
        if (mode == Mode.DEVELOPMENT_BUNDLE) {
            // Shows a "build in progress" page during dev bundle creation
            return new DevBundleBuildingHandler(nodeTasksFuture);
        } else {
            ViteHandler handler = new ViteHandler(devServerLookup, port,
                    options.getNpmFolder(), nodeTasksFuture);
            VaadinServlet.whenFrontendMappingAvailable(
                    () -> ViteWebsocketEndpoint.init(context, handler));
            return handler;
        }
    }

    static List<String> getFrontendExtraFileExtensions(
            ApplicationConfiguration config) {
        List<String> stringProperty = Arrays.stream(config
                .getStringProperty(InitParameters.FRONTEND_EXTRA_EXTENSIONS, "")
                .split(",")).filter(input -> !input.isBlank()).toList();
        return stringProperty.isEmpty() ? null : stringProperty;
    }

    private static Logger log() {
        return LoggerFactory.getLogger(DevModeStartupListener.class);
    }

    /*
     * This method returns all folders of jar files having files in the
     * META-INF/resources/frontend and META-INF/resources/themes folder. We
     * don't use URLClassLoader because will fail in Java 9+
     */
    static Set<File> getFrontendLocationsFromResourceProvider(
            ResourceProvider resourceProvider)
            throws VaadinInitializerException {
        Set<File> frontendFiles = new HashSet<>();
        frontendFiles.addAll(getFrontendLocationsFromResourceProvider(
                resourceProvider, Constants.RESOURCES_FRONTEND_DEFAULT));
        frontendFiles.addAll(
                getFrontendLocationsFromResourceProvider(resourceProvider,
                        Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT));
        frontendFiles.addAll(getFrontendLocationsFromResourceProvider(
                resourceProvider, Constants.RESOURCES_THEME_JAR_DEFAULT));
        return frontendFiles;
    }

    private static void runNodeTasks(NodeTasks tasks) {
        try {
            tasks.execute();
        } catch (ExecutionFailedException exception) {
            log().debug(
                    "Could not initialize dev mode handler. One of the node tasks failed",
                    exception);
            throw new CompletionException(exception);
        }
    }

    private static Set<File> getFrontendLocationsFromResourceProvider(
            ResourceProvider resourceProvider, String resourcesFolder)
            throws VaadinInitializerException {
        Set<File> frontendFiles = new HashSet<>();
        try {
            List<URL> en = resourceProvider
                    .getApplicationResources(resourcesFolder);
            if (en == null) {
                return frontendFiles;
            }
            Set<String> vfsJars = new HashSet<>();
            for (URL url : en) {
                String urlString = url.toString();

                String path = URLDecoder.decode(url.getPath(),
                        StandardCharsets.UTF_8);
                Matcher jarMatcher = JAR_FILE_REGEX.matcher(path);
                Matcher zipProtocolJarMatcher = ZIP_PROTOCOL_JAR_FILE_REGEX
                        .matcher(path);
                Matcher dirMatcher = DIR_REGEX_FRONTEND_DEFAULT.matcher(path);
                Matcher dirResourcesMatcher = DIR_REGEX_RESOURCES_JAR_DEFAULT
                        .matcher(path);
                Matcher dirCompatibilityMatcher = DIR_REGEX_COMPATIBILITY_FRONTEND_DEFAULT
                        .matcher(path);
                Matcher jarVfsMatcher = VFS_FILE_REGEX.matcher(urlString);
                Matcher dirVfsMatcher = VFS_DIRECTORY_REGEX.matcher(urlString);
                if (jarVfsMatcher.find()) {
                    String vfsJar = jarVfsMatcher.group(1);
                    if (vfsJars.add(vfsJar)) { // NOSONAR
                        frontendFiles.add(getPhysicalFileOfJBossVfsJar(
                                URI.create(vfsJar).toURL()));
                    }
                } else if (dirVfsMatcher.find()) {
                    URL vfsDirUrl = URI
                            .create(urlString.substring(0,
                                    urlString.lastIndexOf(resourcesFolder)))
                            .toURL();
                    frontendFiles
                            .add(getPhysicalFileOfJBossVfsDirectory(vfsDirUrl));
                } else if (jarMatcher.find()) {
                    frontendFiles.add(new File(jarMatcher.group(1)));
                } else if ("zip".equalsIgnoreCase(url.getProtocol())
                        && zipProtocolJarMatcher.find()) {
                    frontendFiles.add(new File(zipProtocolJarMatcher.group(1)));
                } else if (dirMatcher.find()) {
                    frontendFiles.add(new File(dirMatcher.group(1)));
                } else if (dirResourcesMatcher.find()) {
                    frontendFiles.add(new File(dirResourcesMatcher.group(1)));
                } else if (dirCompatibilityMatcher.find()) {
                    frontendFiles
                            .add(new File(dirCompatibilityMatcher.group(1)));
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
            throws IOException, VaadinInitializerException {
        try {
            Object virtualFile = url.openConnection().getContent();
            Class<?> virtualFileClass = virtualFile.getClass();

            // Reflection as we cannot afford a dependency to
            // WildFly or JBoss
            Method getChildrenRecursivelyMethod = virtualFileClass
                    .getMethod("getChildrenRecursively");
            Method getPhysicalFileMethod = virtualFileClass
                    .getMethod("getPhysicalFile");

            // By calling getPhysicalFile, we make sure that the
            // corresponding
            // physical files/directories of the root directory and
            // its children
            // are created. Later, these physical files are scanned
            // to collect
            // their resources.
            List<?> virtualFiles = (List<?>) getChildrenRecursivelyMethod
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
            throw new VaadinInitializerException(
                    "Failed to invoke JBoss VFS API.", exc);
        }
    }

    private static File getPhysicalFileOfJBossVfsJar(URL url)
            throws IOException, VaadinInitializerException {
        try {
            Object jarVirtualFile = url.openConnection().getContent();

            // Creating a temporary jar file out of the vfs files
            String vfsJarPath = url.toString();
            String fileNamePrefix = vfsJarPath.substring(
                    vfsJarPath.lastIndexOf(
                            vfsJarPath.contains("\\") ? '\\' : '/') + 1,
                    vfsJarPath.lastIndexOf(".jar"));
            Path tempJar = Files.createTempFile(fileNamePrefix, ".jar");

            generateJarFromJBossVfsFolder(jarVirtualFile, tempJar);

            File tempJarFile = tempJar.toFile();
            tempJarFile.deleteOnExit();
            return tempJarFile;
        } catch (NoSuchMethodException | IllegalAccessException
                | InvocationTargetException exc) {
            throw new VaadinInitializerException(
                    "Failed to invoke JBoss VFS API.", exc);
        }
    }

    private static void generateJarFromJBossVfsFolder(Object jarVirtualFile,
            Path tempJar) throws IOException, IllegalAccessException,
            InvocationTargetException, NoSuchMethodException {
        // We should use reflection to use JBoss VFS API as we cannot
        // afford a
        // dependency to WildFly or JBoss
        Class<?> virtualFileClass = jarVirtualFile.getClass();
        Method getChildrenRecursivelyMethod = virtualFileClass
                .getMethod("getChildrenRecursively");
        Method openStreamMethod = virtualFileClass.getMethod("openStream");
        Method isFileMethod = virtualFileClass.getMethod("isFile");
        Method getPathNameRelativeToMethod = virtualFileClass
                .getMethod("getPathNameRelativeTo", virtualFileClass);

        List<?> jarVirtualChildren = (List<?>) getChildrenRecursivelyMethod
                .invoke(jarVirtualFile);
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(
                Files.newOutputStream(tempJar))) {
            for (Object child : jarVirtualChildren) {
                if (!(Boolean) isFileMethod.invoke(child))
                    continue;

                String relativePath = (String) getPathNameRelativeToMethod
                        .invoke(child, jarVirtualFile);
                try (InputStream inputStream = (InputStream) openStreamMethod
                        .invoke(child)) {
                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zipOutputStream.putNextEntry(zipEntry);
                    inputStream.transferTo(zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }
        }
    }
}
