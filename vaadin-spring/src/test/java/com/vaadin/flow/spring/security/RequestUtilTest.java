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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.RouteTarget;
import com.vaadin.flow.router.internal.RouteUtil;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.server.auth.NavigationContext;
import com.vaadin.flow.server.auth.RoutePathAccessChecker;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.spring.MockVaadinContext;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.SpringVaadinServletService;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class })
public class RequestUtilTest {

    @Autowired
    RequestUtil requestUtil;

    @Autowired
    NavigationAccessControl accessControl;

    @MockitoBean
    VaadinConfigurationProperties vaadinConfigurationProperties;

    @MockitoBean
    private RoutePathAccessChecker accessPathChecker;

    @MockitoBean
    private ServletRegistrationBean<SpringServlet> springServletRegistration;

    @Before
    public void setUp() {
        accessControl.setEnabled(true);
        // Disable path checker
        Mockito.when(accessPathChecker.check(ArgumentMatchers.any()))
                .then(i -> i.getArgument(0, NavigationContext.class).neutral());
    }

    @Test
    public void testRootRequest_init_standardMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");

        Assert.assertTrue(requestUtil.isFrameworkInternalRequest(
                createRequest("", RequestType.INIT)));
    }

    @Test
    public void testRootRequest_other_standardMapping() {
        // given(this.vaadinConfigurationProperties.getUrlMapping()).willReturn("Hello");
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        Assert.assertFalse(requestUtil
                .isFrameworkInternalRequest(createRequest("", null)));
    }

    @Test
    public void testSubRequest_init_standardMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(
                createRequest("/foo", RequestType.INIT)));
    }

    @Test
    public void testSubRequest_other_standardMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        Assert.assertFalse(requestUtil
                .isFrameworkInternalRequest(createRequest("/foo", null)));
    }

    @Test
    public void testRootRequest_init_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        MockHttpServletRequest request = createRequest("/", RequestType.INIT);
        request.setServletPath("/bar");
        Assert.assertTrue(requestUtil.isFrameworkInternalRequest(request));

        request = createRequest("", RequestType.INIT);
        request.setServletPath("/bar");
        Assert.assertTrue(requestUtil.isFrameworkInternalRequest(request));

        request = createRequest(null, RequestType.INIT);
        request.setServletPath("/bar");
        Assert.assertTrue(requestUtil.isFrameworkInternalRequest(request));

    }

    @Test
    public void testRootRequest_other_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        MockHttpServletRequest request = createRequest("/", null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));

        request = createRequest("", null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));

        request = createRequest(null, null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));
    }

    @Test
    public void testSubRequest_init_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        MockHttpServletRequest request = createRequest("/foo",
                RequestType.INIT);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));
    }

    @Test
    public void testExternalRequest_init_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(
                createRequest("/foo", RequestType.INIT)));
    }

    @Test
    public void testSubRequest_other_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        MockHttpServletRequest request = createRequest("/foo", null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));
    }

    @Test
    public void testExternalRequest_other_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        Assert.assertFalse(requestUtil
                .isFrameworkInternalRequest(createRequest("/foo", null)));
    }

    @Route("")
    @AnonymousAllowed
    public static class PublicRootView extends Component {

    }

    @Route("other")
    @AnonymousAllowed
    public static class AnotherPublicView extends Component {

    }

    @Route("admin")
    @RolesAllowed("admin")
    public static class AdminView extends Component {

    }

    @Route("all")
    @PermitAll
    public static class AllUsersView extends Component {

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

        request = createRequest("other");
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

        request = createRequest("other");
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

        request = createRequest("other");
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

        request = createRequest("other");
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
        request = createRequest("other");
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
        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testApplyUrlMapping_fooMappedServlet_prependMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        Assert.assertEquals("/foo/bar", requestUtil.applyUrlMapping("bar"));
        Assert.assertEquals("/foo/bar", requestUtil.applyUrlMapping("/bar"));
        Assert.assertEquals("/foo/bar/", requestUtil.applyUrlMapping("bar/"));
        Assert.assertEquals("/foo/bar/", requestUtil.applyUrlMapping("/bar/"));
        Assert.assertEquals("/foo/bar/baz",
                requestUtil.applyUrlMapping("bar/baz"));
        Assert.assertEquals("/foo/bar/baz",
                requestUtil.applyUrlMapping("/bar/baz"));
        Assert.assertEquals("/foo/", requestUtil.applyUrlMapping(""));
        Assert.assertEquals("/foo/", requestUtil.applyUrlMapping("/"));
        Assert.assertEquals("/foo/", requestUtil.applyUrlMapping(null));
    }

    @Test
    public void testApplyUrlMapping_rootMappedServlet_prependMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        Assert.assertEquals("/bar", requestUtil.applyUrlMapping("bar"));
        Assert.assertEquals("/bar", requestUtil.applyUrlMapping("/bar"));
        Assert.assertEquals("/bar/", requestUtil.applyUrlMapping("bar/"));
        Assert.assertEquals("/bar/", requestUtil.applyUrlMapping("/bar/"));
        Assert.assertEquals("/bar/baz", requestUtil.applyUrlMapping("bar/baz"));
        Assert.assertEquals("/bar/baz",
                requestUtil.applyUrlMapping("/bar/baz"));
        Assert.assertEquals("/", requestUtil.applyUrlMapping(""));
        Assert.assertEquals("/", requestUtil.applyUrlMapping("/"));
        Assert.assertEquals("/", requestUtil.applyUrlMapping(null));
    }

    @Test
    public void testApplyUrlMapping_nullMappedServlet_prependMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn(null);
        Assert.assertEquals("/bar", requestUtil.applyUrlMapping("bar"));
        Assert.assertEquals("/bar", requestUtil.applyUrlMapping("/bar"));
        Assert.assertEquals("/bar/", requestUtil.applyUrlMapping("bar/"));
        Assert.assertEquals("/bar/", requestUtil.applyUrlMapping("/bar/"));
        Assert.assertEquals("/bar/baz", requestUtil.applyUrlMapping("bar/baz"));
        Assert.assertEquals("/bar/baz",
                requestUtil.applyUrlMapping("/bar/baz"));
        Assert.assertEquals("/", requestUtil.applyUrlMapping(""));
        Assert.assertEquals("/", requestUtil.applyUrlMapping("/"));
        Assert.assertEquals("/", requestUtil.applyUrlMapping(null));
    }

    @Test
    public void testAnonymousRouteRequest_accessControlDisable_notAnonymous() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, AnotherPublicView.class);
        addRoute(servlet, AdminView.class);

        MockHttpServletRequest request = createRequest("admin");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));

        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));

        accessControl.setEnabled(false);
        try {
            request = createRequest("admin");
            request.setServletPath("/");
            Assert.assertFalse(requestUtil.isAnonymousRoute(request));

            request = createRequest("other");
            request.setServletPath("/");
            Assert.assertFalse(requestUtil.isAnonymousRoute(request));
        } finally {
            accessControl.setEnabled(true);
        }
    }

    @Test
    public void testFlowRoute_rootMappedServlet_publicView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));

        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));
    }

    @Test
    public void testFlowRoute_rootMappedServlet_notAView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isSecuredFlowRoute(request));

        request = createRequest("other");
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isSecuredFlowRoute(request));
    }

    @Test
    public void testFlowRoute_rootMappedServlet_privateView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, AdminView.class);
        addRoute(servlet, AllUsersView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/admin");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));

        request = createRequest(null);
        request.setServletPath("/all");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));
    }

    @Test
    public void testFlowRoute_fooMappedServlet_publicView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));

        request = createRequest("");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));

        request = createRequest("/");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));

        request = createRequest("other");
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));
    }

    @Test
    public void testFlowRoute_fooMappedServlet_notAView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isSecuredFlowRoute(request));

        request = createRequest("other");
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isSecuredFlowRoute(request));
    }

    @Test
    public void testFlowRoute_fooMappedServlet_privateView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, AdminView.class);
        addRoute(servlet, AllUsersView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/admin");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));

        request = createRequest(null);
        request.setServletPath("/foo/all");
        Assert.assertTrue(requestUtil.isSecuredFlowRoute(request));
    }

    @Test
    public void testFlowRoute_fooMappedServlet_publicViewPathOutsideServlet() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        SpringServlet servlet = setupMockServlet();
        addRoute(servlet, PublicRootView.class);
        addRoute(servlet, AnotherPublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isSecuredFlowRoute(request));
        request = createRequest("other");
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isSecuredFlowRoute(request));
    }

    private SpringServlet setupMockServlet() {
        return setupMockServlet(true);
    }

    private SpringServlet setupMockServlet(boolean inited) {
        SpringServlet servlet = Mockito.mock(SpringServlet.class);
        SpringVaadinServletService service = Mockito
                .mock(SpringVaadinServletService.class);
        Router router = Mockito.mock(Router.class);
        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        DeploymentConfiguration deploymentConfiguration = Mockito
                .mock(DeploymentConfiguration.class);

        Mockito.when(springServletRegistration.getServlet())
                .thenReturn(servlet);
        if (inited) {
            Mockito.when(servlet.getService()).thenReturn(service);
        }
        Mockito.when(service.getRouter()).thenReturn(router);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(deploymentConfiguration);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);
        return servlet;
    }

    @SafeVarargs
    private void registerParentLayout(SpringServlet servlet,
            Class<? extends Component>... views) {
        List<RouteData> routeDataList = new ArrayList<>();
        for (Class<? extends Component> view : views) {
            Optional<Route> route = AnnotationReader.getAnnotationFor(view,
                    Route.class);

            if (route.isEmpty()) {
                throw new IllegalArgumentException(
                        "Unable find a @Route annotation");
            }

            if (route.get().layout() != UI.class) {
                // supports only one
                RouteData routeData = Mockito.mock(RouteData.class);
                Mockito.when(routeData.getNavigationTarget())
                        .thenReturn((Class) view);
                Mockito.when(routeData.getParentLayouts())
                        .thenReturn(List.of(route.get().layout()));
                routeDataList.add(routeData);
            }
        }
        Mockito.when(servlet.getService().getRouter().getRegistry()
                .getRegisteredRoutes()).thenReturn(routeDataList);
    }

    private void addRoute(SpringServlet servlet,
            Class<? extends Component> view) {

        Optional<Route> route = AnnotationReader.getAnnotationFor(view,
                Route.class);

        if (!route.isPresent()) {
            throw new IllegalArgumentException(
                    "Unable find a @Route annotation");
        }

        String path = RouteUtil.getRoutePath(new MockVaadinContext(), view);
        RouteRegistry routeRegistry = servlet.getService().getRouter()
                .getRegistry();
        RouteTarget publicRouteTarget = Mockito.mock(RouteTarget.class);
        NavigationRouteTarget navigationTarget = Mockito
                .mock(NavigationRouteTarget.class);

        Mockito.when(routeRegistry.getNavigationRouteTarget(path))
                .thenReturn(navigationTarget);
        Mockito.when(navigationTarget.getRouteTarget())
                .thenReturn(publicRouteTarget);
        Mockito.when(navigationTarget.getRouteParameters())
                .thenReturn(RouteParameters.empty());

        Mockito.when(publicRouteTarget.getTarget()).thenReturn((Class) view);

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

}
