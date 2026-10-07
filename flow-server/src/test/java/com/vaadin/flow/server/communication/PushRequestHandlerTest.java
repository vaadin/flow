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
package com.vaadin.flow.server.communication;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import java.util.Collections;

import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.interceptor.CorsInterceptor;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushRequestHandlerTest {

    private static final String DROP_CORS_HEADER_PARAMETER = "org.atmosphere.cpr.dropAccessControlAllowOriginHeader";

    @Test
    void initAtmosphere_requestWithForeignOrigin_noCorsHeaders() {
        AtmosphereResponse response = inspectRequestWithOrigin(
                mockServletConfig(null));

        verify(response, never()).addHeader(anyString(), anyString());
        verify(response, never()).setHeader(anyString(), anyString());
    }

    @Test
    void initAtmosphere_corsEnabledByServletInitParameter_originReflected() {
        AtmosphereResponse response = inspectRequestWithOrigin(
                mockServletConfig("false"));

        verify(response).addHeader("Access-Control-Allow-Origin",
                "https://foreign.example");
        verify(response).setHeader("Access-Control-Allow-Credentials", "true");
    }

    private static AtmosphereResponse inspectRequestWithOrigin(
            ServletConfig servletConfig) {
        AtmosphereFramework atmosphere = PushRequestHandler
                .initAtmosphere(servletConfig);
        try {
            CorsInterceptor corsInterceptor = atmosphere.interceptors().stream()
                    .filter(CorsInterceptor.class::isInstance)
                    .map(CorsInterceptor.class::cast).findFirst().orElseThrow();

            AtmosphereRequest request = mock(AtmosphereRequest.class);
            when(request.getHeader("Origin"))
                    .thenReturn("https://foreign.example");
            when(request.getMethod()).thenReturn("GET");
            AtmosphereResponse response = mock(AtmosphereResponse.class);
            AtmosphereResourceImpl resource = mock(
                    AtmosphereResourceImpl.class);
            when(resource.getRequest(false)).thenReturn(request);
            when(resource.getRequest()).thenReturn(request);
            when(resource.getResponse()).thenReturn(response);

            corsInterceptor.inspect(resource);
            return response;
        } finally {
            atmosphere.destroy();
        }
    }

    private static ServletConfig mockServletConfig(String dropCorsHeader) {
        ServletContext context = mock(ServletContext.class);
        when(context.getInitParameterNames())
                .thenReturn(Collections.emptyEnumeration());
        when(context.getAttributeNames())
                .thenReturn(Collections.emptyEnumeration());
        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(context);
        when(config.getServletName()).thenReturn("vaadin");
        when(config.getInitParameterNames()).thenReturn(dropCorsHeader == null
                ? Collections.emptyEnumeration()
                : Collections.enumeration(
                        Collections.singleton(DROP_CORS_HEADER_PARAMETER)));
        when(config.getInitParameter(DROP_CORS_HEADER_PARAMETER))
                .thenReturn(dropCorsHeader);
        return config;
    }
}
