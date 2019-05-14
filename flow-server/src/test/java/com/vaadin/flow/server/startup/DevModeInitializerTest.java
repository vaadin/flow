package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.startup.DevModeInitializer.VisitedClasses;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.FrontendUtils.getBaseDir;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubNode;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


@NotThreadSafe
public class DevModeInitializerTest {
    private ServletContext servletContext;
    private DevModeInitializer devModeInitializer;
    private Set<Class<?>> classes;

    private File mainPackageFile = new File(getBaseDir(), PACKAGE_JSON);
    private File webpackFile = new File(getBaseDir(), WEBPACK_CONFIG);
    private File appPackageFile = new File(getBaseDir(), DEFAULT_GENERATED_DIR + PACKAGE_JSON);

    @JsModule("foo")
    public static class Visited {

    }

    public static class NotVisitedWithoutDeps {

    }

    @JsModule("foo")
    public static class NotVisitedWithDeps {

    }

    public static class WithoutDepsSubclass extends NotVisitedWithoutDeps {

    }

    public static class WithDepsSubclass extends NotVisitedWithDeps {

    }

    public static class VisitedSubclass extends Visited {

    }

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {

        System.setProperty("user.dir", temporaryFolder.getRoot().getPath());

        createStubNode(false, true);
        createStubWebpackServer("Compiled", 1500);

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration registration = Mockito.mock(ServletRegistration.class);
        classes = new HashSet<>();

        Map registry = new HashMap();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());


        mainPackageFile = new File(getBaseDir(), PACKAGE_JSON);
        appPackageFile = new File(getBaseDir(), DEFAULT_GENERATED_DIR + PACKAGE_JSON);
        webpackFile = new File(getBaseDir(), WEBPACK_CONFIG);
        appPackageFile.getParentFile().mkdirs();

        devModeInitializer = new DevModeInitializer();
    }

    @After
    public void teardown() throws Exception, SecurityException {
        System.clearProperty(
                "vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM);
        System.clearProperty(
                "vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS);

        webpackFile.delete();
        mainPackageFile.delete();
        appPackageFile.delete();

        // Reset unique instance in DevModeHandler
        Field atomicHandler = DevModeHandler.class
                .getDeclaredField("atomicHandler");
        atomicHandler.setAccessible(true);
        atomicHandler.set(null, new AtomicReference<>());
    }

    @Test
    public void should_Not_Run_Updaters_when_Disabled() throws Exception {
        System.setProperty(
                "vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_NPM, "true");
        System.setProperty(
                "vaadin." + SERVLET_PARAMETER_DEVMODE_SKIP_UPDATE_IMPORTS,
                "true");
        devModeInitializer.onStartup(classes, servletContext);
        assertFalse(mainPackageFile.canRead());
        assertFalse(webpackFile.canRead());
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoNodeConfFiles() throws Exception {
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoMainPackageFile() throws Exception {
        appPackageFile.createNewFile();
        webpackFile.createNewFile();
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoAppPackageFile() throws Exception {
        mainPackageFile.createNewFile();
        webpackFile.createNewFile();
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoWebpackFile() throws Exception {
        mainPackageFile.createNewFile();
        appPackageFile.createNewFile();
        devModeInitializer.onStartup(classes, servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
    }


    @Test
    public void should_Run_Updaters_when_NodeAllConfFiles() throws Exception {
        FileUtils.write(mainPackageFile, "{}", "UTF-8");
        FileUtils.write(appPackageFile, "{}", "UTF-8");
        webpackFile.createNewFile();
        devModeInitializer.onStartup(classes, servletContext);
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void visitedDependencies() {
        VisitedClasses visited = new VisitedClasses(new HashSet<>(Arrays
                .asList(Object.class.getName(), Visited.class.getName())));

        assertTrue("Dependencies are ok for a visited class",
                visited.allDependenciesVisited(Visited.class));

        assertTrue(
                "Dependencies are ok for an unvisited class without dependencies",
                visited.allDependenciesVisited(NotVisitedWithoutDeps.class));
        assertFalse(
                "Dependnecies are not ok for an unvisited class with dependencies",
                visited.allDependenciesVisited(NotVisitedWithDeps.class));

        assertTrue(
                "Dependencies are ok for an unvisited class without dependencies if super class is ok",
                visited.allDependenciesVisited(VisitedSubclass.class));
        assertTrue(
                "Dependencies are ok for an unvisited class without dependencies if super class is ok",
                visited.allDependenciesVisited(WithoutDepsSubclass.class));
        assertFalse(
                "Dependencies are  not ok for an unvisited class without dependencies if super class is not ok",
                visited.allDependenciesVisited(WithDepsSubclass.class));
    }

}
