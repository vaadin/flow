package com.vaadin.flow.spring.security;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.server.connect.VaadinConnectControllerConfiguration;
import com.vaadin.flow.server.connect.VaadinEndpointProperties;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.VaadinConfigurationProperties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { VaadinEndpointProperties.class })
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        VaadinConnectControllerConfiguration.class })
public class RequestUtilTest {

    @Autowired
    RequestUtil requestUtil;

    @MockBean
    VaadinConfigurationProperties vaadinConfigurationProperties;

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

    static HttpServletRequest createRequest(String pathInfo, RequestType type) {
        return createRequest(pathInfo, type, Collections.emptyMap());
    }

    static HttpServletRequest createRequest(String pathInfo, RequestType type,
            Map<String, String> headers) {
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
