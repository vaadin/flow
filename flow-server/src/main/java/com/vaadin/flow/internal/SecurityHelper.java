/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.stream.Stream;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.PwaConfiguration;
import com.vaadin.flow.server.communication.PwaHandler;
import com.vaadin.flow.server.frontend.FrontendUtils;
import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Helper methods for setting up security contexts in Vaadin applications.
 *
 * @author Vaadin Ltd
 *
 */
public final class SecurityHelper implements Serializable {

    /**
     * URLs matching these patterns should be publicly available for application
     * to work. Used for defining bypass rules in e.g. Spring Security.
     */
    public static final String[] PUBLIC_VAADIN_URLS = {
            "/favicon.ico",
            "/images/**",
            "/icons/**",
            "/" + PwaConfiguration.DEFAULT_PATH,
            "/" + FrontendUtils.SERVICE_WORKER_SRC_JS,
            PwaHandler.SW_RUNTIME_PRECACHE_PATH,
            "/" + PwaConfiguration.DEFAULT_OFFLINE_PATH
    };

    /**
     * Determines whether the request is internal to the Vaadin framework.
     *
     * @param request
     *            a servlet request
     * @return true iff the request is internal
     */
    public static boolean isInternalRequest(HttpServletRequest request) {
        String requestType = request
                .getParameter(ApplicationConstants.REQUEST_TYPE_PARAMETER);
        String referer = request.getHeader("referer");
        boolean serviceWorkedInitiated =
                referer != null && referer.endsWith("/sw.js");
        return serviceWorkedInitiated || requestType != null && Stream
                .of(HandlerHelper.RequestType.values())
                .anyMatch(r -> r.getIdentifier().equals(requestType));
    }

    private SecurityHelper() {
    }
}
