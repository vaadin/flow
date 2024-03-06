/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.slf4j.LoggerFactory;
import org.springframework.security.web.DefaultRedirectStrategy;

import com.vaadin.flow.component.UI;

/**
 * A strategy to handle redirects which is aware of UIDL requests.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class UidlRedirectStrategy extends DefaultRedirectStrategy {

    private final RequestUtil requestUtil;

    public UidlRedirectStrategy(RequestUtil requestUtil) {
        this.requestUtil = requestUtil;
    }

    @Override
    public void sendRedirect(HttpServletRequest request,
            HttpServletResponse response, String url) throws IOException {
        if (requestUtil.isFrameworkInternalRequest(request)) {
            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.getPage().setLocation(url);
            } else {
                LoggerFactory.getLogger(UidlRedirectStrategy.class).warn(
                        "A redirect to {} was request during a Vaadin request, "
                                + "but it was not possible to get the UI instance to perform the action.",
                        url);
            }
        } else {
            super.sendRedirect(request, response, url);
        }
    }
}
