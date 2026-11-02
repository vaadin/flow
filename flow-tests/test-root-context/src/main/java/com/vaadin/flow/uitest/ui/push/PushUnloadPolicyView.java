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
package com.vaadin.flow.uitest.ui.push;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Long polling push view served with the unload permissions policy blocked, as
 * Chromium-based browsers are rolling out by default.
 */
@CustomPush(transport = Transport.LONG_POLLING)
@Route(value = "com.vaadin.flow.uitest.ui.push.PushUnloadPolicyView", layout = ViewTestLayout.class)
public class PushUnloadPolicyView extends BasicPushLongPollingView {

    /**
     * Blocks unload listeners on the page of this view only, so that the rest
     * of the test views keep the browser default. The policy is set explicitly
     * because a browser driven by ChromeDriver does not block unload by
     * default.
     */
    @WebFilter("/view/com.vaadin.flow.uitest.ui.push.PushUnloadPolicyView")
    public static class UnloadPolicyFilter extends HttpFilter {
        @Override
        protected void doFilter(HttpServletRequest request,
                HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            response.setHeader("Permissions-Policy", "unload=()");
            chain.doFilter(request, response);
        }
    }
}
