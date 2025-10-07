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
package com.vaadin.flow.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.HandlerHelper;

/**
 * A strategy to handle redirects which is aware of UIDL requests.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRedirectStrategy extends DefaultRedirectStrategy {

    @Override
    public void sendRedirect(HttpServletRequest request,
            HttpServletResponse response, String url) throws IOException {
        final var servletMapping = request.getHttpServletMapping().getPattern();
        if (HandlerHelper.isFrameworkInternalRequest(servletMapping, request)) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.getPage().setLocation(url);
            } else {
                LoggerFactory.getLogger(UidlRedirectStrategy.class).warn(
                        "A redirect to {} was request during a Vaadin request, "
                                + "but it was not possible to get the UI instance to perform the action.",
                        url);
            }
        } else if (response == null) {
            LoggerFactory.getLogger(UidlRedirectStrategy.class)
                    .warn("A redirect to {} was request, "
                            + "but it has null HttpServletResponse and can't perform the action. "
                            + "Performing logout during a Vaadin request with @Push(transport = Transport.WEBSOCKET) would cause this.",
                            url);
        } else {
            super.sendRedirect(request, response, url);
        }
    }
}
