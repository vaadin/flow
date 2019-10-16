package com.vaadin.flow.server.startup;

import javax.servlet.Registration;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.easymock.EasyMock.anyBoolean;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServletDeployerTest {
    private final ServletDeployer deployer = new ServletDeployer();

    private final Capture<String> servletNames = EasyMock
            .newCapture(CaptureType.ALL);
    private final Capture<String> servletMappings = EasyMock
            .newCapture(CaptureType.ALL);

    private Consumer<ServletRegistration.Dynamic> dynamicMockCheck;

    private static class TestVaadinServlet extends VaadinServlet {
    }

    private static class TestServlet implements Servlet {
        @Override
        public void init(ServletConfig config) {
        }

        @Override
        public ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(ServletRequest req, ServletResponse res) {
        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {
        }
    }

    @Route
    private static class ComponentWithRoute extends Component {
    }

    @Before
    public void clearCaptures() {
        servletNames.reset();
        servletMappings.reset();
    }

    @Test
    public void hasRoutes_automaticallyRegisterTwoServletsWhenNoServletsPresent()
            throws Exception {
        deployer.contextInitialized(getContextEvent(true, false));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void hasWebComponents_automaticallyRegisterOneServletsWhenNoServletsPresent()
            throws Exception {
        deployer.contextInitialized(getContextEvent(false, true));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void doNotRegisterAnythingIfRegistrationIsDisabled()
            throws Exception {
        deployer.contextInitialized(getContextEvent(true, true,
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"),
                        singletonMap(
                                Constants.DISABLE_AUTOMATIC_SERVLET_REGISTRATION,
                                "true"))));

        assertMappingsCount(0, 0);
    }

    @Test
    public void noServlets_noRoutes_noWebComponents_servletsAreNotRegistered()
            throws Exception {
        deployer.contextInitialized(getContextEvent(false, false));

        assertMappingsCount(0, 0);
    }

    @Test
    public void registeredServlets_noRoutes_noWebComponents_compatibilityMode_servletsAreNotRegistered()
            throws Exception {
        assertservletsAreNotRegistered_registeredServlets_noRoutes_noWebComponents(
                true);
    }

    @Test
    public void registeredServlets_noRoutes_noWebComponents_servletsAreNotRegistered()
            throws Exception {
        assertservletsAreNotRegistered_registeredServlets_noRoutes_noWebComponents(
                false);
    }

    @Test
    public void mainServletIsNotRegisteredWhenVaadinServletIsPresent_frontendServletIsRegistered()
            throws Exception {
        dynamicMockCheck = registration -> EasyMock
                .expect(registration.setInitParameters(Collections.singletonMap(
                        Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        Boolean.TRUE.toString())))
                .andReturn(null).once();
        deployer.contextInitialized(getContextEvent(true, true,
                getServletRegistration("testServlet", TestVaadinServlet.class,
                        singletonList("/test/*"),
                        Collections.singletonMap(
                                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                                "true"))));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void frontendServletIsNotRegisteredWhenProductionModeIsActive()
            throws Exception {
        deployer.contextInitialized(getContextEvent(true, true,
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"),
                        singletonMap(
                                Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                                "true"))));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void frontendServletIsNotRegistered_whenMainServletIsRegistered()
            throws Exception {
        deployer.contextInitialized(getContextEvent(true, true));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void frontendServletIsRegisteredWhenAtLeastOneServletHasDevelopmentAndCompatibilityMode()
            throws Exception {
        Map<String, String> productionMode = new HashMap<>();
        productionMode.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, "true");
        productionMode.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                "true");

        Map<String, String> devMode = new HashMap<>();
        devMode.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, "false");
        devMode.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE, "true");

        dynamicMockCheck = registration -> EasyMock
                .expect(registration.setInitParameters(Collections.singletonMap(
                        Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        Boolean.TRUE.toString())))
                .andReturn(null).once();

        deployer.contextInitialized(getContextEvent(true, true,
                getServletRegistration("testServlet1", TestVaadinServlet.class,
                        singletonList("/test1/*"), productionMode),
                getServletRegistration("testServlet2", TestVaadinServlet.class,
                        singletonList("/test2/*"), devMode)));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void frontendServletIsNotRegisteredWhenNoServletsHaveDevelopmentAndCompatibilityMode()
            throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, "false");
        params.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE, "false");
        deployer.contextInitialized(getContextEvent(
                true, true,
                getServletRegistration("testServlet1", TestVaadinServlet.class,
                        singletonList("/test1/*"),
                        singletonMap(
                                Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                                "true")),
                getServletRegistration("testServlet2", TestVaadinServlet.class,
                        singletonList("/test2/*"), params)));

        assertMappingsCount(0, 0);
    }

    @Test
    public void frontendServletIsRegisteredInProductionModeIfOriginalFrontendResourcesAreUsed()
            throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, "true");
        params.put(Constants.USE_ORIGINAL_FRONTEND_RESOURCES, "true");
        params.put(Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE, "true");

        dynamicMockCheck = registration -> EasyMock
                .expect(registration.setInitParameters(Collections.singletonMap(
                        Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        Boolean.TRUE.toString())))
                .andReturn(null).once();
        deployer.contextInitialized(
                getContextEvent(true, true, getServletRegistration("test",
                        TestVaadinServlet.class, emptyList(), params)));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_mainServlet()
            throws Exception {
        dynamicMockCheck = registration -> EasyMock
                .expect(registration.setInitParameters(Collections.singletonMap(
                        Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                        Boolean.TRUE.toString())))
                .andReturn(null).once();

        deployer.contextInitialized(getContextEvent(true, true,
                getServletRegistration("test", TestServlet.class,
                        singletonList("/*"),
                        Collections.singletonMap(
                                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                                "true"))));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_frontendServlet()
            throws Exception {
        deployer.contextInitialized(getContextEvent(true, true,
                getServletRegistration("test", TestServlet.class,
                        singletonList("/frontend/*"), Collections.emptyMap())));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    private void assertMappingsCount(int numServlets, int numMappings) {
        assertEquals(String.format(
                "Expected to have exactly '%d' servlets, but got '%d': '%s'",
                numServlets, servletNames.getValues().size(),
                servletNames.getValues()), servletNames.getValues().size(),
                numServlets);
        assertEquals(String.format(
                "Expected to have exactly '%d' mappings, but got '%d': '%s'",
                numMappings, servletMappings.getValues().size(),
                servletMappings.getValues()),
                servletMappings.getValues().size(), numMappings);
    }

    private void assertMappingIsRegistered(String servletName,
            String mappedPath) {
        int servletNameIndex = servletNames.getValues().indexOf(servletName);
        int pathIndex = servletMappings.getValues().indexOf(mappedPath);
        assertTrue(String.format(
                "Did not find servlet name '%s' among added servlet names: '%s'",
                servletName, servletNames.getValues()), servletNameIndex >= 0);
        assertTrue(
                String.format(
                        "Did not find mapped path '%s' among added paths: '%s'",
                        mappedPath, servletMappings.getValues()),
                pathIndex >= 0);
        assertEquals(
                "Expected servlet name '%s' and mapped path '%s' to be added for the same servlet in the same time",
                pathIndex, servletNameIndex);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ServletContextEvent getContextEvent(boolean addRoutes,
            boolean addWebComponents,
            ServletRegistration... servletRegistrations) throws Exception {
        ServletRegistration.Dynamic dynamicMock = mock(
                ServletRegistration.Dynamic.class);
        dynamicMock.setAsyncSupported(anyBoolean());
        EasyMock.expectLastCall().anyTimes();

        if (dynamicMockCheck != null) {
            dynamicMockCheck.accept(dynamicMock);
        }

        expect(dynamicMock.addMapping(capture(servletMappings)))
                .andReturn(Collections.emptySet()).anyTimes();

        ServletContext contextMock = mock(ServletContext.class);
        expect(contextMock.getClassLoader())
                .andReturn(this.getClass().getClassLoader()).anyTimes();
        expect(contextMock.addServlet(EasyMock.capture(servletNames),
                anyObject(Class.class))).andAnswer(() -> dynamicMock)
                        .anyTimes();

        expect(contextMock.getResource(EasyMock.anyString())).andReturn(null)
                .anyTimes();

        // seems to be a compiler bug, since fails to compile with the actual
        // types specified (or being inlined) but works with raw type
        @SuppressWarnings({ "rawtypes", "serial" })
        Map hack = Stream.of(servletRegistrations).collect(
                Collectors.toMap(Registration::getName, Function.identity()));
        expect(contextMock.getServletRegistrations()).andReturn(hack)
                .anyTimes();

        if (addRoutes) {
            expect(contextMock.getAttribute(ApplicationRouteRegistry.ApplicationRouteRegistryWrapper.class.getName()))
                    .andAnswer(() -> {
                        ApplicationRouteRegistry registry = new ApplicationRouteRegistry();

                        RouteConfiguration routeConfiguration = RouteConfiguration
                                .forRegistry(registry);
                        routeConfiguration.update(() -> {
                            routeConfiguration.getHandledRegistry().clean();
                            routeConfiguration.setAnnotatedRoute(
                                    ComponentWithRoute.class);
                        });
                        return new ApplicationRouteRegistry.ApplicationRouteRegistryWrapper(registry);
                    }).anyTimes();
        }
        if (addWebComponents) {
            expect(contextMock.getAttribute(
                    WebComponentConfigurationRegistry.class.getName()))
                            .andAnswer(() -> {
                                WebComponentConfigurationRegistry registry = new WebComponentConfigurationRegistry() {
                                };
                                registry.setConfigurations(
                                        Collections.singleton(
                                                new WebComponentExporter.WebComponentConfigurationFactory()
                                                        .create(FakeExporter.class)));

                                return registry;
                            }).anyTimes();

        }

        expect(contextMock.getAttribute(anyString())).andReturn(null)
                .anyTimes();
        contextMock.setAttribute(anyObject(), anyObject());
        contextMock.setAttribute(anyObject(), anyObject());
        EasyMock.expectLastCall();

        expect(contextMock.getInitParameterNames())
                .andReturn(Collections.emptyEnumeration()).anyTimes();

        replay(dynamicMock, contextMock);
        return new ServletContextEvent(contextMock);
    }

    private ServletRegistration getServletRegistration(String servletName,
            Class<?> servletClass, Collection<String> pathMappings,
            Map<String, String> initParameters) {
        ServletRegistration registrationMock = mock(ServletRegistration.class);
        expect(registrationMock.getClassName())
                .andReturn(servletClass.getName()).anyTimes();
        expect(registrationMock.getMappings()).andReturn(pathMappings)
                .anyTimes();
        expect(registrationMock.getName()).andReturn(servletName).anyTimes();
        expect(registrationMock.getInitParameters()).andReturn(initParameters)
                .anyTimes();
        Capture<String> parameterNameCapture = newCapture();
        expect(registrationMock.getInitParameter(capture(parameterNameCapture)))
                .andAnswer(() -> initParameters
                        .get(parameterNameCapture.getValue()))
                .anyTimes();
        replay(registrationMock);
        return registrationMock;
    }

    private void assertservletsAreNotRegistered_registeredServlets_noRoutes_noWebComponents(
            boolean compatibilityMode) throws Exception {
        deployer.contextInitialized(getContextEvent(false, false,
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"),
                        singletonMap(
                                Constants.SERVLET_PARAMETER_COMPATIBILITY_MODE,
                                Boolean.toString(compatibilityMode)))));

        assertMappingsCount(0, 0);
    }

    public final static class FakeExporter
            extends WebComponentExporter<Component> {
        public FakeExporter() {
            super("tag");
        }

        @Override
        public void configureInstance(WebComponent<Component> webComponent,
                Component component) {

        }
    }
}
