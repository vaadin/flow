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
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.flow.internal.ContentSecurityPolicyHeaderDelegate;
import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.internal.hilla.FileRouterRequestUtil;
import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@WebAppConfiguration
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class,
        ObjectPostProcessorConfiguration.class,
        VaadinSecurityConfigurerTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
class StrictContentSecurityPolicyHeaderWriterTest {

    private static final String HEADER = "Content-Security-Policy";
    private static final String SPRING_POLICY = "IMG-SRC https://img.example.com;; "
            + "img-src https://repeated.example.com; "
            + "default-src https://cdn.example.com; "
            + "script-src 'self' 'unsafe-inline'; base-uri 'none'; "
            + "media-src 'none'; frame-ancestors 'none'; report-to csp";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectPostProcessor<Object> postProcessor;

    @Autowired
    private PathPatternRequestMatcher.Builder requestMatcherBuilder;

    @MockitoBean
    private EndpointRequestUtil endpointRequestUtil;

    @MockitoBean
    private FileRouterRequestUtil fileRouterRequestUtil;

    private HttpSecurity http;

    private List<Filter> filters;

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
    void policyInSpringSecurity_vaadinPageGetsOneMergedHeaderAndOtherResponsesGetItAsIs()
            throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(
                policy -> policy.policyDirectives(SPRING_POLICY)));

        MockHttpServletResponse vaadinPage = serve(true);
        assertEquals(List.of("default-src 'self' https://cdn.example.com; "
                + "script-src 'nonce-abc' 'strict-dynamic'; "
                + "style-src 'self' 'unsafe-inline' https://cdn.example.com; "
                + "img-src 'self' data: https://img.example.com; "
                + "connect-src 'self' https://cdn.example.com; "
                + "worker-src 'self'; object-src 'none'; base-uri 'self'; "
                + "media-src 'none'; frame-ancestors 'none'; report-to csp"),
                vaadinPage.getHeaders(HEADER));
        // The other headers of Spring Security are not affected
        assertEquals("nosniff", vaadinPage.getHeader("X-Content-Type-Options"));

        assertEquals(List.of(SPRING_POLICY), serve(false).getHeaders(HEADER));
    }

    @Test
    void policyInSpringSecurityWrittenAtStartOfRequest_vaadinPageGetsOneMergedHeader()
            throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(
                policy -> policy.policyDirectives("frame-ancestors 'none'"))
                .withObjectPostProcessor(writeHeadersEagerly()));

        assertEquals(List.of(vaadinPolicy() + "; frame-ancestors 'none'"),
                serve(true).getHeaders(HEADER));
    }

    @Test
    void noPolicyInSpringSecurity_vaadinPageGetsThePolicyOfVaadin()
            throws Exception {
        http.headers(Customizer.withDefaults());

        assertEquals(List.of(vaadinPolicy()), serve(true).getHeaders(HEADER));
    }

    @Test
    void policyThatCannotBeParsed_vaadinPageGetsThePolicyOfVaadin()
            throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(policy -> policy
                .policyDirectives("img-src 'self', default-src 'none'")));

        assertEquals(List.of(vaadinPolicy()), serve(true).getHeaders(HEADER));
    }

    @Test
    void reportOnlyPolicyInSpringSecurity_notMergedAndStillSent()
            throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(policy -> policy
                .policyDirectives("frame-ancestors 'none'").reportOnly()));

        MockHttpServletResponse vaadinPage = serve(true);
        assertEquals(List.of(vaadinPolicy()), vaadinPage.getHeaders(HEADER));
        assertEquals(List.of("frame-ancestors 'none'"),
                vaadinPage.getHeaders(HEADER + "-Report-Only"));
    }

    @Test
    void headersDisabledInSpringSecurity_headerIsLeftToVaadin()
            throws Exception {
        http.headers(HeadersConfigurer::disable);

        // The stand-in for Vaadin sets this header when nothing takes over
        assertEquals(List.of("written by Vaadin"),
                serve(true).getHeaders(HEADER));
    }

    @Test
    void noHeadersConfiguredInSpringSecurity_headerIsLeftToVaadin()
            throws Exception {
        assertEquals(List.of("written by Vaadin"),
                serve(true).getHeaders(HEADER));
        assertNull(serve(false).getHeader(HEADER));
    }

    /**
     * Sends a request through the security filter chain to a servlet that
     * stands in for Vaadin. For a Vaadin page, the servlet does what
     * {@code IndexHtmlRequestHandler} does in the strict mode: it hands the
     * policy of the page to the delegate of the request, or sets the header
     * itself when the request has none.
     */
    private MockHttpServletResponse serve(boolean vaadinPage) throws Exception {
        if (filters == null) {
            filters = http
                    .with(VaadinSecurityConfigurer.vaadin(),
                            vaadin -> vaadin
                                    .anyRequest(AuthorizedUrl::permitAll))
                    .build().getFilters();
        }
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request,
                    HttpServletResponse response) {
                if (!vaadinPage) {
                    return;
                }
                Object delegate = request.getAttribute(
                        ContentSecurityPolicyHeaderDelegate.class.getName());
                if (delegate instanceof ContentSecurityPolicyHeaderDelegate headerDelegate) {
                    headerDelegate.writeHeader(new ContentSecurityPolicy()
                            .setDirective("default-src", "'self'")
                            .setDirective("script-src", "'nonce-abc'",
                                    "'strict-dynamic'")
                            .setDirective("style-src", "'self'",
                                    "'unsafe-inline'")
                            .setDirective("img-src", "'self'", "data:")
                            .setDirective("connect-src", "'self'")
                            .setDirective("worker-src", "'self'")
                            .setDirective("object-src", "'none'")
                            .setDirective("base-uri", "'self'"));
                } else {
                    response.setHeader(HEADER, "written by Vaadin");
                }
            }
        };
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MockFilterChain(servlet, filters.toArray(Filter[]::new))
                .doFilter(request, response);
        return response;
    }

    private static String vaadinPolicy() {
        return "default-src 'self'; script-src 'nonce-abc' 'strict-dynamic'; "
                + "style-src 'self' 'unsafe-inline'; img-src 'self' data:; "
                + "connect-src 'self'; worker-src 'self'; object-src 'none'; "
                + "base-uri 'self'";
    }

    private static ObjectPostProcessor<HeaderWriterFilter> writeHeadersEagerly() {
        return new ObjectPostProcessor<>() {
            @Override
            public <O extends HeaderWriterFilter> O postProcess(O filter) {
                filter.setShouldWriteHeadersEagerly(true);
                return filter;
            }
        };
    }
}
