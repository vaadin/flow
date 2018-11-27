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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServlet;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
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
    public void automaticallyRegisterTwoServletsWhenNoServletsPresent() {
        deployer.contextInitialized(getContextEvent(true));

        assertMappingsCount(2);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void doNotRegisterAnythingIfRegistrationIsDisabled() {
        deployer.contextInitialized(getContextEvent(true,
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"),
                        singletonMap(
                                Constants.DISABLE_AUTOMATIC_SERVLET_REGISTRATION,
                                "true"))));

        assertMappingsCount(0);
    }

    @Test
    public void mainServletIsNotRegisteredWhenNoRoutesArePresent() {
        deployer.contextInitialized(getContextEvent(false));

        assertMappingsCount(1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void mainServletIsNotRegisteredWhenVaadinServletIsPresent() {
        deployer.contextInitialized(getContextEvent(true,
                getServletRegistration("testServlet", TestVaadinServlet.class,
                        singletonList("/test/*"), emptyMap())));

        assertMappingsCount(1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void frontendServletIsNotRegisteredWhenProductionModeIsActive() {
        deployer.contextInitialized(getContextEvent(true,
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"),
                        singletonMap(
                                Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                                "true"))));

        assertMappingsCount(1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void frontendServletIsRegisteredWhenAtLeastOneServletHasDevelopmentMode() {
        deployer.contextInitialized(getContextEvent(true,
                getServletRegistration("testServlet1", TestServlet.class,
                        singletonList("/test1/*"), singletonMap(
                                Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                                "true")),
                getServletRegistration("testServlet2", TestServlet.class,
                        singletonList("/test2/*"),
                        singletonMap(
                                Constants.SERVLET_PARAMETER_PRODUCTION_MODE,
                                "false"))));

        assertMappingsCount(2);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void frontendServletIsRegisteredInProductionModeIfOriginalFrontendResourcesAreUsed() {
        Map<String, String> params = new HashMap<>();
        params.put(Constants.SERVLET_PARAMETER_PRODUCTION_MODE, "true");
        params.put(Constants.USE_ORIGINAL_FRONTEND_RESOURCES, "true");

        deployer.contextInitialized(
                getContextEvent(true, getServletRegistration("test",
                        TestServlet.class, emptyList(), params)));

        assertMappingsCount(2);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_mainServlet() {
        deployer.contextInitialized(getContextEvent(true,
                getServletRegistration("test", TestServlet.class,
                        singletonList("/*"), Collections.emptyMap())));

        assertMappingsCount(1);
        assertMappingIsRegistered("frontendFilesServlet", "/frontend/*");
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_frontendServlet() {
        deployer.contextInitialized(getContextEvent(true,
                getServletRegistration("test", TestServlet.class,
                        singletonList("/frontend/*"), Collections.emptyMap())));

        assertMappingsCount(1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    private void assertMappingsCount(int expectedCount) {
        assertEquals(
                String.format(
                        "Expected to have exactly '%d' mappings, but got: '%s'",
                        expectedCount, servletNames.getValues()),
                servletNames.getValues().size(), expectedCount);
        assertEquals(
                String.format(
                        "Expected to have exactly '%d' mappings, but got: '%s'",
                        expectedCount, servletMappings.getValues()),
                servletMappings.getValues().size(), expectedCount);
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

    private ServletContextEvent getContextEvent(boolean addRoutes,
            ServletRegistration... servletRegistrations) {
        ServletRegistration.Dynamic dynamicMock = mock(
                ServletRegistration.Dynamic.class);
        dynamicMock.setAsyncSupported(anyBoolean());
        EasyMock.expectLastCall().anyTimes();
        expect(dynamicMock.addMapping(capture(servletMappings)))
                .andReturn(Collections.emptySet()).anyTimes();

        ServletContext contextMock = mock(ServletContext.class);
        expect(contextMock.getClassLoader())
                .andReturn(this.getClass().getClassLoader()).anyTimes();
        expect(contextMock.addServlet(EasyMock.capture(servletNames),
                anyObject(Class.class))).andAnswer(() -> dynamicMock)
                        .anyTimes();

        // seems to be a compiler bug, since fails to compile with the actual
        // types specified (or being inlined) but works with raw type
        @SuppressWarnings("rawtypes")
        Map hack = Stream.of(servletRegistrations).collect(
                Collectors.toMap(Registration::getName, Function.identity()));
        expect(contextMock.getServletRegistrations()).andReturn(hack)
                .anyTimes();

        if (addRoutes) {
            expect(contextMock.getAttribute(RouteRegistry.class.getName()))
                    .andAnswer(() -> {
                        GlobalRouteRegistry registry = new GlobalRouteRegistry();
                        registry.setNavigationTargets(Collections
                                .singleton(ComponentWithRoute.class));
                        return registry;
                    }).anyTimes();
        } else {
            expect(contextMock.getAttribute(anyString())).andReturn(null)
                    .anyTimes();
            contextMock.setAttribute(anyObject(), anyObject());
            EasyMock.expectLastCall();
        }

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
                        .get(parameterNameCapture.getValue())).anyTimes();
        replay(registrationMock);
        return registrationMock;
    }
}
