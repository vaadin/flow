package com.vaadin.flow.spring.security;

import javax.servlet.http.HttpSession;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.csrf.DefaultCsrfToken;

public class VaadinSavedRequestAwareAuthenticationSuccessHandlerTest {

    private VaadinSavedRequestAwareAuthenticationSuccessHandler vaadinSavedRequestAwareAuthenticationSuccessHandler;

    @Before
    public void init() {
        vaadinSavedRequestAwareAuthenticationSuccessHandler = new VaadinSavedRequestAwareAuthenticationSuccessHandler();
    }

    @Test
    public void standard_result_for_non_typescript_Clients() throws Exception {
        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        Assert.assertNull(loginResponse.getHeader("Result"));
        Assert.assertEquals(302, loginResponse.getStatus());
    }

    @Test
    public void success_result_for_typescript_client() throws Exception {
        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.addHeader("source", "typescript");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        Assert.assertEquals("success", loginResponse.getHeader("Result"));
        Assert.assertEquals(200, loginResponse.getStatus());
        Assert.assertNull(loginResponse.getHeader("Saved-url"));
        Assert.assertEquals("/", loginResponse.getHeader("Default-url"));
    }

    @Test
    public void saved_url_sent_to_typescript_client() throws Exception {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        MockHttpServletRequest firstRequest = RequestUtilTest
                .createRequest("/the-saved-url");
        HttpSession session = firstRequest.getSession();
        cache.saveRequest(firstRequest, new MockHttpServletResponse());

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.addHeader("source", "typescript");
        loginRequest.setSession(session);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        Assert.assertEquals("success", loginResponse.getHeader("Result"));
        Assert.assertEquals(200, loginResponse.getStatus());
        Assert.assertEquals("http://localhost/the-saved-url",
                loginResponse.getHeader("Saved-url"));
        Assert.assertEquals("/", loginResponse.getHeader("Default-url"));
    }

    @Test
    public void csrfs_sent_to_typescript_client() throws Exception {
        DefaultCsrfToken springCsrfToken = new DefaultCsrfToken(
                "spring-csrf-header-name", "spring-csrf-parameter-name",
                "spring-csrf-token-value");

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.addHeader("source", "typescript");
        loginRequest.setAttribute(CsrfToken.class.getName(), springCsrfToken);

        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        Assert.assertEquals("success", loginResponse.getHeader("Result"));
        Assert.assertEquals(200, loginResponse.getStatus());

        Assert.assertEquals("spring-csrf-header-name",
                loginResponse.getHeader("Spring-CSRF-header"));
        Assert.assertEquals("spring-csrf-token-value",
                loginResponse.getHeader("Spring-CSRF-token"));
    }
}
