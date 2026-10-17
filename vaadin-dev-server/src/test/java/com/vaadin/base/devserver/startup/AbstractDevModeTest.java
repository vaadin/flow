/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.base.devserver.startup;

import jakarta.servlet.ServletContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.base.devserver.AbstractDevServerRunner;
import com.vaadin.base.devserver.DevModeHandlerManagerImpl;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.DevModeHandler;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public abstract class AbstractDevModeTest {

    // JUnit 4 TemporaryFolder silently ignored cleanup failures, but JUnit 5
    // @TempDir fails the test. node_modules created by dev mode cannot always
    // be deleted, so skip automatic cleanup.
    @TempDir(cleanup = CleanupMode.NEVER)
    File temporaryFolder;
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

    @BeforeEach
    public void setup() throws Exception {
        // Reset static node installation cache to ensure test isolation
        com.vaadin.flow.testutil.FrontendStubs.resetFrontendToolsNodeCache();

        Field firstMapping = VaadinServlet.class
                .getDeclaredField("frontendMapping");
        firstMapping.setAccessible(true);
        firstMapping.set(null, "/fake-test-mapping");
        baseDir = temporaryFolder.getPath();
        npmFolder = temporaryFolder;

        Boolean enablePnpm = Boolean.TRUE;
        appConfig = spy(ApplicationConfiguration.class);

        servletContext = mock(ServletContext.class);
        when(servletContext
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);
        when(servletContext.getClassLoader())
                .thenReturn(servletContext.getClass().getClassLoader());
        when(servletContext.getContextPath()).thenReturn("");

        vaadinContext = new VaadinServletContext(servletContext);

        lookup = mock(Lookup.class);
        vaadinContext.setAttribute(Lookup.class, lookup);
        when(servletContext.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        mockApplicationConfiguration(appConfig, enablePnpm);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);
        when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);
        setupMockResourceProvider(resourceProvider);
        devModeHandlerManager = new DevModeHandlerManagerImpl();
        when(lookup.lookup(DevModeHandlerManager.class))
                .thenReturn(devModeHandlerManager);

        configuration = new MockDeploymentConfiguration();
        when(lookup.lookup(DeploymentConfiguration.class))
                .thenReturn(configuration);
        when(lookup.lookup(ApplicationConfiguration.class))
                .thenReturn(appConfig);
        when(lookup.lookup(StaticFileHandlerFactory.class))
                .thenReturn(service -> new StaticFileServer(service));

        vaadinService = mock(VaadinService.class);

        when(vaadinService.getContext()).thenReturn(vaadinContext);
        when(vaadinService.getDeploymentConfiguration())
                .thenReturn(configuration);

    }

    @AfterEach
    void teardown() {
        if (handler != null) {
            handler.stop();
            handler = null;
        }

    }

    protected void setupMockResourceProvider(
            ResourceProvider mockResourceProvider) throws IOException {
        // Expose actual META-INF/frontend resources from the classpath so
        // that @JsModule files on UI can be found during frontend scanning.
        List<URL> frontendUrls = Collections.list(getClass().getClassLoader()
                .getResources(Constants.RESOURCES_FRONTEND_DEFAULT));
        when(mockResourceProvider
                .getApplicationResources(Constants.RESOURCES_FRONTEND_DEFAULT))
                .thenReturn(frontendUrls);
    }

    private void mockApplicationConfiguration(
            ApplicationConfiguration appConfig, boolean enablePnpm) {
        when(appConfig.getStringProperty(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        when(appConfig.isProductionMode()).thenReturn(false);
        when(appConfig.getContext()).thenReturn(vaadinContext);
        when(appConfig.getMode())
                .thenReturn(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD);
        when(appConfig.isPnpmEnabled()).thenReturn(enablePnpm);

        when(appConfig.getBooleanProperty(anyString(), anyBoolean()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        when(appConfig.getStringProperty(FrontendUtils.PROJECT_BASEDIR, null))
                .thenReturn(baseDir);
        when(appConfig.getBuildFolder()).thenReturn(Constants.TARGET);
        when(appConfig.getPropertyNames())
                .thenReturn(Collections.enumeration(Collections.emptyList()));
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
        assertNotNull(devModeHandler);
        ((AbstractDevServerRunner) (devModeHandler)).waitForDevServer();
    }

    protected static boolean hasDevServerProcess(
            DevModeHandler devModeHandler) {
        assertNotNull(devModeHandler);
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

    void removeDevModeHandlerInstance() throws Exception {
        // Reset unique instance of DevModeHandler
        Field devModeHandler = DevModeHandlerManagerImpl.class
                .getDeclaredField("devModeHandler");
        devModeHandler.setAccessible(true);
        devModeHandler.set(devModeHandlerManager, null);
    }

}
