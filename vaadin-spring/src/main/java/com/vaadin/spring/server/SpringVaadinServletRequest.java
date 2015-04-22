/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.server;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.util.UrlPathHelper;

import com.vaadin.server.VaadinServletRequest;
import com.vaadin.server.VaadinServletService;

/**
 * Custom Vaadin servlet request to handle getPathInfo() when the servlet is
 * used via ServletForwardingController which does not set it.
 */
public class SpringVaadinServletRequest extends VaadinServletRequest {

    /**
     * Construct a Spring servlet request
     *
     * @param request
     * @param vaadinService
     */
    public SpringVaadinServletRequest(HttpServletRequest request,
            VaadinServletService vaadinService) {
        super(request, vaadinService);
    }

    @Override
    public String getPathInfo() {
        String pathInfo = super.getPathInfo();
        if (pathInfo == null) {
            pathInfo = new UrlPathHelper().getPathWithinApplication(this);
        }
        return pathInfo;
    }

}
