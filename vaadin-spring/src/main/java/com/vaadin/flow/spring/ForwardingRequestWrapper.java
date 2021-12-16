/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

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
