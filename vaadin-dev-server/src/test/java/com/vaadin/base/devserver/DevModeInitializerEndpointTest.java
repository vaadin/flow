package com.vaadin.base.devserver;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import com.vaadin.base.devserver.startup.DevModeInitializer;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.fusion.frontend.EndpointGeneratorTaskFactoryImpl;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;
import static com.vaadin.flow.testutil.FrontendStubs.createStubWebpackServer;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_JAVA_SOURCE_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DevModeInitializerEndpointTest {

    String baseDir;
    ServletContext servletContext;
    Set<Class<?>> classes;
    DevModeInitializer devModeInitializer;
    private ApplicationConfiguration appConfig;
    DevModeHandlerManager devModeHandlerManager;

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private DevModeHandler handler;

    private static class VaadinServletSubClass extends VaadinServlet {

    }

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {
        temporaryFolder.create();
        baseDir = temporaryFolder.getRoot().getPath();
        Boolean enablePnpm = Boolean.TRUE;

        appConfig = Mockito.mock(ApplicationConfiguration.class);
        mockApplicationConfiguration(appConfig, enablePnpm);
        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        assertFalse("No DevModeHandler should be available at test start",
                DevModeHandlerManager
                        .getDevModeHandler(
                                new VaadinServletContext(servletContext))
                        .isPresent());

        createStubNode(false, true, baseDir);
        createStubWebpackServer("Compiled", 500, baseDir, true);

        // Prevent TaskRunNpmInstall#cleanUp from deleting node_modules
        new File(baseDir, "node_modules/.modules.yaml").createNewFile();

        ServletRegistration vaadinServletRegistration = Mockito
                .mock(ServletRegistration.class);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        Mockito.doReturn(new EndpointGeneratorTaskFactoryImpl()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        devModeHandlerManager = new DevModeHandlerManagerImpl();
        Mockito.when(lookup.lookup(DevModeHandlerManager.class))
                .thenReturn(devModeHandlerManager);

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

        FileUtils.forceMkdir(
                new File(baseDir, DEFAULT_CONNECT_JAVA_SOURCE_FOLDER));

        devModeInitializer = new DevModeInitializer();
    }

    @After
    public void teardown() throws Exception {
        if (handler != null) {
            handler.stop();
            handler = null;
        }

        temporaryFolder.delete();
    }

    @Test
    public void should_generateOpenApi_when_EndpointPresents()
            throws Exception {
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, DEFAULT_CONNECT_OPENAPI_JSON_FILE)
                .toFile();
        File src = new File(
                getClass().getClassLoader().getResource("java").getFile());
        Mockito.when(appConfig.getStringProperty(
                Mockito.eq(CONNECT_JAVA_SOURCE_FOLDER_TOKEN),
                Mockito.anyString())).thenReturn(src.getAbsolutePath());

        Assert.assertFalse(generatedOpenApiJson.exists());
        DevModeInitializer devModeInitializer = new DevModeInitializer();
        devModeInitializer.onStartup(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevModeServer();
        Assert.assertTrue("Should generate OpenAPI spec if Endpoint is used.",
                generatedOpenApiJson.exists());
    }

    private DevModeHandler getDevModeHandler() {
        Optional<DevModeHandler> maybeHandler = DevModeHandlerManager
                .getDevModeHandler(new VaadinServletContext(servletContext));
        if (!maybeHandler.isPresent()) {
            throw new IllegalStateException(
                    "No dev mode handler found in context");
        }
        return maybeHandler.get();

    }

    @Test
    public void should_notGenerateOpenApi_when_EndpointIsNotUsed()
            throws Exception {
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, DEFAULT_CONNECT_OPENAPI_JSON_FILE)
                .toFile();

        Assert.assertFalse(generatedOpenApiJson.exists());
        devModeInitializer.onStartup(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevModeServer();
        Assert.assertFalse(
                "Should not generate OpenAPI spec if Endpoint is not used.",
                generatedOpenApiJson.exists());
    }

    @Test
    public void should_generateTs_files() throws Exception {
        // Configure a folder that has .java classes with valid endpoints
        // Not using `src/test/java` because there are invalid endpoint
        // names
        // in some tests
        File src = new File(
                getClass().getClassLoader().getResource("java").getFile());
        Mockito.when(appConfig.getStringProperty(
                Mockito.eq(CONNECT_JAVA_SOURCE_FOLDER_TOKEN),
                Mockito.anyString())).thenReturn(src.getAbsolutePath());

        DevModeInitializer devModeInitializer = new DevModeInitializer();

        File ts1 = new File(baseDir,
                DEFAULT_PROJECT_FRONTEND_GENERATED_DIR + "MyEndpoint.ts");
        File ts2 = new File(baseDir, DEFAULT_PROJECT_FRONTEND_GENERATED_DIR
                + "connect-client.default.ts");

        assertFalse(ts1.exists());
        assertFalse(ts2.exists());
        devModeInitializer.onStartup(classes, servletContext);
        handler = getDevModeHandler();
        waitForDevModeServer();
        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
    }

    private void waitForDevModeServer() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Assert.assertNotNull(handler);
        Method join = WebpackHandler.class.getDeclaredMethod("join");
        join.setAccessible(true);
        join.invoke(handler);
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

}
