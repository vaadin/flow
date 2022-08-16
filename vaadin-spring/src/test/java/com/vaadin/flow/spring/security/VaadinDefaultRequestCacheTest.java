package com.vaadin.flow.spring.security;

import java.util.Collections;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class })
public class VaadinDefaultRequestCacheTest {

    @Autowired
    VaadinDefaultRequestCache cache;
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

    @Test
    public void errorRequestNotSaved() {
        HttpServletRequest request = RequestUtilTest.createRequest("error",
                null);
        HttpServletResponse response = createResponse();
        Assert.assertFalse(requestUtil.isFrameworkInternalRequest(request));
        cache.saveRequest(request, response);
        Assert.assertNull(cache.getRequest(request, response));
    }

    @Test
    public void getRequest_uses_delegateRequestCache() throws Exception {
        HttpServletRequest request = RequestUtilTest
                .createRequest("/hello-world", null);
        HttpServletResponse response = createResponse();
        SavedRequest expectedSavedRequest = Mockito.mock(SavedRequest.class);
        RequestCache delegateRequestCache = Mockito.mock(RequestCache.class);
        Mockito.doReturn(expectedSavedRequest).when(delegateRequestCache)
                .getRequest(request, response);
        cache.setDelegateRequestCache(delegateRequestCache);

        SavedRequest actualSavedRequest = cache.getRequest(request, response);
        Mockito.verify(delegateRequestCache).getRequest(request, response);
        Assert.assertEquals(expectedSavedRequest, actualSavedRequest);

        cache.setDelegateRequestCache(new HttpSessionRequestCache());
    }

    @Test
    public void getMatchingRequest_uses_delegateRequestCache()
            throws Exception {
        HttpServletRequest request = RequestUtilTest
                .createRequest("/hello-world", null);
        HttpServletResponse response = createResponse();
        HttpServletRequest expectedMachingRequest = RequestUtilTest
                .createRequest("", null);
        RequestCache delegateRequestCache = Mockito.mock(RequestCache.class);
        Mockito.doReturn(expectedMachingRequest).when(delegateRequestCache)
                .getMatchingRequest(request, response);
        cache.setDelegateRequestCache(delegateRequestCache);

        HttpServletRequest actualMatchingRequest = cache
                .getMatchingRequest(request, response);
        Mockito.verify(delegateRequestCache).getMatchingRequest(request,
                response);
        Assert.assertEquals(expectedMachingRequest, actualMatchingRequest);

        cache.setDelegateRequestCache(new HttpSessionRequestCache());
    }

    @Test
    public void saveRequest_uses_delegateRequestCache() throws Exception {
        HttpServletRequest request = RequestUtilTest
                .createRequest("/hello-world", null);
        HttpServletResponse response = createResponse();
        RequestCache delegateRequestCache = Mockito.mock(RequestCache.class);
        cache.setDelegateRequestCache(delegateRequestCache);

        cache.saveRequest(request, response);
        Mockito.verify(delegateRequestCache).saveRequest(request, response);

        cache.setDelegateRequestCache(new HttpSessionRequestCache());
    }

    @Test
    public void removeRequest_uses_delegateRequestCache() throws Exception {
        HttpServletRequest request = RequestUtilTest
                .createRequest("/hello-world", null);
        HttpServletResponse response = createResponse();
        RequestCache delegateRequestCache = Mockito.mock(RequestCache.class);
        cache.setDelegateRequestCache(delegateRequestCache);

        cache.removeRequest(request, response);
        Mockito.verify(delegateRequestCache).removeRequest(request, response);

        cache.setDelegateRequestCache(new HttpSessionRequestCache());
    }

    private HttpServletResponse createResponse() {
        return Mockito.mock(HttpServletResponse.class);
    }

}
