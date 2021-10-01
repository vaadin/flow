package com.vaadin.base.devserver.startup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.base.devserver.WebpackHandler;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.VaadinInitializerException;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.RESOURCES_THEME_JAR_DEFAULT;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@NotThreadSafe
public class DevModeInitializerTest extends DevModeInitializerTestBase {

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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void loadingJars_useModernResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingJars_allFilesExist(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingJars_useResourcesThemesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingJarsWithProtocol_allFilesExist(RESOURCES_THEME_JAR_DEFAULT,
                "src/test/resources/jar-with-themes-resources.jar!/META-INF/resources/themes",
                this::jarUrlBuilder);
    }

    @Test
    public void loadingZipProtocolJars_useModernResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingZipProtocolJars_allFilesExist(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingJars_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingJars_allFilesExist(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_useModernResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingFsResources_allFilesExist("/dir-with-modern-frontend/",
                RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_useResourcesThemesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingFsResources_allFilesExist("/dir-with-theme-resources/",
                RESOURCES_THEME_JAR_DEFAULT);
    }

    @Test
    public void loadingFsResources_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, VaadinInitializerException {
        loadingFsResources_allFilesExist("/dir-with-frontend-resources/",
                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_usesVfsProtocol_allFilesExist()
            throws Exception {
        String path = "/dir-with-modern-frontend/" + RESOURCES_FRONTEND_DEFAULT;
        MockVirtualFile virtualFile = new MockVirtualFile();
        virtualFile.file = new File(getClass().getResource(path).toURI());

        URLConnection urlConnection = Mockito.mock(URLConnection.class);
        Mockito.when(urlConnection.getContent()).thenReturn(virtualFile);

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
    }

    @Test
    public void should_Run_Updaters_doesNotThrow() throws Exception {
        // no any exception means that updaters are executed and dev mode server
        // started
        process();
    }

    @Test
    public void should_Run_Updaters_when_NoNodeConfFiles_doesNotThrow()
            throws Exception {
        webpackFile.delete();
        mainPackageFile.delete();
        // no any exception means that updaters are executed and dev mode server
        // started
        process();
    }

    public void should_Run_Updaters() throws Exception {
        process();
        assertNotNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoNodeConfFiles() throws Exception {
        webpackFile.delete();
        mainPackageFile.delete();
        process();
        assertNotNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoMainPackageFile()
            throws Exception {
        assertNull(WebpackHandler.getDevModeHandler());
        mainPackageFile.delete();
        assertNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoAppPackageFile() throws Exception {
        process();
        assertNotNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoWebpackFile() throws Exception {
        webpackFile.delete();
        process();
        assertNotNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_inProductionMode() throws Exception {
        Mockito.when(appConfig.isProductionMode()).thenReturn(true);
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_AddContextListener() throws Exception {
        ArgumentCaptor<? extends EventListener> arg = ArgumentCaptor
                .forClass(EventListener.class);
        process();
        Mockito.verify(servletContext, Mockito.never())
                .addListener(arg.capture());
    }

    @Test
    public void listener_should_stopDevModeHandler_onDestroy()
            throws Exception {
        Mockito.when(appConfig.reuseDevServer()).thenReturn(false);

        process();

        assertNotNull(WebpackHandler.getDevModeHandler());

        runDestroy();

        assertNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void shouldUseByteCodeScannerIfPropertySet() throws Exception {
        Mockito.when(appConfig.getBooleanProperty(
                InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                false)).thenReturn(true);
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(NotVisitedWithDeps.class);
        classes.add(Visited.class);
        classes.add(RoutedWithReferenceToVisited.class);
        devModeInitializer.onStartup(classes, servletContext);
        waitForDevModeServer();
        ArgumentCaptor<? extends FallbackChunk> arg = ArgumentCaptor
                .forClass(FallbackChunk.class);
        Mockito.verify(servletContext, Mockito.atLeastOnce()).setAttribute(
                Mockito.eq(FallbackChunk.class.getName()), arg.capture());
        FallbackChunk fallbackChunk = arg.getValue();
        Assert.assertFalse(fallbackChunk.getModules().contains("foo"));

        Assert.assertTrue(fallbackChunk.getModules().contains("bar"));
    }

    @Test
    public void shouldUseFullPathScannerByDefault() throws Exception {
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(NotVisitedWithDeps.class);
        classes.add(Visited.class);
        classes.add(RoutedWithReferenceToVisited.class);
        devModeInitializer.onStartup(classes, servletContext);
        waitForDevModeServer();
        Mockito.verify(servletContext, Mockito.times(0)).setAttribute(
                Mockito.eq(FallbackChunk.class.getName()),
                Mockito.any(FallbackChunk.class));
    }

    @Test
    public void should_generateOpenApi_when_EndpointPresents()
            throws Exception {
        String originalJavaSourceFolder = null;
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, DEFAULT_CONNECT_OPENAPI_JSON_FILE)
                .toFile();
        try {
            originalJavaSourceFolder = System
                    .getProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);

            // Configure a folder to check the endpoints, doesn't matter
            // which folder, since the actual task won't be run, just
            // to verify the mocked task is executed.
            System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                    javaSourceFolder.getRoot().getAbsolutePath());

            Assert.assertFalse(generatedOpenApiJson.exists());
            DevModeInitializer devModeInitializer = new DevModeInitializer();
            devModeInitializer.onStartup(classes, servletContext);
            waitForDevModeServer();

            Mockito.verify(taskGenerateFusion, times(1)).execute();
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
    public void should_notGenerateOpenApi_when_EndpointIsNotUsed()
            throws Exception {
        String originalJavaSourceFolder = null;
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, DEFAULT_CONNECT_OPENAPI_JSON_FILE)
                .toFile();
        try {
            originalJavaSourceFolder = System
                    .getProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            System.clearProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);
            Mockito.doReturn(null).when(lookup)
                    .lookup(EndpointGeneratorTaskFactory.class);

            Assert.assertFalse(generatedOpenApiJson.exists());
            devModeInitializer.onStartup(classes, servletContext);
            waitForDevModeServer();

            Mockito.verify(taskGenerateFusion, never()).execute();
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
    public void should_generateTs_files() throws Exception {
        String originalJavaSourceFolder = null;
        try {
            originalJavaSourceFolder = System
                    .getProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);

            // Configure a folder to check the endpoints, doesn't matter
            // which folder, since the actual task won't be run, just
            // to verify the mocked task is executed.
            System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                    javaSourceFolder.getRoot().getAbsolutePath());

            DevModeInitializer devModeInitializer = new DevModeInitializer();

            devModeInitializer.onStartup(classes, servletContext);
            waitForDevModeServer();

            Mockito.verify(taskGenerateFusion, times(1)).execute();
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
    public void onStartup_emptyServletRegistrations_shouldCreateDevModeHandler()
            throws Exception {
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(classes, servletContext);
        assertNotNull(WebpackHandler.getDevModeHandler());
    }

    @Test
    public void onStartup_devModeAlreadyStarted_shouldBeTrueWhenStarted()
            throws Exception {
        final Map<String, Object> servletContextAttributes = new HashMap<>();
        Mockito.doAnswer(answer -> {
            String key = answer.getArgument(0);
            Object value = answer.getArgument(1);
            servletContextAttributes.putIfAbsent(key, value);
            return null;
        }).when(servletContext).setAttribute(Mockito.anyString(),
                Mockito.anyObject());
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .thenAnswer(answer -> servletContextAttributes
                        .get(answer.getArgument(0)));

        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        Lookup lookup = Mockito.mock(Lookup.class);
        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        servletContextAttributes.put(Lookup.class.getName(), lookup);

        process();
        assertTrue(DevModeInitializer.isDevModeAlreadyStarted(
                new VaadinServletContext(servletContext)));
    }

    @Test(expected = IllegalStateException.class)
    public void onStartup_fallbackBaseDirIsNotProjectDirectory_throws()
            throws Exception {
        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(null);
        TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        baseDir = tmp.getRoot().getPath();

        String originalUserDirValue = null;
        try {
            originalUserDirValue = System.getProperty("user.dir");
            System.setProperty("user.dir", baseDir);
            devModeInitializer.onStartup(classes, servletContext);
        } finally {
            if (originalUserDirValue != null) {
                System.setProperty("user.dir", originalUserDirValue);
            }
        }
    }

    @Test
    public void onStartup_fallbackBaseDirIsMavenProjectDirectory_isAccepted()
            throws Exception {
        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(null);
        TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        tmp.newFile("pom.xml");
        baseDir = tmp.getRoot().getPath();

        String originalUserDirValue = null;
        try {
            originalUserDirValue = System.getProperty("user.dir");
            System.setProperty("user.dir", baseDir);
            devModeInitializer.onStartup(classes, servletContext);
        } finally {
            if (originalUserDirValue != null) {
                System.setProperty("user.dir", originalUserDirValue);
            }
        }
    }

    @Test
    public void onStartup_fallbackBaseDirIsGradleProjectDirectory_isAccepted()
            throws Exception {
        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(null);
        TemporaryFolder tmp = new TemporaryFolder();
        tmp.create();
        tmp.newFile("build.gradle");
        baseDir = tmp.getRoot().getPath();

        String originalUserDirValue = null;
        try {
            originalUserDirValue = System.getProperty("user.dir");
            System.setProperty("user.dir", baseDir);
            devModeInitializer.onStartup(classes, servletContext);
        } finally {
            if (originalUserDirValue != null) {
                System.setProperty("user.dir", originalUserDirValue);
            }
        }
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

        // Create mock loader with the single jar to be found
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(classLoader.getResources(resourcesFolder))
                .thenReturn(Collections.enumeration(urls));

        // load jars from classloader
        List<File> jarFilesFromClassloader = new ArrayList<>(DevModeInitializer
                .getFrontendLocationsFromClassloader(classLoader));

        // Assert that jar was found and accepted
        assertEquals("One jar should have been found and added as a File", 1,
                jarFilesFromClassloader.size());
        // Assert that the file can be found from the filesystem by the given
        // path.
        assertTrue("File in path 'with space' doesn't load from given path",
                jarFilesFromClassloader.get(0).exists());
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
        // Create mock loader with the single jar to be found
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(classLoader.getResources(resourcesFolder))
                .thenReturn(Collections.enumeration(urls));

        // load jars from classloader
        List<File> locations = new ArrayList<>(DevModeInitializer
                .getFrontendLocationsFromClassloader(classLoader));

        // Assert that resource was found and accepted
        assertEquals("One resource should have been found and added as a File",
                1, locations.size());
        // Assert that the file can be found from the filesystem by the given
        // path.
        assertTrue("Resource doesn't load from given path",
                locations.get(0).exists());
    }

}
