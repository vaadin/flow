package com.vaadin.flow.server.startup;

import java.util.Arrays;
import java.util.EventListener;
import java.util.HashSet;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.startup.DevModeInitializer.VisitedClasses;

import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DevModeInitializerTest {

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
    public ExpectedException exception = ExpectedException.none();
    private DevModeInitializerTestBase baseInitializer = new DevModeInitializerTestBase();

    @Before
    public void setup() throws Exception {
        baseInitializer.setup();
    }

    @After
    public void teardown() throws Exception, SecurityException {
        baseInitializer.teardown();
    }

    @Test
    public void should_Run_Updaters() throws Exception {
        baseInitializer.runOnStartup();
        assertNotNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoNodeConfFiles()
            throws Exception {
        baseInitializer.webpackFile.delete();
        baseInitializer.mainPackageFile.delete();
        baseInitializer.appPackageFile.delete();
        baseInitializer.runOnStartup();
        assertNotNull(baseInitializer.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_when_NoMainPackageFile()
            throws Exception {
        baseInitializer.mainPackageFile.delete();
        assertNull(baseInitializer.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoAppPackageFile()
            throws Exception {
        baseInitializer.appPackageFile.delete();
        baseInitializer.runOnStartup();
        assertNotNull(baseInitializer.getDevModeHandler());
    }

    @Test
    public void should_Run_Updaters_when_NoWebpackFile() throws Exception {
        baseInitializer.webpackFile.delete();
        baseInitializer.runOnStartup();
        assertNotNull(baseInitializer.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_inBowerMode() throws Exception {
        System.clearProperty("vaadin." + SERVLET_PARAMETER_COMPATIBILITY_MODE);
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(baseInitializer.classes, baseInitializer.servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_Run_Updaters_inProductionMode() throws Exception {
        System.setProperty("vaadin." + SERVLET_PARAMETER_PRODUCTION_MODE,
                "true");
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(baseInitializer.classes, baseInitializer.servletContext);
        assertNull(DevModeHandler.getDevModeHandler());
    }

    @Test
    public void should_Not_AddContextListener() throws Exception {
        ArgumentCaptor<? extends EventListener> arg = ArgumentCaptor.forClass(EventListener.class);
        baseInitializer.runOnStartup();
        Mockito.verify(baseInitializer.servletContext, Mockito.never()).addListener(arg.capture());
    }

    @Test
    public void listener_should_stopDevModeHandler_onDestroy() throws Exception {
        baseInitializer.initParams.put(SERVLET_PARAMETER_REUSE_DEV_SERVER, "false");

        baseInitializer.runOnStartup();

        assertNotNull(DevModeHandler.getDevModeHandler());

        baseInitializer.runDestroy();

        assertNull(DevModeHandler.getDevModeHandler());
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
