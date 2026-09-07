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
package com.vaadin.flow.navigate;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Denies framing of the offline stub, the way Spring Security and other
 * security filters do for every response by default.
 * <p>
 * The Flow client renders the offline stub within an iframe, so the service
 * worker is expected to serve it without this header. See
 * {@code ServiceWorkerIT.offlineStub_framingDeniedByServer_offlineStubShown}.
 */
@WebFilter(urlPatterns = "/offline-stub.html")
public class DenyFramingFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        response.setHeader("X-Frame-Options", "DENY");
        chain.doFilter(request, response);
    }
}
