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
import org.mockito.ArgumentCaptor;
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
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import com.vaadin.flow.internal.hilla.EndpointRequestUtil;
import com.vaadin.flow.internal.hilla.FileRouterRequestUtil;
import com.vaadin.flow.server.ContentSecurityPolicy;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import com.vaadin.flow.spring.SpringBootAutoConfiguration;
import com.vaadin.flow.spring.SpringSecurityAutoConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebAppConfiguration
@ContextConfiguration(classes = { SpringBootAutoConfiguration.class,
        SpringSecurityAutoConfiguration.class,
        ObjectPostProcessorConfiguration.class,
        VaadinSecurityConfigurerTest.TestConfig.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
class SpringSecurityContentSecurityPolicyInitializerTest {

    private static final String SPRING_POLICY = "img-src https://img.example.com; frame-ancestors 'none'";
    private static final String VAADIN_POLICY = "script-src 'nonce-abc'";
    private static final String HEADER = "Content-Security-Policy";

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ObjectPostProcessor<Object> postProcessor;

    @Autowired
    private PathPatternRequestMatcher.Builder requestMatcherBuilder;

    @Autowired
    private SpringSecurityContentSecurityPolicyInitializer initializer;

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
    void policyInSpringSecurity_vaadinPageGetsItMergedAndOtherResponsesGetItAsIs()
            throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(
                policy -> policy.policyDirectives(SPRING_POLICY)));

        MockHttpServletResponse vaadinPage = serve(true);
        assertEquals(List.of(VAADIN_POLICY + "+" + SPRING_POLICY),
                vaadinPage.getHeaders(HEADER));
        // The other headers of Spring Security are not affected
        assertEquals("nosniff", vaadinPage.getHeader("X-Content-Type-Options"));

        assertEquals(List.of(SPRING_POLICY), serve(false).getHeaders(HEADER));
    }

    @Test
    void policyInSpringSecurityWrittenEagerly_vaadinPageGetsItMerged()
            throws Exception {
        http.headers(headers -> headers
                .contentSecurityPolicy(
                        policy -> policy.policyDirectives(SPRING_POLICY))
                .withObjectPostProcessor(writeHeadersEagerly()));

        assertEquals(List.of(VAADIN_POLICY + "+" + SPRING_POLICY),
                serve(true).getHeaders(HEADER));
    }

    @Test
    void noPolicyInSpringSecurity_vaadinPolicyUnchanged() throws Exception {
        http.headers(Customizer.withDefaults());

        assertEquals(List.of(VAADIN_POLICY), serve(true).getHeaders(HEADER));
    }

    @Test
    void reportOnlyPolicyInSpringSecurity_notMergedAndStillSent()
            throws Exception {
        http.headers(headers -> headers.contentSecurityPolicy(
                policy -> policy.policyDirectives(SPRING_POLICY).reportOnly()));

        MockHttpServletResponse vaadinPage = serve(true);
        assertEquals(List.of(VAADIN_POLICY), vaadinPage.getHeaders(HEADER));
        assertEquals(List.of(SPRING_POLICY),
                vaadinPage.getHeaders(HEADER + "-Report-Only"));
    }

    @Test
    void notStrictMode_nothingMerged() {
        IndexHtmlResponse response = mock(IndexHtmlResponse.class);
        when(response.getContentSecurityPolicy()).thenReturn(null);

        registeredListener().modifyIndexHtmlResponse(response);

        verify(response, never()).mergeContentSecurityPolicy(anyString());
    }

    /**
     * Sends a request through the security filter chain to a servlet that
     * stands in for Vaadin. For a Vaadin page, it runs the listener the way
     * {@code IndexHtmlRequestHandler} does and then sets the header of the
     * strict mode, joining the merged policies with a plus sign.
     */
    private MockHttpServletResponse serve(boolean vaadinPage) throws Exception {
        if (filters == null) {
            filters = http
                    .with(VaadinSecurityConfigurer.vaadin(),
                            vaadin -> vaadin
                                    .anyRequest(AuthorizedUrl::permitAll))
                    .build().getFilters();
        }
        IndexHtmlRequestListener listener = registeredListener();
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request,
                    HttpServletResponse response) {
                if (!vaadinPage) {
                    return;
                }
                List<String> merged = new ArrayList<>(List.of(VAADIN_POLICY));
                IndexHtmlResponse indexHtmlResponse = mock(
                        IndexHtmlResponse.class);
                VaadinRequest vaadinRequest = mock(VaadinRequest.class);
                when(vaadinRequest.getAttribute(anyString())).thenAnswer(
                        call -> request.getAttribute(call.getArgument(0)));
                when(indexHtmlResponse.getVaadinRequest())
                        .thenReturn(vaadinRequest);
                when(indexHtmlResponse.getContentSecurityPolicy())
                        .thenReturn(new ContentSecurityPolicy());
                when(indexHtmlResponse.mergeContentSecurityPolicy(anyString()))
                        .thenAnswer(call -> {
                            merged.add(call.getArgument(0));
                            return List.of();
                        });
                listener.modifyIndexHtmlResponse(indexHtmlResponse);
                response.setHeader(HEADER, String.join("+", merged));
            }
        };
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new MockFilterChain(servlet, filters.toArray(Filter[]::new))
                .doFilter(request, response);
        return response;
    }

    private IndexHtmlRequestListener registeredListener() {
        ServiceInitEvent event = mock(ServiceInitEvent.class);
        initializer.serviceInit(event);
        ArgumentCaptor<IndexHtmlRequestListener> listener = ArgumentCaptor
                .forClass(IndexHtmlRequestListener.class);
        verify(event).addIndexHtmlRequestListener(listener.capture());
        return listener.getValue();
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
