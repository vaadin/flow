package com.vaadin.base.devserver.startup;

import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.PACKAGE_JSON;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_REUSE_DEV_SERVER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_JAVA_SOURCE_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.WEBPACK_CONFIG;
import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;
import static com.vaadin.flow.testutil.FrontendStubs.createStubWebpackServer;
import static org.mockito.ArgumentMatchers.any;

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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRegistration;

import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.TaskGenerateFusion;
import com.vaadin.flow.server.frontend.TaskGenerateOpenAPI;

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Base class for DevModeInitializer tests. It is an independent class so as it
 * can be created and executed with custom classloaders.
 */
public class DevModeInitializerTestBase extends AbstractDevModeTest {

    DevModeInitializer devModeInitializer;

    // These fields are intentionally scoped default so
    // as they can be used in package tests
    Set<Class<?>> classes;
    File mainPackageFile;
    File webpackFile;
    EndpointGeneratorTaskFactory endpointGeneratorTaskFactory;
    TaskGenerateFusion taskGenerateFusion;
    TaskGenerateOpenAPI taskGenerateOpenAPI;

    @Rule
    public final TemporaryFolder javaSourceFolder = new TemporaryFolder();

    public static class VaadinServletSubClass extends VaadinServlet {

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setup() throws Exception {
        super.setup();

        createStubNode(false, true, baseDir);
        createStubWebpackServer("Compiled", 500, baseDir, true);

        // Prevent TaskRunNpmInstall#cleanUp from deleting node_modules
        new File(baseDir, "node_modules/.modules.yaml").createNewFile();

        ServletRegistration vaadinServletRegistration = Mockito
                .mock(ServletRegistration.class);

        Mockito.when(vaadinServletRegistration.getClassName())
                .thenReturn(VaadinServletSubClass.class.getName());
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

        classes = new HashSet<>();
        classes.add(this.getClass());

        Map registry = new HashMap();

        /*
         * Adding extra registrations to make sure that DevModeInitializer picks
         * the correct registration which is a VaadinServlet registration.
         */
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

    @Override
    public void teardown() {
        super.teardown();
        System.clearProperty("vaadin." + SERVLET_PARAMETER_PRODUCTION_MODE);
        System.clearProperty("vaadin." + SERVLET_PARAMETER_REUSE_DEV_SERVER);
        System.clearProperty("vaadin." + CONNECT_JAVA_SOURCE_FOLDER_TOKEN);

        webpackFile.delete();
        mainPackageFile.delete();
    }

    public void process() throws Exception {
        devModeInitializer.process(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevServer();
    }

    public void runDestroy() throws Exception {
        ServletContextEvent event = Mockito.mock(ServletContextEvent.class);
        Mockito.when(event.getServletContext()).thenReturn(servletContext);
        devModeInitializer.contextDestroyed(event);
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
