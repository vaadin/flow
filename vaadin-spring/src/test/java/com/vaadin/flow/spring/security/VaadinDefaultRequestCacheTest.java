package com.vaadin.flow.spring.security;

import java.lang.reflect.Method;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.fusion.Endpoint;
import com.vaadin.fusion.EndpointRegistry;
import com.vaadin.fusion.VaadinConnectControllerConfiguration;
import com.vaadin.fusion.VaadinEndpointProperties;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { VaadinEndpointProperties.class })
@ContextConfiguration(classes = { VaadinConnectControllerConfiguration.class,
        SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class })
public class VaadinDefaultRequestCacheTest {

    @Autowired
    VaadinDefaultRequestCache cache;
    @Autowired
    EndpointRegistry endpointRegistry;
    @Autowired
    RequestUtil requestUtil;

    @Test
    public void normalRouteRequestSaved() {
        HttpServletRequest request = RequestUtilTest
                .createRequest("/hello-world", null);
        HttpServletResponse response = createResponse();

        Assert.assertNull(cache.getRequest(request, response));
        cache.saveRequest(request, response);
        Assert.assertNotNull(cache.getRequest(request, response));
    }

    @Test
    public void internalRequestsNotSaved() {
        HttpServletRequest request = RequestUtilTest.createRequest(null,
                RequestType.INIT);
        HttpServletResponse response = createResponse();
        Assert.assertTrue(requestUtil.isFrameworkInternalRequest(request));
        cache.saveRequest(request, response);
        Assert.assertNull(cache.getRequest(request, response));
    }

    @Test
    public void serviceWorkerRequestNotSaved() {
        HttpServletRequest request = RequestUtilTest.createRequest("", null,
                Collections.singletonMap("Referer",
                        "https://labs.vaadin.com/business/sw.js"));
        HttpServletResponse response = createResponse();
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));
        cache.saveRequest(request, response);
        Assert.assertNull(cache.getRequest(request, response));
    }

    @Endpoint
    public static class FakeEndpoint {
        public void fakeMethod() {
        }
    }

    @Test
    public void endpointRequestNotSaved() throws Exception {
        HttpServletRequest request = RequestUtilTest
                .createRequest("/connect/fakeendpoint/fakemethod");
        HttpServletResponse response = createResponse();
        Method registerMethod = EndpointRegistry.class
                .getDeclaredMethod("registerEndpoint", Object.class);
        registerMethod.setAccessible(true);
        registerMethod.invoke(endpointRegistry, new FakeEndpoint());

        cache.saveRequest(request, response);
        Assert.assertNull(cache.getRequest(request, response));
    }

    private HttpServletResponse createResponse() {
        return Mockito.mock(HttpServletResponse.class);
    }

}
