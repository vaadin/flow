package com.vaadin.base.devserver.startup;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletContext;

import com.vaadin.base.devserver.DevModeHandlerManagerImpl;
import com.vaadin.base.devserver.MockDeploymentConfiguration;
import com.vaadin.base.devserver.WebpackHandler;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

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

    public void setup() throws Exception {
        baseDir = temporaryFolder.getRoot().getPath();
        npmFolder = temporaryFolder.getRoot();

        Boolean enablePnpm = Boolean.TRUE;
        appConfig = Mockito.mock(ApplicationConfiguration.class);
        mockApplicationConfiguration(appConfig, enablePnpm);

        servletContext = Mockito.mock(ServletContext.class);
        Mockito.when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        lookup = Mockito.mock(Lookup.class);
        Mockito.when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        devModeHandlerManager = new DevModeHandlerManagerImpl();
        Mockito.when(lookup.lookup(DevModeHandlerManager.class))
                .thenReturn(devModeHandlerManager);

        configuration = new MockDeploymentConfiguration();
        Mockito.when(lookup.lookup(DeploymentConfiguration.class))
                .thenReturn(configuration);
        Mockito.when(lookup.lookup(ApplicationConfiguration.class))
                .thenReturn(appConfig);
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
        Mockito.when(appConfig.getBuildFolder()).thenReturn(Constants.TARGET);
        Mockito.when(appConfig.getFlowResourcesFolder())
                .thenReturn(Paths
                        .get(Constants.TARGET,
                                FrontendUtils.DEFAULT_FLOW_RESOURCES_FOLDER)
                        .toString());
    }

    protected DevModeHandler getDevModeHandler() {
        return DevModeHandlerManager
                .getDevModeHandler(new VaadinServletContext(servletContext))
                .orElse(null);
    }

    protected void waitForDevModeServer() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        Assert.assertNotNull(handler);
        Method join = WebpackHandler.class.getDeclaredMethod("join");
        join.setAccessible(true);
        join.invoke(handler);
    }

    protected boolean hasWebpackProcess() {
        Assert.assertNotNull(getDevModeHandler());
        Field webpackProcessField;
        try {
            webpackProcessField = WebpackHandler.class
                    .getDeclaredField("webpackProcess");
            webpackProcessField.setAccessible(true);
            AtomicReference<Process> webpackProcess = (AtomicReference<Process>) webpackProcessField
                    .get(handler);
            return webpackProcess.get() != null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
