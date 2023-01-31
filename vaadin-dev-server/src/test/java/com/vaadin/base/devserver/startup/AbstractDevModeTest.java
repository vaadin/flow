package com.vaadin.base.devserver.startup;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import com.vaadin.base.devserver.AbstractDevServerRunner;
import com.vaadin.base.devserver.DevModeHandlerManagerImpl;
import com.vaadin.base.devserver.MockDeploymentConfiguration;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import jakarta.servlet.ServletContext;

public abstract class AbstractDevModeTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    protected ApplicationConfiguration appConfig;
    protected ServletContext servletContext;
    protected Lookup lookup;
    protected DevModeHandlerManager devModeHandlerManager;
    protected DevModeHandler handler;
    protected String baseDir;
    protected File npmFolder;
    protected MockDeploymentConfiguration configuration;
    protected VaadinService vaadinService;
    protected VaadinServletContext vaadinContext;

    @Before
    public void setup() throws Exception {
        Field firstMapping = VaadinServlet.class
                .getDeclaredField("frontendMapping");
        firstMapping.setAccessible(true);
        firstMapping.set(null, "/fake-test-mapping");
        baseDir = temporaryFolder.getRoot().getPath();
        npmFolder = temporaryFolder.getRoot();

        Boolean enablePnpm = Boolean.TRUE;
        appConfig = Mockito.spy(ApplicationConfiguration.class);

        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);
        Mockito.when(servletContext.getClassLoader())
                .thenReturn(servletContext.getClass().getClassLoader());
        Mockito.when(servletContext.getContextPath()).thenReturn("");

        vaadinContext = new VaadinServletContext(servletContext);

        mockApplicationConfiguration(appConfig, enablePnpm);

        lookup = Mockito.mock(Lookup.class);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        setupMockResourceProvider(resourceProvider);
        devModeHandlerManager = new DevModeHandlerManagerImpl();
        Mockito.when(lookup.lookup(DevModeHandlerManager.class))
                .thenReturn(devModeHandlerManager);

        configuration = new MockDeploymentConfiguration();
        Mockito.when(lookup.lookup(DeploymentConfiguration.class))
                .thenReturn(configuration);
        Mockito.when(lookup.lookup(ApplicationConfiguration.class))
                .thenReturn(appConfig);
        Mockito.when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(service -> new StaticFileServer(service));

        vaadinService = Mockito.mock(VaadinService.class);

        Mockito.when(vaadinService.getContext()).thenReturn(vaadinContext);
        Mockito.when(vaadinService.getDeploymentConfiguration())
                .thenReturn(configuration);

    }

    @After
    public void teardown() {
        if (handler != null) {
            handler.stop();
            handler = null;
        }

    }

    protected void setupMockResourceProvider(
            ResourceProvider mockResourceProvider) throws IOException {

    }

    private void mockApplicationConfiguration(
            ApplicationConfiguration appConfig, boolean enablePnpm) {
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);
        Mockito.when(appConfig.frontendHotdeploy()).thenReturn(true);
        Mockito.when(appConfig.isPnpmEnabled()).thenReturn(enablePnpm);

        Mockito.when(appConfig.getStringProperty(Mockito.anyString(),
                Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        Mockito.when(appConfig.getBooleanProperty(Mockito.anyString(),
                Mockito.anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        Mockito.when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR,
                null)).thenReturn(baseDir);
        Mockito.when(appConfig.getBuildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections.emptyList()));
        Mockito.when(appConfig.getContext()).thenReturn(vaadinContext);
    }

    protected DevModeHandler getDevModeHandler() {
        return devModeHandlerManager.getDevModeHandler();
    }

    protected int getDevServerPort() {
        return handler.getPort();
    }

    protected void waitForDevServer() {
        waitForDevServer(handler);
    }

    protected static void waitForDevServer(DevModeHandler devModeHandler) {
        Assert.assertNotNull(devModeHandler);
        ((AbstractDevServerRunner) (devModeHandler)).waitForDevServer();
    }

    protected static boolean hasDevServerProcess(
            DevModeHandler devModeHandler) {
        Assert.assertNotNull(devModeHandler);
        Field devServerProcessField;
        try {
            devServerProcessField = AbstractDevServerRunner.class
                    .getDeclaredField("devServerProcess");
            devServerProcessField.setAccessible(true);
            AtomicReference<Process> devServerProcess = (AtomicReference<Process>) devServerProcessField
                    .get(devModeHandler);
            return devServerProcess.get() != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void removeDevModeHandlerInstance() throws Exception {
        // Reset unique instance of DevModeHandler
        Field devModeHandler = DevModeHandlerManagerImpl.class
                .getDeclaredField("devModeHandler");
        devModeHandler.setAccessible(true);
        devModeHandler.set(devModeHandlerManager, null);
    }

}
