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
package com.vaadin.base.devserver.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendBuildUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.VaadinInitializerException;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_THEME_JAR_DEFAULT;
import static com.vaadin.flow.server.Constants.TARGET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@NotThreadSafe
class DevModeInitializerTest extends DevModeInitializerTestBase {

    @JsModule("foo")
    public static class Visited {
    }

    public static class NotVisitedWithoutDeps {
    }

    @JsModule("bar")
    public static class NotVisitedWithDeps {
    }

    public static class WithoutDepsSubclass extends NotVisitedWithoutDeps {
    }

    public static class WithDepsSubclass extends NotVisitedWithDeps {
    }

    public static class VisitedSubclass extends Visited {
    }

    @Route
    public static class RoutedWithReferenceToVisited {
        Visited b;
    }

    public static class MockVirtualFile {
        File file;

        public List<MockVirtualFile> getChildrenRecursively() {
            List<MockVirtualFile> files = new ArrayList<>();

            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    MockVirtualFile mvf = new MockVirtualFile();
                    mvf.file = child;
                    files.add(mvf);
                    files.addAll(mvf.getChildrenRecursively());
                }
            }
            return files;
        }

        public InputStream openStream() throws FileNotFoundException {
            return new FileInputStream(file);
        }

        public File getPhysicalFile() {
            return file;
        }

        public String getPathNameRelativeTo(MockVirtualFile other) {
            return Paths.get(file.toURI())
                    .relativize(Paths.get(other.file.toURI())).toString();
        }

        public boolean isFile() {
            return file.isFile();
        }
    }

    @Test
    void loadingJars_useModernResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingJars_allFilesExist(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    void loadingJars_useResourcesThemesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingJarsWithProtocol_allFilesExist(RESOURCES_THEME_JAR_DEFAULT,
                "src/test/resources/jar-with-themes-resources.jar!/META-INF/resources/themes",
                this::jarUrlBuilder);
    }

    @Test
    void loadingZipProtocolJars_useModernResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingZipProtocolJars_allFilesExist(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    void loadingJars_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingJars_allFilesExist(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    void loadingFsResources_useModernResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingFsResources_allFilesExist("/dir-with-modern-frontend/",
                RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    void loadingFsResources_useResourcesThemesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingFsResources_allFilesExist("/dir-with-theme-resources/",
                RESOURCES_THEME_JAR_DEFAULT);
    }

    @Test
    void loadingFsResources_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingFsResources_allFilesExist("/dir-with-frontend-resources/",
                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    void loadingFsResources_usesVfsProtocol_allFilesExist() throws Exception {
        String path = "/dir-with-modern-frontend/" + RESOURCES_FRONTEND_DEFAULT;
        MockVirtualFile virtualFile = new MockVirtualFile();
        virtualFile.file = new File(getClass().getResource(path).toURI());

        URLConnection urlConnection = Mockito.mock(URLConnection.class);
        Mockito.when(urlConnection.getContent()).thenReturn(virtualFile);

        try {
            URL.setURLStreamHandlerFactory(protocol -> {
                if (protocol.equals("vfs")) {
                    return new URLStreamHandler() {
                        @Override
                        protected URLConnection openConnection(URL u) {
                            return urlConnection;
                        }
                    };
                }
                return null;
            });
            URL url = new URL("vfs://some-non-existent-place" + path);

            loadingFsResources_allFilesExist(Collections.singletonList(url),
                    RESOURCES_FRONTEND_DEFAULT);
        } finally {
            Field field = URL.class.getDeclaredField("factory");
            field.setAccessible(true);
            field.set(null, null);
        }
    }

    @Test
    void should_Run_Updaters_doesNotThrow() throws Exception {
        // no any exception means that updaters are executed and dev mode server
        // started
        process();
    }

    @Test
    void should_Run_Updaters_when_NoNodeConfFiles_doesNotThrow()
            throws Exception {
        devServerConfigFile.delete();
        mainPackageFile.delete();
        // no any exception means that updaters are executed and dev mode server
        // started
        process();
    }

    void should_Run_Updaters() throws Exception {
        process();
        assertDevModeHandlerStarted();
    }

    @Test
    void should_Run_Updaters_when_NoNodeConfFiles() throws Exception {
        devServerConfigFile.delete();
        mainPackageFile.delete();
        process();
        assertDevModeHandlerStarted();
    }

    @Test
    void should_Not_Run_Updaters_when_NoMainPackageFile() throws Exception {
        assertNoDevModeHandlerCreated();
        mainPackageFile.delete();
        assertNoDevModeHandlerCreated();
    }

    @Test
    void should_Run_Updaters_when_NoAppPackageFile() throws Exception {
        process();
        assertDevModeHandlerStarted();
    }

    @Test
    void should_Run_Updaters_when_NoViteFile() throws Exception {
        devServerConfigFile.delete();
        process();
        assertDevModeHandlerStarted();
    }

    @Test
    void should_Not_Run_Updaters_inProductionMode() throws Exception {
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);
        devModeStartupListener = new DevModeStartupListener();
        devModeStartupListener.onStartup(classes, servletContext);
        assertNoDevModeHandlerCreated();
    }

    @Test
    void should_Not_AddContextListener() throws Exception {
        ArgumentCaptor<? extends EventListener> arg = ArgumentCaptor
                .forClass(EventListener.class);
        process();
        Mockito.verify(servletContext, Mockito.never())
                .addListener(arg.capture());
    }

    @Test
    void listener_should_stopDevModeHandler_onDestroy() throws Exception {
        Mockito.when(appConfig.reuseDevServer()).thenReturn(false);

        process();

        assertTrue(hasDevServerProcess(handler));
        runDestroy();
        assertFalse(hasDevServerProcess(handler));
    }

    @LoadDependenciesOnStartup
    public static class AppConfEager implements AppShellConfigurator {

    }

    @Test
    void shouldUseByteCodeScannerIfPropertySet() throws Exception {
        Mockito.when(appConfig.getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                false)).thenReturn(true);

        devModeStartupListener = new DevModeStartupListener();
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(NotVisitedWithDeps.class);
        classes.add(Visited.class);
        classes.add(RoutedWithReferenceToVisited.class);
        classes.add(AppConfEager.class);
        devModeStartupListener.onStartup(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevServer();

        String content = getFlowGeneratedImports();
        // Referenced by the route
        assertTrue(content.contains("import 'foo';"));
        // Not referenced by the route
        assertFalse(content.contains("import 'bar';"));
    }

    private String getFlowGeneratedImports() throws IOException {
        return Files.readString(FrontendUtils
                .getFlowGeneratedImports(
                        new File(npmFolder, FrontendUtils.DEFAULT_FRONTEND_DIR))
                .toPath(), StandardCharsets.UTF_8);
    }

    @Test
    void shouldUseFullPathScannerByDefault() throws Exception {
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(NotVisitedWithDeps.class);
        classes.add(Visited.class);
        classes.add(RoutedWithReferenceToVisited.class);
        devModeStartupListener.onStartup(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevServer();
        String content = getFlowGeneratedImports();
        // Referenced by the route
        assertTrue(content.contains("import 'foo';"));
        // Not referenced by the route
        assertTrue(content.contains("import 'bar';"));
    }

    @Test
    void should_generateOpenApi_when_EndpointPresents() throws Exception {
        String originalJavaSourceFolder = null;
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, "classes/com/vaadin/hilla/openapi.json")
                .toFile();
        try {
            originalJavaSourceFolder = System
                    .getProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);

            // Configure a folder to check the endpoints, doesn't matter
            // which folder, since the actual task won't be run, just
            // to verify the mocked task is executed.
            System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                    javaSourceFolder.getAbsolutePath());

            assertFalse(generatedOpenApiJson.exists());
            try (MockedStatic<FrontendBuildUtils> util = Mockito.mockStatic(
                    FrontendBuildUtils.class, Mockito.CALLS_REAL_METHODS)) {
                util.when(() -> FrontendBuildUtils.isHillaUsed(Mockito.any(),
                        Mockito.any())).thenReturn(true);
                devModeStartupListener.onStartup(classes, servletContext);
                handler = getDevModeHandler();
                waitForDevServer();
            }

            Mockito.verify(taskGenerateEndpoint, times(1)).execute();
        } finally {
            if (originalJavaSourceFolder != null) {
                System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                        originalJavaSourceFolder);
            } else {
                System.clearProperty(
                        "vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            }
            generatedOpenApiJson.delete();
        }

    }

    @Test
    void should_notGenerateOpenApi_when_EndpointIsNotUsed() throws Exception {
        String originalJavaSourceFolder = null;
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, "classes/com/vaadin/hilla/openapi.json")
                .toFile();
        try {
            originalJavaSourceFolder = System
                    .getProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            System.clearProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            Mockito.doReturn(null).when(lookup)
                    .lookup(EndpointGeneratorTaskFactory.class);

            assertFalse(generatedOpenApiJson.exists());
            devModeStartupListener.onStartup(classes, servletContext);
            handler = getDevModeHandler();
            waitForDevServer();

            Mockito.verify(taskGenerateEndpoint, never()).execute();
        } finally {
            if (originalJavaSourceFolder != null) {
                System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                        originalJavaSourceFolder);
            } else {
                System.clearProperty(
                        "vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            }
            generatedOpenApiJson.delete();
        }
    }

    @Test
    void should_generateTs_files() throws Exception {
        String originalJavaSourceFolder = null;
        try {
            originalJavaSourceFolder = System
                    .getProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);

            // Configure a folder to check the endpoints, doesn't matter
            // which folder, since the actual task won't be run, just
            // to verify the mocked task is executed.
            System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                    javaSourceFolder.getAbsolutePath());

            try (MockedStatic<FrontendBuildUtils> util = Mockito.mockStatic(
                    FrontendBuildUtils.class, Mockito.CALLS_REAL_METHODS)) {
                util.when(() -> FrontendBuildUtils.isHillaUsed(Mockito.any(),
                        Mockito.any())).thenReturn(true);
                devModeStartupListener.onStartup(classes, servletContext);
                handler = getDevModeHandler();
                waitForDevServer();
            }

            Mockito.verify(taskGenerateEndpoint, times(1)).execute();
        } finally {
            if (originalJavaSourceFolder != null) {
                System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                        originalJavaSourceFolder);
            } else {
                System.clearProperty(
                        "vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            }
        }
    }

    @Test
    void onStartup_emptyServletRegistrations_shouldCreateDevModeHandler()
            throws Exception {
        devModeStartupListener.onStartup(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevServer();
        assertDevModeHandlerStarted();
    }

    @Test
    void onStartup_devModeAlreadyStarted_shouldBeTrueWhenStarted()
            throws Exception {
        final Map<String, Object> servletContextAttributes = new HashMap<>();
        Mockito.doAnswer(answer -> {
            String key = answer.getArgument(0);
            Object value = answer.getArgument(1);
            servletContextAttributes.putIfAbsent(key, value);
            return null;
        }).when(servletContext).setAttribute(Mockito.anyString(),
                Mockito.any());
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .thenAnswer(answer -> {
                    return servletContextAttributes.get(answer.getArgument(0));
                });

        servletContextAttributes.put(Lookup.class.getName(), lookup);
        servletContextAttributes.put(ApplicationConfiguration.class.getName(),
                appConfig);

        process();
        assertNotNull(devModeHandlerManager.getDevModeHandler());
    }

    @Test
    void onStartup_fallbackBaseDirIsNotProjectDirectory_throws()
            throws Exception {
        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(null);
        File tmp = Files.createTempDirectory("devmode-test").toFile();
        baseDir = tmp.getPath();

        String originalUserDirValue = null;
        try {
            originalUserDirValue = System.getProperty("user.dir");
            System.setProperty("user.dir", baseDir);
            assertThrows(IllegalStateException.class,
                    () -> devModeStartupListener.onStartup(classes,
                            servletContext));
        } finally {
            if (originalUserDirValue != null) {
                System.setProperty("user.dir", originalUserDirValue);
            }
        }
    }

    @Test
    void onStartup_fallbackBaseDirIsMavenProjectDirectory_isAccepted()
            throws Exception {
        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(null);
        File tmp = Files.createTempDirectory("devmode-test").toFile();
        new File(tmp, "pom.xml").createNewFile();
        baseDir = tmp.getPath();

        String originalUserDirValue = null;
        try {
            originalUserDirValue = System.getProperty("user.dir");
            System.setProperty("user.dir", baseDir);
            devModeStartupListener.onStartup(classes, servletContext);
        } finally {
            if (originalUserDirValue != null) {
                System.setProperty("user.dir", originalUserDirValue);
            }
        }
    }

    @Test
    void onStartup_fallbackBaseDirIsGradleProjectDirectory_isAccepted()
            throws Exception {
        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(null);
        File tmp = Files.createTempDirectory("devmode-test").toFile();
        new File(tmp, "build.gradle").createNewFile();
        baseDir = tmp.getPath();

        String originalUserDirValue = null;
        try {
            originalUserDirValue = System.getProperty("user.dir");
            System.setProperty("user.dir", baseDir);
            devModeStartupListener.onStartup(classes, servletContext);
        } finally {
            if (originalUserDirValue != null) {
                System.setProperty("user.dir", originalUserDirValue);
            }
        }
    }

    @Test
    void getFrontendExtraExtensions_noExtensionsSet_returnsNull() {
        Mockito.when(appConfig.getStringProperty(
                InitParameters.FRONTEND_EXTRA_EXTENSIONS, "")).thenReturn("");

        List<String> frontendExtraFileExtensions = DevModeInitializer
                .getFrontendExtraFileExtensions(appConfig);
        assertNull(frontendExtraFileExtensions);
    }

    @Test
    void getFrontendExtraExtensions_extensionsSet_returnsExtensionsList() {
        Mockito.when(appConfig.getStringProperty(
                InitParameters.FRONTEND_EXTRA_EXTENSIONS, ""))
                .thenReturn(".svg,.ico,png");

        List<String> frontendExtraFileExtensions = DevModeInitializer
                .getFrontendExtraFileExtensions(appConfig);
        assertEquals(3, frontendExtraFileExtensions.size());
    }

    private void loadingJars_allFilesExist(String resourcesFolder)
            throws IOException, VaadinInitializerException {
        loadingJarsWithProtocol_allFilesExist(resourcesFolder,
                this::jarUrlBuilder);
    }

    private URL jarUrlBuilder(String url) {
        try {
            return new URL("jar:" + url);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadingZipProtocolJars_allFilesExist(String resourcesFolder)
            throws IOException, VaadinInitializerException {
        final URLStreamHandler dummyZipHandler = new URLStreamHandler() {

            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return null;
            }
        };

        loadingJarsWithProtocol_allFilesExist(resourcesFolder, s -> {
            try {
                return new URL("zip", "", -1, s.replaceFirst("file:", ""),
                        dummyZipHandler);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void loadingJarsWithProtocol_allFilesExist(String resourcesFolder,
            Function<String, URL> urlBuilder)
            throws IOException, VaadinInitializerException {
        loadingJarsWithProtocol_allFilesExist(resourcesFolder,
                "src/test/resources/with%20space/jar-with-frontend-resources.jar!/META-INF/resources/frontend",
                urlBuilder);
    }

    private void loadingJarsWithProtocol_allFilesExist(String resourcesFolder,
            String jarContent, Function<String, URL> urlBuilder)
            throws IOException, VaadinInitializerException {
        // Create jar urls with the given urlBuilder for test
        String urlPath = this.getClass().getResource("/").toString()
                .replace("target/test-classes/", "") + jarContent;
        URL jar = urlBuilder.apply(urlPath);
        List<URL> urls = new ArrayList<>();
        urls.add(jar);

        // Create mock resource provider with the single jar to be found
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        Mockito.when(resourceProvider.getApplicationResources(resourcesFolder))
                .thenReturn(urls);

        // load jars from classloader
        List<File> jarFilesFromClassloader = new ArrayList<>(DevModeInitializer
                .getFrontendLocationsFromResourceProvider(resourceProvider));

        // Assert that jar was found and accepted
        assertEquals(1, jarFilesFromClassloader.size(),
                "One jar should have been found and added as a File");
        // Assert that the file can be found from the filesystem by the given
        // path.
        assertTrue(jarFilesFromClassloader.get(0).exists(),
                "File in path 'with space' doesn't load from given path");
    }

    private void loadingFsResources_allFilesExist(String resourcesRoot,
            String resourcesFolder)
            throws IOException, VaadinInitializerException {
        List<URL> urls = Collections.singletonList(
                getClass().getResource(resourcesRoot + resourcesFolder));
        loadingFsResources_allFilesExist(urls, resourcesFolder);
    }

    private void loadingFsResources_allFilesExist(Collection<URL> urls,
            String resourcesFolder)
            throws IOException, VaadinInitializerException {

        // Create mock resource provider with the single jar to be found
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        Mockito.when(resourceProvider.getApplicationResources(resourcesFolder))
                .thenReturn(List.copyOf(urls));

        // load jars from classloader
        List<File> locations = new ArrayList<>(DevModeInitializer
                .getFrontendLocationsFromResourceProvider(resourceProvider));

        // Assert that resource was found and accepted
        assertEquals(1, locations.size(),
                "One resource should have been found and added as a File");
        // Assert that the file can be found from the filesystem by the given
        // path.
        assertTrue(locations.get(0).exists(),
                "Resource doesn't load from given path");
    }

    private void assertNoDevModeHandlerCreated() {
        assertNull(getDevModeHandler());
    }

    private void assertDevModeHandlerStarted() {
        assertTrue(hasDevServerProcess(handler));
    }

}
