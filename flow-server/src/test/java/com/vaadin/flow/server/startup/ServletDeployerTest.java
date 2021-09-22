package com.vaadin.flow.server.startup;

import javax.servlet.Registration;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.frontend.FallbackChunk;
import com.vaadin.flow.server.frontend.FrontendUtils;

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

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
    public void automaticallyRegisterTwoServletsWhenNoServletsPresent()
            throws Exception {
        deployer.contextInitialized(getContextEvent());

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void doNotRegisterAnythingIfRegistrationIsDisabled()
            throws Exception {
        deployer.contextInitialized(getContextEvent(getServletRegistration(
                "testServlet", TestServlet.class, singletonList("/test/*"),
                singletonMap(
                        InitParameters.DISABLE_AUTOMATIC_SERVLET_REGISTRATION,
                        "true"))));

        assertMappingsCount(0, 0);
    }

    @Test
    public void registeredServlets_compatibilityMode_servletsAreNotRegistered()
            throws Exception {
        assertservletsAreNotRegistered_registeredServlets(true);
    }

    @Test
    public void registeredServlets_servletsAreNotRegistered() throws Exception {
        assertservletsAreNotRegistered_registeredServlets(false);
    }

    @Test
    public void frontendServletIsNotRegisteredWhenProductionModeIsActive()
            throws Exception {
        deployer.contextInitialized(getContextEvent(getServletRegistration(
                "testServlet", TestServlet.class, singletonList("/test/*"),
                singletonMap(InitParameters.SERVLET_PARAMETER_PRODUCTION_MODE,
                        "true"))));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void frontendServletIsNotRegistered_whenMainServletIsRegistered()
            throws Exception {
        deployer.contextInitialized(getContextEvent());

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_mainServlet()
            throws Exception {
        dynamicMockCheck = registration -> EasyMock
                .expect(registration.setInitParameters(Collections.emptyMap()))
                .andReturn(null).once();

        deployer.contextInitialized(getContextEvent(
                getServletRegistration("test", TestServlet.class,
                        singletonList("/*"), Collections.emptyMap())));

        assertMappingsCount(0, 0);
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_frontendServlet()
            throws Exception {
        deployer.contextInitialized(getContextEvent(
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
    private ServletContextEvent getContextEvent(
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

        Lookup lookup = mock(Lookup.class);
        expect(contextMock.getAttribute(Lookup.class.getName()))
                .andReturn(lookup).anyTimes();

        ResourceProvider resourceProvider = mock(ResourceProvider.class);

        expect(resourceProvider.getApplicationResources(anyObject()))
                .andReturn(Collections.emptyList()).anyTimes();

        replay(resourceProvider);

        expect(lookup.lookup(ResourceProvider.class))
                .andReturn(resourceProvider).anyTimes();

        replay(lookup);

        ApplicationConfiguration appConfig = mock(
                ApplicationConfiguration.class);

        expect(appConfig.getPropertyNames())
                .andReturn(Collections.emptyEnumeration()).anyTimes();
        expect(appConfig.getStringProperty(EasyMock.anyString(),
                EasyMock.anyString())).andReturn(null).anyTimes();
        expect(appConfig.isProductionMode()).andReturn(false);
        FallbackChunk chunk = mock(FallbackChunk.class);
        expect(appConfig.getFallbackChunk()).andReturn(chunk).anyTimes();

        replay(appConfig);

        expect(contextMock
                .getAttribute(ApplicationConfiguration.class.getName()))
                        .andReturn(appConfig).anyTimes();

        expect(contextMock.getContextPath()).andReturn("").once();
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

        expect(contextMock.getAttribute(anyString())).andReturn(null)
                .anyTimes();
        contextMock.setAttribute(anyObject(), anyObject());
        contextMock.setAttribute(anyObject(), anyObject());
        EasyMock.expectLastCall();

        File token = tempFolder.newFile();
        FileUtils.write(token, "{}", StandardCharsets.UTF_8);

        expect(contextMock.getInitParameterNames())
                .andReturn(Collections.enumeration(Collections
                        .singletonList(FrontendUtils.PARAM_TOKEN_FILE)))
                .anyTimes();
        expect(contextMock.getInitParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .andReturn(token.getPath()).anyTimes();

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

    private void assertservletsAreNotRegistered_registeredServlets(
            boolean compatibilityMode) throws Exception {
        deployer.contextInitialized(getContextEvent(
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"), Collections.emptyMap())));

        assertMappingsCount(1, 1);
    }

}
