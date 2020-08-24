package com.vaadin.flow.server.startup;

import javax.servlet.ServletException;

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.Maps;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.connect.generator.VaadinConnectClientGenerator;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.RESOURCES_FRONTEND_DEFAULT;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_GENERATED_TS_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
            throws IOException, ServletException {
        loadingJars_allFilesExist(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingZipProtocolJars_useModernResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingZipProtocolJars_allFilesExist(RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingJars_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingJars_allFilesExist(COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_useModernResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingFsResources_allFilesExist("/dir-with-modern-frontend/",
                RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingFsResources_allFilesExist("/dir-with-frontend-resources/",
                COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_usesVfsProtocol_allFilesExist()
            throws Exception {
        String path = Paths
                .get("/dir-with-modern-frontend", RESOURCES_FRONTEND_DEFAULT)
                .toString();
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
        URL url = new URL(
                Paths.get("vfs://some-non-existent-place", path).toString());

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
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoNodeConfFiles() throws Exception {
        webpackFile.delete();
        mainPackageFile.delete();
        process();
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoMainPackageFile()
            throws Exception {
        assertNull(DevModeHandler.getDevModeHandler());
        mainPackageFile.delete();
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoAppPackageFile() throws Exception {
        process();
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoWebpackFile() throws Exception {
        webpackFile.delete();
        process();
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_inProductionMode() throws Exception {
        System.setProperty("vaadin." + SERVLET_PARAMETER_PRODUCTION_MODE,
                "true");
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
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
        initParams.put(SERVLET_PARAMETER_REUSE_DEV_SERVER, "false");

        process();

        assertNotNull(DevModeHandler.getDevModeHandler());

        runDestroy();

        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void shouldUseByteCodeScannerIfPropertySet() throws Exception {
        initParams.put(Constants.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE,
                "true");
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
        Mockito.verify(servletContext, Mockito.times(0)).setAttribute(
                Mockito.eq(FallbackChunk.class.getName()),
                Mockito.any(FallbackChunk.class));
    }

    @Test
    public void should_generateOpenApi_when_EndpointPresents()
            throws Exception {

        // Configure a folder that has .java classes with valid endpoints
        // Not using `src/test/java` because there are invalid endpoint names
        // in some tests
        File src = new File(
                getClass().getClassLoader().getResource("java").getFile());
        System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                src.getAbsolutePath());

        File generatedOpenApiJson = Paths
                .get(baseDir, DEFAULT_CONNECT_OPENAPI_JSON_FILE).toFile();

        Assert.assertFalse(generatedOpenApiJson.exists());
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(classes, servletContext);
        waitForDevModeServer();
        Assert.assertTrue("Should generate OpenAPI spec if Endpoint is used.",
                generatedOpenApiJson.exists());
    }

    @Test
    public void should_notGenerateOpenApi_when_EndpointIsNotUsed()
            throws Exception {
        File generatedOpenApiJson = Paths
                .get(baseDir, DEFAULT_CONNECT_OPENAPI_JSON_FILE).toFile();
        Assert.assertFalse(generatedOpenApiJson.exists());
        devModeInitializer.onStartup(classes, servletContext);
        Assert.assertFalse(
                "Should not generate OpenAPI spec if Endpoint is not used.",
                generatedOpenApiJson.exists());
    }

    @Test
    public void should_generateTs_files() throws Exception {

        // Configure a folder that has .java classes with valid endpoints
        // Not using `src/test/java` because there are invalid endpoint names
        // in some tests
        File src = new File(
                getClass().getClassLoader().getResource("java").getFile());
        System.setProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN,
                src.getAbsolutePath());

        DevModeInitializer devModeInitializer = new DevModeInitializer();

        File ts1 = new File(baseDir,
                DEFAULT_CONNECT_GENERATED_TS_DIR + "MyEndpoint.ts");
        File ts2 = new File(baseDir, DEFAULT_CONNECT_GENERATED_TS_DIR
                + VaadinConnectClientGenerator.CONNECT_CLIENT_NAME);

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());
        devModeInitializer.onStartup(classes, servletContext);
        waitForDevModeServer();
        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
    }

    @Test
    public void onStartup_emptyServletRegistrations_shouldCreateDevModeHandler()
            throws Exception {
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(Collections.emptyMap());
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.enumeration(new HashSet<>(
                        Arrays.asList(Constants.SERVLET_PARAMETER_ENABLE_PNPM,
                                FrontendUtils.PROJECT_BASEDIR))));
        Mockito.when(
                servletContext.getInitParameter(FrontendUtils.PROJECT_BASEDIR))
                .thenReturn(initParams.get(FrontendUtils.PROJECT_BASEDIR));
        Mockito.when(servletContext
                .getInitParameter(Constants.SERVLET_PARAMETER_ENABLE_PNPM))
                .thenReturn(initParams
                        .get(Constants.SERVLET_PARAMETER_ENABLE_PNPM));
        devModeInitializer.onStartup(classes, servletContext);
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void onStartup_devModeAlreadyStarted_shouldBeTrueWhenStarted() throws Exception {
        final Map<String, Object> servletContextAttributes = Maps.newHashMap();
        Mockito.doAnswer(answer -> {
            String key = answer.getArgumentAt(0, String.class);
            Object value = answer.getArgumentAt(1, Object.class);
            servletContextAttributes.putIfAbsent(key, value);
            return null;
        })
                .when(servletContext)
                .setAttribute(Mockito.anyString(), Mockito.anyObject());
        Mockito.when(servletContext.getAttribute(Mockito.anyString()))
                .thenAnswer(answer ->
                        servletContextAttributes.get(answer.getArgumentAt(0, String.class)));
        process();
        assertTrue(DevModeInitializer.isDevModeAlreadyStarted(servletContext));
    }

    @Test(expected = IllegalStateException.class)
    public void onStartup_fallbackBaseDirIsNotProjectDirectory_throws()
            throws Exception {
        initParams.remove(FrontendUtils.PROJECT_BASEDIR);
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
        initParams.remove(FrontendUtils.PROJECT_BASEDIR);
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
        initParams.remove(FrontendUtils.PROJECT_BASEDIR);
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
            throws IOException, ServletException {
        loadingJarsWithProtocol_allFilesExist(resourcesFolder, s -> {
            try {
                return new URL("jar:" + s);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void loadingZipProtocolJars_allFilesExist(String resourcesFolder)
            throws IOException, ServletException {
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
            throws IOException, ServletException {
        // Create jar urls with the given urlBuilder for test
        String urlPath = this.getClass().getResource("/").toString()
                .replace("target/test-classes/", "")
                + "src/test/resources/with%20space/jar-with-frontend-resources.jar!/META-INF/resources/frontend";
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
            String resourcesFolder) throws IOException, ServletException {
        List<URL> urls = Collections.singletonList(
                getClass().getResource(resourcesRoot + resourcesFolder));
        loadingFsResources_allFilesExist(urls, resourcesFolder);
    }

    private void loadingFsResources_allFilesExist(Collection<URL> urls,
            String resourcesFolder) throws IOException, ServletException {
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
