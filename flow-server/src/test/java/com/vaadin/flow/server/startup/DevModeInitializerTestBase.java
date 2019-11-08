package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.server.DevModeHandler;
import com.vaadin.flow.server.DevModeHandlerTest;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.Constants.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubNode;
import static com.vaadin.flow.server.frontend.NodeUpdateTestUtil.createStubWebpackServer;

/**
 * Base class for DevModeInitializer tests. It is an independent class so as it
 * can be created and executed with custom classloaders.
 */
public class DevModeInitializerTestBase {

    DevModeInitializer devModeInitializer;

    // These fields are intentionally scoped default so
    // as they can be used in package tests
    ServletContext servletContext;
    Map<String, String> initParams;
    Set<Class<?>> classes;
    File mainPackageFile;
    File appPackageFile;
    File webpackFile;
    String baseDir;

    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {
        temporaryFolder.create();
        baseDir = temporaryFolder.getRoot().getPath();

        createStubNode(false, true, baseDir);
        createStubWebpackServer("Compiled", 0, baseDir);

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration registration = Mockito
                .mock(ServletRegistration.class);

        initParams = new HashMap<>();
        initParams.put(FrontendUtils.PROJECT_BASEDIR, baseDir);

        Mockito.when(registration.getInitParameters()).thenReturn(initParams);

        classes = new HashSet<>();
        classes.add(this.getClass());

        Map registry = new HashMap();
        registry.put("foo", registration);
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletContext.getClassLoader())
                .thenReturn(this.getClass().getClassLoader());

        mainPackageFile = new File(baseDir, PACKAGE_JSON);
        appPackageFile = new File(baseDir,
                DEFAULT_GENERATED_DIR + PACKAGE_JSON);
        webpackFile = new File(baseDir, WEBPACK_CONFIG);
        appPackageFile.getParentFile().mkdirs();

        FileUtils.write(mainPackageFile, "{}", "UTF-8");
        FileUtils.write(appPackageFile, "{}", "UTF-8");
        webpackFile.createNewFile();

        // Default is Bower Mode, change to Npm Mode
        System.setProperty("vaadin." + SERVLET_PARAMETER_COMPATIBILITY_MODE,
                "false");

        devModeInitializer = new DevModeInitializer();
    }

    @After
    public void teardown() throws Exception, SecurityException {
        System.clearProperty("vaadin." + SERVLET_PARAMETER_COMPATIBILITY_MODE);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_PRODUCTION_MODE);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_REUSE_DEV_SERVER);

        webpackFile.delete();
        mainPackageFile.delete();
        appPackageFile.delete();
        temporaryFolder.delete();
        if (DevModeHandler.getDevModeHandler() != null) {
            DevModeHandler.getDevModeHandler().removeRunningDevServerPort();
        }
        DevModeHandlerTest.removeDevModeHandlerInstance();
    }

    public void runOnStartup() throws Exception {
        devModeInitializer.onStartup(classes, servletContext);
    }

    public void runDestroy() throws Exception {
        devModeInitializer.contextDestroyed(null);
    }

    static List<URL> getClasspathURLs() {
        return Arrays.stream(
                System.getProperty("java.class.path").split(File.pathSeparator))
                .map(s -> {
                    try {
                        return new File(s).toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    public DevModeHandler getDevModeHandler() {
        return DevModeHandler.getDevModeHandler();
    }
}
