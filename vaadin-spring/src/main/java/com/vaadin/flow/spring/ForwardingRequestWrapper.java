/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.web.util.UrlPathHelper;

import com.vaadin.flow.server.VaadinServlet;

/**
 * Creates an {@link HttpServletRequestWrapper} instance which returns
 * appropriate {@code servletPath} and {@code pathInfo} (which are expected by
 * {@link VaadinServlet}) in case forwarding is enforced.
 *
 * @see <a href="https://jira.spring.io/browse/SPR-17457">Spring issue</a>
 *
 * @author Vaadin Ltd
 *
 */
public class ForwardingRequestWrapper extends HttpServletRequestWrapper {

    private UrlPathHelper urlPathHelper = new UrlPathHelper();

    public ForwardingRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    @Override
    public String getServletPath() {
        String pathInfo = super.getPathInfo();
        if (pathInfo == null) {
            // the path where a ServletForwardingController is registered is not
            // a real servlet path
            return "";
        } else {
            return super.getServletPath();
        }
    }

    @Override
    public String getPathInfo() {
        String pathInfo = super.getPathInfo();
        if (pathInfo == null) {
            // this uses getServletPath() and should work both with and without
            // clearServletPath
            pathInfo = urlPathHelper.getPathWithinServletMapping(this);
        }
        return pathInfo;
    }
}
