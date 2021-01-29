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
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.fusion.frontend.EndpointGeneratorTaskFactoryImpl;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import static com.vaadin.flow.testutil.FrontendStubs.createStubNode;
import static com.vaadin.flow.testutil.FrontendStubs.createStubWebpackServer;
import static com.vaadin.flow.server.Constants.CONNECT_JAVA_SOURCE_FOLDER_TOKEN;
import static com.vaadin.flow.server.Constants.TARGET;
import static com.vaadin.flow.server.InitParameters.SERVLET_PARAMETER_DEVMODE_OPTIMIZE_BUNDLE;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_JAVA_SOURCE_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_CONNECT_OPENAPI_JSON_FILE;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_GENERATED_DIR;
import static com.vaadin.flow.server.frontend.FrontendUtils.DEFAULT_PROJECT_FRONTEND_GENERATED_DIR;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@NotThreadSafe
public class DevModeInitializerEndpointTest {

    String baseDir;
    ServletContext servletContext;
    Set<Class<?>> classes;
    DevModeInitializer devModeInitializer;
    private ApplicationConfiguration appConfig;

    private final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static class VaadinServletSubClass extends VaadinServlet {

    }

    @Before
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setup() throws Exception {
        assertNull("No DevModeHandler should be available at test start",
                DevModeHandlerImpl.getDevModeHandler());

        temporaryFolder.create();
        baseDir = temporaryFolder.getRoot().getPath();
        Boolean enablePnpm = Boolean.TRUE;

        appConfig = Mockito.mock(ApplicationConfiguration.class);
        mockApplicationConfiguration(appConfig, enablePnpm);

        createStubNode(false, true, baseDir);
        createStubWebpackServer(DevModeHandlerImplTest.COMPILE_OK_OUTPUT, 500, baseDir, true);

        // Prevent TaskRunNpmInstall#cleanUp from deleting node_modules
        new File(baseDir, "node_modules/.modules.yaml").createNewFile();

        servletContext = Mockito.mock(ServletContext.class);
        ServletRegistration vaadinServletRegistration = Mockito
                .mock(ServletRegistration.class);

        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        Mockito.doReturn(new EndpointGeneratorTaskFactoryImpl()).when(lookup)
                .lookup(EndpointGeneratorTaskFactory.class);

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

        FileUtils.forceMkdir(
                new File(baseDir, DEFAULT_CONNECT_JAVA_SOURCE_FOLDER));

        devModeInitializer = new DevModeInitializer();
    }

    @After
    public void teardown() throws Exception {
        final DevModeHandlerImpl devModeHandler = DevModeHandlerImpl
                .getDevModeHandler();
        if (devModeHandler != null) {
            devModeHandler.stop();
            // Wait until dev mode handler has stopped.
            while (DevModeHandlerImpl.getDevModeHandler() != null) {
                Thread.sleep(200); // NOSONAR
            }
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
        waitForDevModeServer();
        Assert.assertTrue("Should generate OpenAPI spec if Endpoint is used.",
                generatedOpenApiJson.exists());
    }

    @Test
    public void should_notGenerateOpenApi_when_EndpointIsNotUsed()
            throws Exception {
        File generatedOpenApiJson = Paths
                .get(baseDir, TARGET, DEFAULT_CONNECT_OPENAPI_JSON_FILE)
                .toFile();

        Assert.assertFalse(generatedOpenApiJson.exists());
        devModeInitializer.onStartup(classes, servletContext);
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
        waitForDevModeServer();
        assertTrue(ts1.exists());
        assertTrue(ts2.exists());
    }

    private void waitForDevModeServer() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        DevModeHandlerImpl handler = DevModeHandlerImpl.getDevModeHandler();
        Assert.assertNotNull(handler);
        Method join = DevModeHandlerImpl.class.getDeclaredMethod("join");
        join.setAccessible(true);
        join.invoke(handler);
    }

    private ServletContext mockServletContext() {
        ServletContext context = Mockito.mock(ServletContext.class);
        Mockito.when(
                context.getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);
        return context;
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
