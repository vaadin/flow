package com.vaadin.flow.server.startup;

import javax.servlet.ServletException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.frontend.FallbackChunk;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static org.junit.Assert.assertEquals;
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

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void loadingJars_useModernResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingJars_allFilesExist(Constants.RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingJars_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingJars_allFilesExist(
                Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_useModernResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingFsResources_allFilesExist("/dir-with-modern-frontend/",
                Constants.RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void loadingFsResources_useObsoleteResourcesFolder_allFilesExist()
            throws IOException, ServletException {
        loadingFsResources_allFilesExist("/dir-with-frontend-resources/",
                Constants.COMPATIBILITY_RESOURCES_FRONTEND_DEFAULT);
    }

    @Test
    public void should_Run_Updaters() throws Exception {
        runOnStartup();
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoNodeConfFiles() throws Exception {
        webpackFile.delete();
        mainPackageFile.delete();
        appPackageFile.delete();
        runOnStartup();
        assertNotNull(getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoMainPackageFile()
            throws Exception {
        mainPackageFile.delete();
        assertNull(getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoAppPackageFile() throws Exception {
        appPackageFile.delete();
        runOnStartup();
        assertNotNull(getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoWebpackFile() throws Exception {
        webpackFile.delete();
        runOnStartup();
        assertNotNull(getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_inBowerMode() throws Exception {
        System.setProperty("vaadin." + SERVLET_PARAMETER_COMPATIBILITY_MODE,
                "true");
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
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
        runOnStartup();
        Mockito.verify(servletContext, Mockito.never())
                .addListener(arg.capture());
    }

    @Test
    public void listener_should_stopDevModeHandler_onDestroy()
            throws Exception {
        initParams.put(SERVLET_PARAMETER_REUSE_DEV_SERVER, "false");

        runOnStartup();

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
        ArgumentCaptor<? extends FallbackChunk> arg = ArgumentCaptor
                .forClass(FallbackChunk.class);
        Mockito.verify(servletContext, Mockito.atLeastOnce()).setAttribute(
                Mockito.eq(FallbackChunk.class.getName()), arg.capture());
        FallbackChunk fallbackChunk = arg.getValue();
        Assert.assertFalse(fallbackChunk.getModules().contains("foo"));

        Assert.assertTrue(fallbackChunk.getModules().contains("bar"));
    }

    @Test
    public void initDevModeHandler_usePolymerVersion() throws Exception {
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        initParams.put(Constants.SERVLET_PARAMETER_DEVMODE_POLYMER_VERSION,
                "3.3.0");
        devModeInitializer.onStartup(Collections.emptySet(), servletContext);

        String packageJson = Files
                .readAllLines(mainPackageFile.toPath(), StandardCharsets.UTF_8)
                .stream().collect(Collectors.joining());
        JsonObject json = Json.parse(packageJson);
        JsonObject deps = json.get("dependencies");
        Assert.assertEquals("3.3.0", deps.getString("@polymer/polymer"));
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

    private void loadingJars_allFilesExist(String resourcesFolder)
            throws IOException, ServletException {
        // Create jar urls for test
        URL jar = new URL("jar:"
                + this.getClass().getResource("/").toString()
                        .replace("target/test-classes/", "")
                + "src/test/resources/with%20space/jar-with-frontend-resources.jar!/META-INF/resources/frontend");
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
