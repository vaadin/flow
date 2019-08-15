package com.vaadin.flow.server.startup;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.server.frontend.TestUtils;

import static com.vaadin.flow.server.frontend.FrontendUtils.FLOW_NPM_PACKAGE_NAME;
import static com.vaadin.flow.server.frontend.FrontendUtils.NODE_MODULES;

public class DevModeInitializerClassLoaderTest {

    @Test
    public void should_loadResources_from_customClassLoader() throws Exception {

        // Get a list of all the URLs in the classPath
        List<URL> path = DevModeInitializerTestBase.getClasspathURLs();
        // Add a couple of locations with test resources
        path.add(TestUtils.getTestJar("jar-with-frontend-resources.jar").toURI()
                .toURL());
        path.add(TestUtils.getTestFolder("dir-with-frontend-resources/").toURI()
                .toURL());

        // Create our custom classLoader
        URLClassLoader customLoader = new URLClassLoader(
                path.toArray(new URL[0]), null);

        // Load the base class with the custom loader
        Class<?> clz = customLoader
                .loadClass(DevModeInitializerTestBase.class.getName());
        Object initializer = clz.newInstance();

        // Since base class was created using a different classLoader,
        // its methods and fields need to be called using reflection
        Method method = clz.getMethod("setup");
        method.invoke(initializer);

        method = clz.getMethod("runOnStartup");
        method.invoke(initializer);

        Field field = clz.getDeclaredField("baseDir");
        field.setAccessible(true);
        String baseDir = field.get(initializer).toString();

        method = clz.getMethod("runDestroy");
        method.invoke(initializer);

        customLoader.close();

        List<String> files = TestUtils.listFilesRecursively(
                new File(baseDir, NODE_MODULES + FLOW_NPM_PACKAGE_NAME));
        Assert.assertEquals(3, files.size());

        Assert.assertTrue("Js resource should have been copied from jar file",
                files.contains("ExampleConnector.js"));

        Assert.assertTrue("Css resource should have been copied from jar file",
                files.contains("inline.css"));

        Assert.assertTrue(
                "Js resource should have been copied from resource folder",
                files.contains("resourceInFolder.js"));
    }

}
