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

package com.vaadin.flow.spring.security;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.auth.AccessPathChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.spring.MockVaadinContext;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.SpringVaadinServletService;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

import static org.mockito.ArgumentMatchers.any;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = { RequestUtilPathAccessTest.TestConfig.class,
        SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class, })
public class RequestUtilPathAccessTest {

    @Autowired
    RequestUtil requestUtil;

    @Autowired
    NavigationAccessControl accessControl;

    @MockitoBean
    VaadinConfigurationProperties vaadinConfigurationProperties;

    @MockitoBean
    private ServletRegistrationBean<SpringServlet> springServletRegistration;

    @MockitoBean
    private AccessPathChecker accessPathChecker;

    @Before
    public void setupMockAccessPathChecker() {
        Set<String> publicPaths = Set.of("", "alias/public", "other",
                "alias/other");
        Set<String> publicPathsPrefix = Set.of("tpl/public/", "tpl/other/");
        Mockito.when(accessPathChecker.hasAccess(any(), any(), any()))
                .then(i -> {
                    String path = i.getArgument(0);
                    if (publicPaths.contains(path) || publicPathsPrefix.stream()
                            .anyMatch(path::startsWith)) {
                        return true;
                    }
                    return false;
                });
    }

    @Route("")
    @RouteAlias("alias/public")
    @RouteAlias("tpl/public/:p1/:p2")
    public static class PublicRootView extends Component {

    }

    @Route("other")
    @RouteAlias("alias/other")
    @RouteAlias("tpl/other/:p1/:p2")
    public static class AnotherPublicView extends Component {

    }

    @Route("admin")
    @RouteAlias("alias/admin")
    @RouteAlias("tpl/admin/:p1/:p2")
    public static class AdminView extends Component {

    }

    @Test
    public void testAnonymousRouteRequest_rootMappedServlet_publicView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/public");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/public/123/abc");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/other");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/other/123/abc");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_rootMappedServlet_notAView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/public");
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/public/123/abc");
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/other");
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/other/123/abc");
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

    }

    @Test
    public void testAnonymousRouteRequest_rootMappedServlet_privateView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        addRoute(setupMockServlet(), AdminView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/admin");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest(null);
        request.setServletPath("/alias/admin");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest(null);
        request.setServletPath("/tpl/admin/abc/123");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_publicView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("/");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/public");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/public/abc/123");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/other");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/other/abc/123");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_notAView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/public");
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/public/123/abc");
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/other");
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/other/123/abc");
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_privateView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        addRoute(setupMockServlet(), AdminView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/admin");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest(null);
        request.setServletPath("/foo/alias/admin");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest(null);
        request.setServletPath("/foo/tpl/admin/abc/123");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_publicViewPathOutsideServlet() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/public");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/public/abc/123");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/other");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/other/abc/123");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

    }

    @Test
    public void testRouteRequest_servletNotInited() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        setupMockServlet(false);
        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/public");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/public/123/abc");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("alias/other");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("tpl/other/123/abc");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

    }

    private SpringServlet setupMockServlet() {
        return setupMockServlet(true);
    }

    private SpringServlet setupMockServlet(boolean inited) {
        SpringServlet servlet = Mockito.mock(SpringServlet.class);
        SpringVaadinServletService service = Mockito
                .mock(SpringVaadinServletService.class);
        Router router = Mockito.mock(Router.class);

        RouteRegistry routeRegistry = new TestRouteRegistry();
        DeploymentConfiguration deploymentConfiguration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(springServletRegistration.getServlet())
                .thenReturn(servlet);
        if (inited) {
            Mockito.when(servlet.getService()).thenReturn(service);
        }
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(service.getRouter()).thenReturn(router);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);
        return servlet;
    }

    private void addRoute(SpringServlet servlet,
            Class<? extends Component> view) {

        Optional<Route> route = AnnotationReader.getAnnotationFor(view,
                Route.class);

        if (!route.isPresent()) {
            throw new IllegalArgumentException(
                    "Unable find a @Route annotation");
        }
        RouteRegistry routeRegistry = servlet.getService().getRouter()
                .getRegistry();
        RouteConfiguration.forRegistry(routeRegistry).setAnnotatedRoute(view);
    }

    static MockHttpServletRequest createRequest(String pathInfo) {
        return createRequest(pathInfo, null);
    }

    static MockHttpServletRequest createRequest(String pathInfo,
            RequestType type) {
        return createRequest(pathInfo, type, Collections.emptyMap());
    }

    static MockHttpServletRequest createRequest(String pathInfo,
            RequestType type, Map<String, String> headers) {
        String uri = (pathInfo == null ? "/" : pathInfo);
        MockHttpServletRequest r = new MockHttpServletRequest("GET", uri);
        r.setPathInfo(pathInfo);
        if (type != null) {
            r.setParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER,
                    type.getIdentifier());
        }
        headers.forEach((key, value) -> r.addHeader(key, value));

        return r;
    }

    private static class TestRouteRegistry extends ApplicationRouteRegistry {
        public TestRouteRegistry() {
            super(new MockVaadinContext(new DefaultRoutePathProvider()));
        }
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        NavigationAccessControlConfigurer navigationAccessControlConfigurer() {
            return new NavigationAccessControlConfigurer()
                    .withRoutePathAccessChecker();
        }
    }
}
