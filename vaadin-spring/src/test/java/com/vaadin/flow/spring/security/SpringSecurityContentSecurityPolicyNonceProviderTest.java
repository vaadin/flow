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

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.configuration.ObjectPostProcessorConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer.AuthorizedUrl;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.flow.internal.ContentSecurityPolicyNonceProvider;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.internal.hilla.FileRouterRequestUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class,
        ObjectPostProcessorConfiguration.class,
        VaadinSecurityConfigurerTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
class SpringSecurityContentSecurityPolicyNonceProviderTest {

    private static final String HEADER = "Content-Security-Policy";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectPostProcessor<Object> postProcessor;

    @Autowired
    private PathPatternRequestMatcher.Builder requestMatcherBuilder;

    @Autowired
    private ContentSecurityPolicyNonceProvider nonceProvider;

    @MockitoBean
    private EndpointRequestUtil endpointRequestUtil;

    @MockitoBean
    private FileRouterRequestUtil fileRouterRequestUtil;

    private HttpSecurity http;

    @BeforeEach
    void setUp() {
        var authManagerBuilder = new AuthenticationManagerBuilder(postProcessor)
                .authenticationProvider(new TestingAuthenticationProvider());
        http = new HttpSecurity(postProcessor, authManagerBuilder,
                Map.of(ApplicationContext.class, applicationContext,
                        PathPatternRequestMatcher.Builder.class,
                        requestMatcherBuilder));
    }

    @Test
    void policyInSpringSecurity_nonceIsTheOneOfThePolicySentWithTheResponse()
            throws Exception {
        http.headers(headers -> headers
                .contentSecurityPolicy(policy -> policy.policyDirectives(
                        "script-src 'nonce-{nonce}' 'strict-dynamic'")));

        List<String> nonces = new ArrayList<>();
        MockHttpServletResponse response = serve(nonces);

        assertEquals(1, nonces.size());
        assertEquals(22, nonces.get(0).length(), nonces.get(0));
        assertEquals(List.of(
                "script-src 'nonce-" + nonces.get(0) + "' 'strict-dynamic'"),
                response.getHeaders(HEADER));
    }

    @Test
    void policyInSpringSecurityForOtherPaths_noPolicyIsSent() throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(policy -> policy
                .policyDirectives("script-src 'nonce-{nonce}' 'strict-dynamic'")
                .requestMatchers("/api/**")));

        List<String> nonces = new ArrayList<>();
        MockHttpServletResponse response = serve(nonces);

        // Spring Security creates the nonce for every request, and decides
        // where its policy applies
        assertEquals(22, nonces.get(0).length(), nonces.get(0));
        assertEquals(List.of(), response.getHeaders(HEADER));
    }

    @Test
    void noPolicyInSpringSecurity_noNonce() throws Exception {
        http.headers(Customizer.withDefaults());

        List<String> nonces = new ArrayList<>();
        serve(nonces);

        // The policy and its nonce are then left to Vaadin
        assertNull(nonces.get(0));
    }

    /**
     * Sends a request through the security filter chain to a servlet that asks
     * for the nonce of the request, as Vaadin does when it creates a page.
     */
    private MockHttpServletResponse serve(List<String> nonces)
            throws Exception {
        List<Filter> filters = http
                .with(VaadinSecurityConfigurer.vaadin(),
                        vaadin -> vaadin.anyRequest(AuthorizedUrl::permitAll))
                .build().getFilters();
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request,
                    HttpServletResponse response) {
                VaadinRequest vaadinRequest = mock(VaadinRequest.class);
                when(vaadinRequest.getAttribute(anyString())).thenAnswer(
                        call -> request.getAttribute(call.getArgument(0)));
                nonces.add(nonceProvider.findNonce(vaadinRequest));
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MockFilterChain(servlet, filters.toArray(Filter[]::new))
                .doFilter(new MockHttpServletRequest("GET", "/"), response);
        return response;
    }
}
