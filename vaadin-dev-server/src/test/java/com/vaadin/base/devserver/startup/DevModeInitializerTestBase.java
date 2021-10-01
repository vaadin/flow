package com.vaadin.base.devserver.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.base.devserver.DevModeHandlerImpl;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.frontend.TaskGenerateFusion;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import elemental.json.Json;
import elemental.json.JsonObject;

import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.base.devserver.DevModeHandlerImpl.getDevModeHandler;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_JAVA_SOURCE_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;
import static com.vaadin.flow.testutil.FrontendStubs.createStubWebpackServer;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;

/**
 * Base class for DevModeInitializer tests. It is an independent class so as it
 * can be created and executed with custom classloaders.
 */
public class DevModeInitializerTestBase {

    DevModeInitializer devModeInitializer;

    // These fields are intentionally scoped default so
    // as they can be used in package tests
    ServletContext servletContext;
    Set<Class<?>> classes;
    File mainPackageFile;
    File webpackFile;
    String baseDir;
    Lookup lookup;
    EndpointGeneratorTaskFactory endpointGeneratorTaskFactory;
    TaskGenerateFusion taskGenerateFusion;
    TaskGenerateOpenAPI taskGenerateOpenAPI;

    ApplicationConfiguration appConfig;

    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final TemporaryFolder javaSourceFolder = new TemporaryFolder();

    public static class VaadinServletSubClass extends VaadinServlet {

    }

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {
        assertNull(getDevModeHandler());

        temporaryFolder.create();
        baseDir = temporaryFolder.getRoot().getPath();
        Boolean enablePnpm = Boolean.TRUE;

        appConfig = Mockito.mock(ApplicationConfiguration.class);
        mockApplicationConfiguration(appConfig, enablePnpm);

        createStubNode(false, true, baseDir);
        createStubWebpackServer("Compiled", 500, baseDir, true);

        // Prevent TaskRunNpmInstall#cleanUp from deleting node_modules
        new File(baseDir, "node_modules/.modules.yaml").createNewFile();

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration vaadinServletRegistration = Mockito
                .mock(ServletRegistration.class);

        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        lookup = Mockito.mock(Lookup.class);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);
        endpointGeneratorTaskFactory = Mockito
                .mock(EndpointGeneratorTaskFactory.class);
        taskGenerateFusion = Mockito.mock(TaskGenerateFusion.class);
        taskGenerateOpenAPI = Mockito.mock(TaskGenerateOpenAPI.class);
        Mockito.doReturn(endpointGeneratorTaskFactory).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);
        Mockito.doReturn(taskGenerateFusion).when(endpointGeneratorTaskFactory)
                .createTaskGenerateFusion(any(), any(), any(), any());
        Mockito.doReturn(taskGenerateOpenAPI).when(endpointGeneratorTaskFactory)
                .createTaskGenerateOpenAPI(any(), any(), any(), any());

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);

        Mockito.when(vaadinServletRegistration.getClassName())
                .thenReturn(VaadinServletSubClass.class.getName());

        classes = new HashSet<>();
        classes.add(this.getClass());

        Map registry = new HashMap();

        // Adding extra registrations to make sure that DevModeInitializer picks
        // the correct registration which is a VaadinServlet registration.
        registry.put("extra1", Mockito.mock(ServletRegistration.class));
        registry.put("foo", vaadinServletRegistration);
        registry.put("extra2", Mockito.mock(ServletRegistration.class));
        Mockito.when(servletContext.getServletRegistrations())
                .thenReturn(registry);
        Mockito.when(servletContext.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(servletContext.getClassLoader())
                .thenReturn(this.getClass().getClassLoader());

        mainPackageFile = new File(baseDir, PACKAGE_JSON);
        webpackFile = new File(baseDir, WEBPACK_CONFIG);

        // Not this needs to update according to dependencies in
        // NodeUpdater.getDefaultDependencies and
        // NodeUpdater.getDefaultDevDependencies
        FileUtils.write(mainPackageFile, getInitalPackageJson().toJson(),
                "UTF-8");
        webpackFile.createNewFile();
        FileUtils.forceMkdir(
                new File(baseDir, DEFAULT_CONNECT_JAVA_SOURCE_FOLDER));

        devModeInitializer = new DevModeInitializer();
    }

    private JsonObject getInitalPackageJson() {
        JsonObject packageJson = Json.createObject();
        JsonObject vaadinPackages = Json.createObject();

        vaadinPackages.put("dependencies", Json.createObject());
        JsonObject defaults = vaadinPackages.getObject("dependencies");
        defaults.put("@polymer/polymer", "3.2.0");

        vaadinPackages.put("devDependencies", Json.createObject());
        defaults = vaadinPackages.getObject("devDependencies");
        defaults.put("webpack", "4.30.0");
        defaults.put("webpack-cli", "3.3.0");
        defaults.put("webpack-dev-server", "3.3.0");
        defaults.put("webpack-babel-multi-target-plugin", "2.3.1");
        defaults.put("compression-webpack-plugin", "3.0.0");
        defaults.put("webpack-merge", "4.2.1");
        defaults.put("raw-loader", "3.0.0");

        vaadinPackages.put("hash", "");

        packageJson.put("vaadin", vaadinPackages);

        return packageJson;
    }

    @After
    public void teardown() throws Exception, SecurityException {
        System.clearProperty("vaadin." + SERVLET_PARAMETER_PRODUCTION_MODE);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_REUSE_DEV_SERVER);
        System.clearProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);

        webpackFile.delete();
        mainPackageFile.delete();
        temporaryFolder.delete();
        javaSourceFolder.delete();
        if (getDevModeHandler() != null) {
            getDevModeHandler().stop();
        }
    }

    public void process() throws Exception {
        devModeInitializer.process(classes, servletContext);
        waitForDevModeServer();
    }

    protected void waitForDevModeServer() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        DevModeHandlerImpl handler = DevModeHandlerImpl.getDevModeHandler();
        Assert.assertNotNull(handler);
        Method join = DevModeHandlerImpl.class.getDeclaredMethod("join");
        join.setAccessible(true);
        join.invoke(handler);
    }

    public void runDestroy() throws Exception {
        devModeInitializer.contextDestroyed(null);
    }

    private void mockApplicationConfiguration(
            ApplicationConfiguration appConfig, boolean enablePnpm) {
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);
        Mockito.when(appConfig.enableDevServer()).thenReturn(true);
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(enablePnpm);

        Mockito.when(appConfig.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.when(appConfig.getBooleanProperty(Mockito.anyString(),
                Mockito.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(baseDir);
        Mockito.when(appConfig.getBuildFolder()).thenReturn(TARGET);
        Mockito.when(appConfig.getFlowResourcesFolder()).thenReturn(
                Paths.get(TARGET, FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER)
                        .toString());
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

}
