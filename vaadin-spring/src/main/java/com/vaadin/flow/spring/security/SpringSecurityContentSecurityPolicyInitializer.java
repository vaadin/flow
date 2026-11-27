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
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;

/**
 * Combines the Content Security Policy that an application configures in Spring
 * Security with the policy that Vaadin creates for its pages in the strict
 * mode.
 * <p>
 * Spring Security writes its headers when the response is committed and leaves
 * out a header the response already has. Vaadin sets its own
 * {@code Content-Security-Policy} header before that, so without this class the
 * policy from Spring Security would be missing from every Vaadin page. With it,
 * the application configures the policy once, with
 * {@code http.headers(headers -> headers.contentSecurityPolicy(...))}, and the
 * Vaadin page gets that policy merged with the nonce-based script policy, see
 * {@link IndexHtmlResponse#mergeContentSecurityPolicy(String)}.
 * <p>
 * It works in two steps. {@link VaadinSecurityConfigurer} adds a
 * {@link HeaderCaptureFilter} to the security filter chain, which makes the
 * headers of that chain available to the request. When Vaadin then writes its
 * page, this listener reads the policy Spring Security would send for the
 * request and merges it.
 */
public class SpringSecurityContentSecurityPolicyInitializer
        implements VaadinServiceInitListener {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SpringSecurityContentSecurityPolicyInitializer.class);

    private static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";

    // Policies are fixed in the configuration, so each is reported once
    private final Set<String> reportedPolicies = ConcurrentHashMap.newKeySet();

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.addIndexHtmlRequestListener(this::mergeSpringSecurityPolicy);
    }

    private void mergeSpringSecurityPolicy(IndexHtmlResponse response) {
        if (response.getContentSecurityPolicy() == null) {
            return;
        }
        Object capture = response.getVaadinRequest()
                .getAttribute(HeaderCaptureFilter.REQUEST_ATTRIBUTE);
        if (!(capture instanceof HeaderCapture headerCapture)) {
            return;
        }
        for (String policy : headerCapture.get()) {
            List<String> ignored = response.mergeContentSecurityPolicy(policy);
            if (!ignored.isEmpty() && reportedPolicies.add(policy)) {
                LOGGER.warn(
                        """
                                The Content Security Policy configured in Spring Security \
                                sets {}, which is not applied to Vaadin pages. With \
                                contentSecurityPolicy=strict, Vaadin allows scripts only by \
                                a nonce it creates for each response. The other directives \
                                of the policy are applied. Policy: {}""",
                        String.join(", ", ignored), policy);
            }
        }
    }

    /**
     * The {@code Content-Security-Policy} header values that Spring Security
     * sends for a request.
     */
    @FunctionalInterface
    interface HeaderCapture extends Supplier<List<String>> {
    }

    /**
     * Makes the {@code Content-Security-Policy} headers of a security filter
     * chain available to the request, without writing them to the response.
     * <p>
     * Spring Security offers no way to read the header writers it is configured
     * with. The values are found by running its {@link HeaderWriterFilter}
     * again, on a response that records headers instead of sending them. This
     * also covers a policy that only applies to some requests, and one added as
     * a custom header writer.
     */
    static final class HeaderCaptureFilter implements Filter {

        static final String REQUEST_ATTRIBUTE = HeaderCaptureFilter.class
                .getName();

        private @Nullable HeaderWriterFilter headerWriterFilter;

        void setHeaderWriterFilter(HeaderWriterFilter headerWriterFilter) {
            this.headerWriterFilter = headerWriterFilter;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response,
                FilterChain chain) throws IOException, ServletException {
            HeaderWriterFilter filter = headerWriterFilter;
            if (filter != null
                    && request instanceof HttpServletRequest httpRequest
                    && response instanceof HttpServletResponse httpResponse) {
                // Read only when Vaadin asks, since most requests are not for
                // a page
                HeaderCapture capture = () -> captureHeaders(filter,
                        httpRequest, httpResponse);
                request.setAttribute(REQUEST_ATTRIBUTE, capture);
            }
            chain.doFilter(request, response);
        }

        private static List<String> captureHeaders(HeaderWriterFilter filter,
                HttpServletRequest request, HttpServletResponse response) {
            RecordingResponse recording = new RecordingResponse(response);
            try {
                filter.doFilter(new UnfilteredRequest(request), recording,
                        (req, res) -> {
                            // Only the headers are needed
                        });
            } catch (IOException | ServletException e) {
                throw new IllegalStateException(
                        "Could not read the headers Spring Security writes", e);
            }
            return List.copyOf(
                    recording.getHeaders(CONTENT_SECURITY_POLICY_HEADER));
        }
    }

    /**
     * A request that looks as if no {@link OncePerRequestFilter} has handled it
     * yet, so that the header filter runs again. The markers of the filters are
     * kept apart from the real request.
     */
    private static final class UnfilteredRequest
            extends HttpServletRequestWrapper {

        private final Map<String, Object> markers = new HashMap<>();

        UnfilteredRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public @Nullable Object getAttribute(String name) {
            return isMarker(name) ? markers.get(name)
                    : super.getAttribute(name);
        }

        @Override
        public void setAttribute(String name, Object value) {
            if (isMarker(name)) {
                markers.put(name, value);
            } else {
                super.setAttribute(name, value);
            }
        }

        @Override
        public void removeAttribute(String name) {
            if (isMarker(name)) {
                markers.remove(name);
            } else {
                super.removeAttribute(name);
            }
        }

        private static boolean isMarker(String name) {
            return name.endsWith(OncePerRequestFilter.ALREADY_FILTERED_SUFFIX);
        }
    }

    /**
     * A response that records the headers set on it and passes none of them on.
     * It starts without headers, so that a header writer that leaves out a
     * header the response already has still writes it.
     */
    private static final class RecordingResponse
            extends HttpServletResponseWrapper {

        private final Map<String, List<String>> headers = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER);

        RecordingResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setHeader(String name, String value) {
            List<String> values = new ArrayList<>();
            values.add(value);
            headers.put(name, values);
        }

        @Override
        public void addHeader(String name, String value) {
            headers.computeIfAbsent(name, key -> new ArrayList<>()).add(value);
        }

        @Override
        public void setDateHeader(String name, long date) {
            setHeader(name, Long.toString(date));
        }

        @Override
        public void addDateHeader(String name, long date) {
            addHeader(name, Long.toString(date));
        }

        @Override
        public void setIntHeader(String name, int value) {
            setHeader(name, Integer.toString(value));
        }

        @Override
        public void addIntHeader(String name, int value) {
            addHeader(name, Integer.toString(value));
        }

        @Override
        public boolean containsHeader(String name) {
            return headers.containsKey(name);
        }

        @Override
        public @Nullable String getHeader(String name) {
            List<String> values = headers.get(name);
            return values == null ? null : values.get(0);
        }

        @Override
        public Collection<String> getHeaders(String name) {
            return headers.getOrDefault(name, List.of());
        }

        @Override
        public Collection<String> getHeaderNames() {
            return headers.keySet();
        }

        @Override
        public boolean isCommitted() {
            return false;
        }
    }
}
