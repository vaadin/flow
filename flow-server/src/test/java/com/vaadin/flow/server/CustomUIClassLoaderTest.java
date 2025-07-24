package com.vaadin.flow.server;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import org.junit.Test;
import org.mockito.Mockito;

public class CustomUIClassLoaderTest {

    /**
     * Stub root
     */
    public static class MyUI extends UI {
        @Override
        protected void init(VaadinRequest request) {
            // Nothing to see here
        }
    }

    /**
     * Dummy ClassLoader that just saves the name of the requested class before
     * delegating to the default implementation.
     */
    public class LoggingClassLoader extends ClassLoader {

        private List<String> requestedClasses = new ArrayList<>();

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve)
                throws ClassNotFoundException {
            requestedClasses.add(name);
            return super.loadClass(name, resolve);
        }
    }

    /**
     * Tests that a UI class can be loaded even if no classloader has been
     * provided.
     *
     * @throws Exception
     *             if thrown
     */
    @Test
    public void testWithDefaultClassLoader() throws Exception {
        VaadinSession application = createStubApplication();

        Class<? extends UI> uiClass = BootstrapHandler
                .getUIClass(createRequestMock(getClass().getClassLoader()));

        assertEquals(MyUI.class, uiClass);
    }

    private static DeploymentConfiguration createConfigurationMock() {
        Properties properties = new Properties();
        properties.put(InitParameters.UI_PARAMETER, MyUI.class.getName());
        VaadinContext context = new MockVaadinContext();
        ApplicationConfiguration config = Mockito
                .mock(ApplicationConfiguration.class);
        Mockito.when(config.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(config.getBuildFolder()).thenReturn(".");
        Mockito.when(config.getContext()).thenReturn(context);
        return new DefaultDeploymentConfiguration(config,
                CustomUIClassLoaderTest.class, properties);
    }

    private static VaadinRequest createRequestMock(ClassLoader classloader) {
        // Mock a VaadinService to give the passed classloader
        VaadinService configurationMock = Mockito.mock(VaadinService.class);
        DeploymentConfiguration deploymentConfiguration = createConfigurationMock();
        Mockito.when(configurationMock.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(configurationMock.getClassLoader())
                .thenReturn(classloader);

        // Mock a VaadinRequest to give the mocked vaadin service
        VaadinRequest requestMock = Mockito.mock(VaadinRequest.class);
        Mockito.when(requestMock.getService()).thenReturn(configurationMock);
        Mockito.when(requestMock.getService()).thenReturn(configurationMock);
        Mockito.when(requestMock.getService()).thenReturn(configurationMock);

        return requestMock;
    }

    /**
     * Tests that the ClassLoader passed in the ApplicationStartEvent is used to
     * load UI classes.
     *
     * @throws Exception
     *             if thrown
     */
    @Test
    public void testWithClassLoader() throws Exception {
        LoggingClassLoader loggingClassLoader = new LoggingClassLoader();

        Class<? extends UI> uiClass = BootstrapHandler
                .getUIClass(createRequestMock(loggingClassLoader));

        assertEquals(MyUI.class, uiClass);
        assertEquals(1, loggingClassLoader.requestedClasses.size());
        assertEquals(MyUI.class.getName(),
                loggingClassLoader.requestedClasses.get(0));

    }

    private VaadinSession createStubApplication() {
        return new AlwaysLockedVaadinSession(new MockVaadinServletService()) {
            @Override
            public DeploymentConfiguration getConfiguration() {
                return createConfigurationMock();
            }
        };
    }
}
