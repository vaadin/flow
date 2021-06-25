package com.vaadin.flow.spring.security;

import java.util.Collections;
import java.util.Map;

import javax.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.router.internal.NavigationRouteTarget;
import com.vaadin.flow.router.internal.RouteTarget;
import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.fusion.VaadinConnectControllerConfiguration;
import com.vaadin.fusion.VaadinEndpointProperties;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;
import com.vaadin.flow.spring.SpringServlet;
import com.vaadin.flow.spring.SpringVaadinServletService;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { VaadinEndpointProperties.class })
@ContextConfiguration(classes = { VaadinConnectControllerConfiguration.class,
        SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class })
public class RequestUtilTest {

    @Autowired
    RequestUtil requestUtil;

    @MockBean
    VaadinConfigurationProperties vaadinConfigurationProperties;

    @MockBean
    private ServletRegistrationBean<SpringServlet> springServletRegistration;

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
        Assert.assertTrue(requestUtil.isFrameworkInternalRequest(
                createRequest("/bar", RequestType.INIT)));
    }

    @Test
    public void testRootRequest_other_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        Assert.assertFalse(requestUtil
                .isFrameworkInternalRequest(createRequest("/bar", null)));
    }

    @Test
    public void testSubRequest_init_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(
                createRequest("/bar/foo", RequestType.INIT)));
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
        Assert.assertFalse(requestUtil
                .isFrameworkInternalRequest(createRequest("/bar/foo", null)));
    }

    @Test
    public void testExternalRequest_other_customMapping() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/bar/*");
        Assert.assertFalse(requestUtil
                .isFrameworkInternalRequest(createRequest("/foo", null)));
    }

    @Test
    public void testAnonymousRouteRequest_rootServlet() {

    }

    @Route("")
    @AnonymousAllowed
    public static class PublicView extends Component {

    }

    @Route("admin")
    @RolesAllowed("admin")
    public static class AdminView extends Component {

    }

    @Test
    public void testAnonymousRouteRequest_rootMappedServlet_publicView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        setupMockServlet("/", PublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_rootMappedServlet_notAView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        setupMockServlet("/", PublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_rootMappedServlet_privateView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/*");
        setupMockServlet("/admin", AdminView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/admin");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_publicView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        setupMockServlet("", PublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/");
        Assert.assertTrue(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_notAView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        setupMockServlet("", PublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/bar");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_privateView() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        setupMockServlet("admin", AdminView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/foo/admin");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    @Test
    public void testAnonymousRouteRequest_fooMappedServlet_publicViewPathOutsideServlet() {
        Mockito.when(vaadinConfigurationProperties.getUrlMapping())
                .thenReturn("/foo/*");
        setupMockServlet("", PublicView.class);

        MockHttpServletRequest request = createRequest(null);
        request.setServletPath("/");
        Assert.assertFalse(requestUtil.isAnonymousRoute(request));
    }

    private void setupMockServlet(String viewRoute,
            Class<? extends Component> view) {
        SpringServlet servlet = Mockito.mock(SpringServlet.class);
        SpringVaadinServletService service = Mockito
                .mock(SpringVaadinServletService.class);
        Router router = Mockito.mock(Router.class);
        RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
        NavigationRouteTarget publicNavigationTarget = Mockito
                .mock(NavigationRouteTarget.class);
        RouteTarget publicRouteTarget = Mockito.mock(RouteTarget.class);

        Mockito.when(springServletRegistration.getServlet())
                .thenReturn(servlet);
        Mockito.when(servlet.getService()).thenReturn(service);
        Mockito.when(service.getRouter()).thenReturn(router);
        Mockito.when(router.getRegistry()).thenReturn(routeRegistry);
        Mockito.when(routeRegistry.getNavigationRouteTarget(viewRoute))
                .thenReturn(publicNavigationTarget);
        Mockito.when(publicNavigationTarget.getRouteTarget())
                .thenReturn(publicRouteTarget);
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
