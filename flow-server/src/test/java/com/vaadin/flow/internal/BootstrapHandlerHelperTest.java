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
package com.vaadin.flow.internal;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BootstrapHandlerHelperTest {

    @Test
    void getServiceUrl_nullPathInfo_returnsDot() {
        VaadinServletRequest request = createRequest(null, "", "/", "");
        assertEquals(".", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @Test
    void getServiceUrl_simplePath_returnsCorrectRelativePath() {
        VaadinServletRequest request = createRequest("/some/path", "",
                "/some/path", "");
        assertEquals("./..", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @Test
    void getServiceUrl_encodedSlashInPath_treatsAsOneSegment() {
        // Simulates a wildcard route with %2F: /wild/a%2Fb
        // getPathInfo() returns decoded /wild/a/b (2 slashes after root)
        // but the raw URI has /wild/a%2Fb (1 slash after root)
        // The baseHref should be ./.., not ./../..
        VaadinServletRequest request = createRequest("/wild/a/b", "",
                "/wild/a%2Fb", "");
        assertEquals("./..", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @Test
    void getServiceUrl_encodedSlashWithContextPath_treatsAsOneSegment() {
        VaadinServletRequest request = createRequest("/wild/a/b", "",
                "/ctx/wild/a%2Fb", "/ctx");
        assertEquals("./..", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @Test
    void getServiceUrl_multipleEncodedSlashes_countsCorrectly() {
        // /wild/a%2Fb%2Fc has 1 real slash after root in the raw form
        VaadinServletRequest request = createRequest("/wild/a/b/c", "",
                "/wild/a%2Fb%2Fc", "");
        assertEquals("./..", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @Test
    void getServiceUrl_pathWithSpaces_unaffected() {
        // Spaces (%20) don't affect slash counting
        VaadinServletRequest request = createRequest("/file with spaces.js", "",
                "/file%20with%20spaces.js", "");
        assertEquals(".", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @Test
    void getServiceUrl_normalPathWithServletPath_returnsCorrectPath() {
        VaadinServletRequest request = createRequest("/view/sub", "/app",
                "/app/view/sub", "");
        assertEquals("./..", BootstrapHandlerHelper.getServiceUrl(request));
    }

    @ParameterizedTest
    @CsvSource(nullValues = "none", value = {
            "/push/*, /app/*, /push/VAADIN/push",
            "none, /app/*, /app/VAADIN/push", "none, none, none" })
    void getPushEndpointPath_pushServletMappingOrServletMapping(
            String pushServletMapping, String servletMapping, String expected) {
        ServletContext context = mock(ServletContext.class);
        if (servletMapping != null) {
            ServletRegistration registration = mock(ServletRegistration.class);
            when(registration.getMappings())
                    .thenReturn(List.of(servletMapping));
            when(context.getServletRegistrations())
                    .thenAnswer(invocation -> Map.of("app", registration));
        }
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletName()).thenReturn("app");
        when(config.getServletContext()).thenReturn(context);
        when(config.getInitParameter(
                InitParameters.SERVLET_PARAMETER_PUSH_SERVLET_MAPPING))
                .thenReturn(pushServletMapping);

        assertEquals(expected,
                BootstrapHandlerHelper.getPushEndpointPath(config));
    }

    private VaadinServletRequest createRequest(String pathInfo,
            String servletPath, String requestURI, String contextPath) {
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(httpRequest.getPathInfo()).thenReturn(pathInfo);
        when(httpRequest.getServletPath()).thenReturn(servletPath);
        when(httpRequest.getRequestURI()).thenReturn(requestURI);
        when(httpRequest.getContextPath()).thenReturn(contextPath);
        VaadinServletService service = mock(VaadinServletService.class);
        return new VaadinServletRequest(httpRequest, service);
    }
}
