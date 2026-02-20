/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

import com.vaadin.flow.server.auth.NavigationAccessControl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VaadinSavedRequestAwareAuthenticationSuccessHandlerTest {

    private VaadinSavedRequestAwareAuthenticationSuccessHandler vaadinSavedRequestAwareAuthenticationSuccessHandler;

    @BeforeEach
    void init() {
        vaadinSavedRequestAwareAuthenticationSuccessHandler = new VaadinSavedRequestAwareAuthenticationSuccessHandler();
    }

    @Test
    void standard_result_for_non_typescript_Clients() throws Exception {
        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        assertEquals("/", loginResponse.getHeader("Location"));
    }

    @Test
    void directLogin_nonTypescriptClientsAndDefaultTargetUrl_redirectToDefaultTargetUrl()
            throws Exception {
        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .setDefaultTargetUrl("/foo");
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        assertEquals("/foo", loginResponse.getHeader("Location"));
    }

    @Test
    void savedUrl_nonTypescriptClients_alwaysUseDefaultTargetUrl_redirectToDefaultTargetUrl()
            throws Exception {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        MockHttpServletRequest firstRequest = RequestUtilTest
                .createRequest("/the-saved-url");
        HttpSession session = firstRequest.getSession();
        cache.saveRequest(firstRequest, new MockHttpServletResponse());

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.setSession(session);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .setAlwaysUseDefaultTargetUrl(true);
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .setDefaultTargetUrl("/foo");
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        assertEquals("/foo", loginResponse.getHeader("Location"));
    }

    @Test
    void savedUrl_nonTypescriptClients_targetParameter_redirectToTargetParameter()
            throws Exception {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        MockHttpServletRequest firstRequest = RequestUtilTest
                .createRequest("/the-saved-url");
        HttpSession session = firstRequest.getSession();
        cache.saveRequest(firstRequest, new MockHttpServletResponse());

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.setParameter("Saved-url", "/foo");
        loginRequest.setSession(session);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        assertEquals("/foo", loginResponse.getHeader("Location"));
    }

    @Test
    void savedUrl_nonTypescriptClients_redirectToSavedUrl() throws Exception {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        MockHttpServletRequest firstRequest = RequestUtilTest
                .createRequest("/the-saved-url");
        HttpSession session = firstRequest.getSession();
        cache.saveRequest(firstRequest, new MockHttpServletResponse());

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.setSession(session);

        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        // HttpSessionRequestCache uses request parameter "continue",
        // see HttpSessionRequestCache::setMatchingRequestParameterName
        assertEquals("http://localhost/the-saved-url?continue",
                loginResponse.getHeader("Location"));
    }

    @Test
    void viewAccessCheckerSavedUrl_nonTypescriptClients_redirectToSessionStoredUrl()
            throws Exception {

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        HttpSession session = loginRequest.getSession();
        // Simulate NavigationAccessControl
        session.setAttribute(
                NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE,
                "http://localhost/last-route");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        assertEquals("http://localhost/last-route",
                loginResponse.getHeader("Location"));
    }

    @Test
    void savedUrlAndViewAccessCheckerSavedUrl_nonTypescriptClients_redirectToSavedUrl()
            throws Exception {
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        MockHttpServletRequest firstRequest = RequestUtilTest
                .createRequest("/a-previous-saved-url");
        HttpSession session = firstRequest.getSession();
        cache.saveRequest(firstRequest, new MockHttpServletResponse());
        // Simulate NavigationAccessControl
        session.setAttribute(
                NavigationAccessControl.SESSION_STORED_REDIRECT_ABSOLUTE,
                "http://localhost/last-route");

        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.setSession(session);
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertNull(loginResponse.getHeader("Result"));
        assertEquals(302, loginResponse.getStatus());
        // HttpSessionRequestCache uses request parameter "continue",
        // see HttpSessionRequestCache::setMatchingRequestParameterName
        assertEquals("http://localhost/a-previous-saved-url?continue",
                loginResponse.getHeader("Location"));
    }

    @Test
    void success_result_for_typescript_client() throws Exception {
        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.addHeader("source", "typescript");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertEquals("success", loginResponse.getHeader("Result"));
        assertEquals(200, loginResponse.getStatus());
        assertNull(loginResponse.getHeader("Saved-url"));
        assertEquals("/", loginResponse.getHeader("Default-url"));
    }

    @Test
    void saved_url_sent_to_typescript_client() throws Exception {
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

        assertEquals("success", loginResponse.getHeader("Result"));
        assertEquals(200, loginResponse.getStatus());
        // HttpSessionRequestCache uses request parameter "continue",
        // see HttpSessionRequestCache::setMatchingRequestParameterName
        assertEquals("http://localhost/the-saved-url?continue",
                loginResponse.getHeader("Saved-url"));
        assertEquals("/", loginResponse.getHeader("Default-url"));
    }

    @Test
    void csrfs_sent_to_typescript_client() throws Exception {
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

        assertEquals("success", loginResponse.getHeader("Result"));
        assertEquals(200, loginResponse.getStatus());

        assertEquals("spring-csrf-header-name",
                loginResponse.getHeader("Spring-CSRF-header"));
        assertEquals("spring-csrf-token-value",
                loginResponse.getHeader("Spring-CSRF-token"));
    }

    @Test
    void csrfs_sent_to_typescript_client_stateless() throws Exception {
        DefaultCsrfToken springCsrfToken = new DefaultCsrfToken(
                "spring-csrf-header-name", "spring-csrf-parameter-name",
                "spring-csrf-token-value");

        var mockCsrfTokenRepository = Mockito.mock(CsrfTokenRepository.class);
        Mockito.when(mockCsrfTokenRepository.generateToken(Mockito.any()))
                .thenReturn(springCsrfToken);
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .setCsrfTokenRepository(mockCsrfTokenRepository);
        MockHttpServletRequest loginRequest = RequestUtilTest
                .createRequest("/login");
        loginRequest.addHeader("source", "typescript");

        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        vaadinSavedRequestAwareAuthenticationSuccessHandler
                .onAuthenticationSuccess(loginRequest, loginResponse,
                        new UsernamePasswordAuthenticationToken("foo", "bar"));

        assertEquals("success", loginResponse.getHeader("Result"));
        assertEquals(200, loginResponse.getStatus());

        assertEquals("spring-csrf-header-name",
                loginResponse.getHeader("Spring-CSRF-header"));
        assertEquals("spring-csrf-token-value",
                loginResponse.getHeader("Spring-CSRF-token"));

        Mockito.verify(mockCsrfTokenRepository, Mockito.times(1))
                .generateToken(loginRequest);
        Mockito.verify(mockCsrfTokenRepository, Mockito.times(1))
                .saveToken(springCsrfToken, loginRequest, loginResponse);
    }
}
