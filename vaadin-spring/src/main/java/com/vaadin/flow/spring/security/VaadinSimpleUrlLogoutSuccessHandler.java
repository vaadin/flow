/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.Serializable;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

/**
 * Default logout success handler for {@link VaadinWebSecurity}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
class VaadinSimpleUrlLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler
        implements Serializable {

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        handle(request, response, authentication);
    }

    @Override
    protected void handle(HttpServletRequest request,
            HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if (response == null) {
            // tolerate null response without failing
            String targetUrl = determineTargetUrl(request, response,
                    authentication);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            super.handle(request, response, authentication);
        }
    }

}
