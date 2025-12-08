/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.server.startup;

import jakarta.servlet.Registration;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.frontend.FrontendUtils;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServletDeployerTest {
    private final ServletDeployer deployer = new ServletDeployer();

    private List<String> servletNames;
    private List<String> servletMappings;
    private List<Integer> servletLoadOnStartup;

    private boolean disableAutomaticServletRegistration = false;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

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
        servletNames = new ArrayList<>();
        servletMappings = new ArrayList<>();
        servletLoadOnStartup = new ArrayList<>();
    }

    @Test
    public void automaticallyRegisterTwoServletsWhenNoServletsPresent()
            throws Exception {
        deployer.contextInitialized(getContextEvent());

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
        assertLoadOnStartupSet();
    }

    @Test
    public void doNotRegisterAnythingIfRegistrationIsDisabled()
            throws Exception {
        disableAutomaticServletRegistration = true;
        deployer.contextInitialized(getContextEvent(
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"), Collections.emptyMap())));

        assertMappingsCount(0, 0);
    }

    @Test
    public void registeredNonVaadinServlets_vaadinServletsAreRegistered()
            throws Exception {
        deployer.contextInitialized(getContextEvent(
                getServletRegistration("testServlet", TestServlet.class,
                        singletonList("/test/*"), Collections.emptyMap())));

        assertMappingsCount(1, 1);
        assertLoadOnStartupSet();
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
        assertLoadOnStartupSet();
    }

    @Test
    public void frontendServletIsNotRegistered_whenMainServletIsRegistered()
            throws Exception {
        deployer.contextInitialized(getContextEvent());

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
        assertLoadOnStartupSet();
    }

    @Test
    public void servletsWithoutClassName_registrationDoesNotFail()
            throws Exception {
        deployer.contextInitialized(getContextEvent(getServletRegistration(
                "test", null, singletonList("/WEB-INF/test.jsp"),
                Collections.emptyMap())));

        assertMappingsCount(1, 1);
        assertMappingIsRegistered(ServletDeployer.class.getName(), "/*");
        assertLoadOnStartupSet();
    }

    @Test
    public void servletIsNotRegisteredWhenAnotherHasTheSamePathMapping_mainServlet()
            throws Exception {
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
        assertLoadOnStartupSet();
    }

    private void assertMappingsCount(int numServlets, int numMappings) {
        assertEquals(String.format(
                "Expected to have exactly '%d' servlets, but got '%d': '%s'",
                numServlets, servletNames.size(), servletNames),
                servletNames.size(), numServlets);
        assertEquals(String.format(
                "Expected to have exactly '%d' mappings, but got '%d': '%s'",
                numMappings, servletMappings.size(), servletMappings),
                servletMappings.size(), numMappings);
    }

    private void assertMappingIsRegistered(String servletName,
            String mappedPath) {
        int servletNameIndex = servletNames.indexOf(servletName);
        int pathIndex = servletMappings.indexOf(mappedPath);
        assertTrue(String.format(
                "Did not find servlet name '%s' among added servlet names: '%s'",
                servletName, servletNames), servletNameIndex >= 0);
        assertTrue(String.format(
                "Did not find mapped path '%s' among added paths: '%s'",
                mappedPath, servletMappings), pathIndex >= 0);
        assertEquals(
                "Expected servlet name '%s' and mapped path '%s' to be added for the same servlet in the same time",
                pathIndex, servletNameIndex);
    }

    private void assertLoadOnStartupSet() {
        assertEquals("Servlet loadOnStartup should be invoked only once", 1,
                servletLoadOnStartup.size());
        assertEquals(
                String.format(
                        "Expected servlet loadOnStartup to be '%d' but was '%d",
                        1, servletLoadOnStartup.get(0)),
                (Integer) 1, servletLoadOnStartup.get(0));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private ServletContextEvent getContextEvent(
            ServletRegistration... servletRegistrations) throws Exception {
        ServletRegistration.Dynamic dynamicMock = Mockito
                .mock(ServletRegistration.Dynamic.class);

        Mockito.when(dynamicMock.addMapping(Mockito.anyString()))
                .thenAnswer(answer -> {
                    String mappings = answer.getArgument(0);
                    this.servletMappings.addAll(Arrays.asList(mappings));
                    return Collections.emptySet();
                });
        Mockito.doAnswer(i -> this.servletLoadOnStartup.add(i.getArgument(0)))
                .when(dynamicMock).setLoadOnStartup(ArgumentMatchers.anyInt());

        ServletContext contextMock = Mockito.mock(ServletContext.class);

        Lookup lookup = Mockito.mock(Lookup.class);
        Mockito.when(contextMock.getAttribute(Lookup.class.getName()))
                .thenReturn(lookup);

        ResourceProvider resourceProvider = Mockito
                .mock(ResourceProvider.class);

        Mockito.when(resourceProvider.getApplicationResources(Mockito.any()))
                .thenReturn(Collections.emptyList());

        Mockito.when(lookup.lookup(ResourceProvider.class))
                .thenReturn(resourceProvider);

        ApplicationConfiguration appConfig = Mockito
                .mock(ApplicationConfiguration.class);

        Mockito.when(appConfig.getPropertyNames())
                .thenReturn(Collections.emptyEnumeration());
        Mockito.when(appConfig.isProductionMode()).thenReturn(false);

        Mockito.when(appConfig.disableAutomaticServletRegistration())
                .thenReturn(disableAutomaticServletRegistration);

        Mockito.when(contextMock
                .getAttribute(ApplicationConfiguration.class.getName()))
                .thenReturn(appConfig);

        Mockito.when(contextMock.getContextPath()).thenReturn("");
        Mockito.when(contextMock.getClassLoader())
                .thenReturn(this.getClass().getClassLoader());

        Mockito.when(contextMock.addServlet(Mockito.anyString(),
                Mockito.any(Class.class))).thenAnswer(answer -> {
                    String servletName = answer.getArgument(0);
                    servletNames.add(servletName);
                    return dynamicMock;
                });

        // seems to be a compiler bug, since fails to compile with the
        // actual
        // types specified (or being inlined) but works with raw type
        Map hack = Stream.of(servletRegistrations).collect(
                Collectors.toMap(Registration::getName, Function.identity()));
        Mockito.when(contextMock.getServletRegistrations()).thenReturn(hack);

        File token = tempFolder.newFile();
        FileUtils.write(token, "{}", StandardCharsets.UTF_8);

        Mockito.when(contextMock.getInitParameterNames())
                .thenReturn(Collections.enumeration(Collections
                        .singletonList(FrontendUtils.PARAM_TOKEN_FILE)));
        Mockito.when(
                contextMock.getInitParameter(FrontendUtils.PARAM_TOKEN_FILE))
                .thenReturn(token.getPath());

        return new ServletContextEvent(contextMock);
    }

    private ServletRegistration getServletRegistration(String servletName,
            Class<?> servletClass, Collection<String> pathMappings,
            Map<String, String> initParameters) {
        ServletRegistration registrationMock = Mockito
                .mock(ServletRegistration.class);
        Mockito.when(registrationMock.getClassName()).thenReturn(
                servletClass != null ? servletClass.getName() : null);
        Mockito.when(registrationMock.getMappings()).thenReturn(pathMappings);
        Mockito.when(registrationMock.getName()).thenReturn(servletName);
        Mockito.when(registrationMock.getInitParameters())
                .thenReturn(initParameters);
        Mockito.when(registrationMock.getInitParameter(Mockito.anyString()))
                .thenAnswer(answer -> {
                    String name = answer.getArgument(0);
                    return initParameters.get(name);
                });
        return registrationMock;
    }

}
