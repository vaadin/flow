/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.faulttolerance;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import com.vaadin.flow.server.VaadinServletResponse;

@WebFilter(urlPatterns = "/*")
public class BeforeOutputStreamActionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        response = new BeforeOutputStreamActionResponse(
                (HttpServletResponse) response);
        chain.doFilter(request, response);
    }

    static void beforeGettingOutputStream(Runnable action) {
        ServletResponse response = VaadinServletResponse.getCurrent()
                .getResponse();
        if (response instanceof BeforeOutputStreamActionResponse cast) {
            cast.beforeGettingOutputStream(action);
        }
    }

    public static class BeforeOutputStreamActionResponse
            extends HttpServletResponseWrapper {
        private Runnable action;

        BeforeOutputStreamActionResponse(HttpServletResponse response) {
            super(response);
        }

        private void beforeGettingOutputStream(Runnable action) {
            this.action = action;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (action != null) {
                action.run();
                action = null;
            }
            return super.getOutputStream();
        }
    }
}
