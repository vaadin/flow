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
package com.vaadin.flow.internal;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletRegistration;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Helper methods for use in bootstrapping.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class BootstrapHandlerHelper implements Serializable {

    private BootstrapHandlerHelper() {
        // Only utility methods
    }

    /**
     * Gets the service URL as a URL relative to the request URI.
     *
     * @param vaadinRequest
     *            the request
     * @return the relative service URL
     */
    public static String getServiceUrl(VaadinRequest vaadinRequest) {
        String pathInfo = vaadinRequest.getPathInfo();
        if (pathInfo == null) {
            return ".";
        } else {
            /*
             * Make a relative URL to the servlet by adding one ../ for each
             * path segment in pathInfo (i.e. the part of the requested path
             * that comes after the servlet mapping)
             */
            return HandlerHelper.getCancelingRelativePath(pathInfo);
        }
    }

    /**
     * Gets the push URL as a URL relative to the request URI.
     *
     * @param vaadinSession
     *            the session
     * @param vaadinRequest
     *            the request
     * @return the relative push URL
     */
    public static String getPushURL(VaadinSession vaadinSession,
            VaadinRequest vaadinRequest) {
        String pushServletMapping = determinePushServletMapping(vaadinSession);

        String pushURL = pushServletMapping + Constants.PUSH_MAPPING;

        String contextPath = vaadinRequest.getService()
                .getContextRootRelativePath(vaadinRequest);

        if (contextPath.endsWith("/") && pushURL.startsWith("/")) {
            pushURL = pushURL.substring(1);
        }
        return contextPath + pushURL;
    }

    public static String determinePushServletMapping(
            VaadinSession vaadinSession) {
        String pushServletMapping = getCleanedPushServletMapping(
                vaadinSession.getConfiguration().getPushServletMapping());
        // If no explicit pushServletMapping is defined, check for potential
        // multiple servlet mappings
        if (pushServletMapping == null
                && vaadinSession.getService() instanceof VaadinServletService) {
            Optional<ServletRegistration> servletRegistration = getServletRegistration(
                    ((VaadinServletService) vaadinSession.getService())
                            .getServlet());
            if (servletRegistration.isPresent()) {
                pushServletMapping = findFirstUrlMapping(
                        servletRegistration.get());
            }
        }
        return pushServletMapping;
    }

    /**
     * Cleans up the given push servlet mapping value for proper use.
     * Effectively makes sure it starts and ends with a '/', and removes
     * possible '/*' at the end.
     *
     * @param pushServletMapping
     *            Original pushServletMapping value
     * @return cleaned-up value, or null if the original value is blank.
     */
    public static String getCleanedPushServletMapping(
            String pushServletMapping) {
        if (pushServletMapping == null || pushServletMapping.trim().isEmpty()) {
            return null;
        } else {
            if (!pushServletMapping.startsWith("/")) {
                pushServletMapping = "/" + pushServletMapping;
            }
            if (pushServletMapping.endsWith("/*")) {
                pushServletMapping = pushServletMapping.replace("/*", "/");
            }
            if (!pushServletMapping.endsWith("/")) {
                pushServletMapping += "/";
            }
            return pushServletMapping;
        }
    }

    /**
     * Returns a {@link ServletRegistration} for the given
     * {@link ServletConfig}, if available.
     *
     * @param servletConfig
     *            {@link ServletConfig} to find the registration for.
     * @return an optional {@link ServletRegistration}, or an empty optional if
     *         a registration is not available.
     */
    public static Optional<ServletRegistration> getServletRegistration(
            ServletConfig servletConfig) {
        String name = servletConfig.getServletName();
        if (name != null) {
            Map<String, ? extends ServletRegistration> regs = servletConfig
                    .getServletContext().getServletRegistrations();
            if (regs != null) {
                return Optional.ofNullable(servletConfig.getServletContext()
                        .getServletRegistrations().get(name));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the first of sorted URL mappings of the given
     * {@link ServletRegistration}, ignoring '/VAADIN/*' and '/vaadinServlet/*'
     * mapping.
     *
     * @param registration
     *            {@link ServletRegistration} from which to look up the
     *            mappings.
     * @return first URL mapping, ignoring '/VAADIN/*' and '/vaadinServlet/*'
     */
    public static String findFirstUrlMapping(ServletRegistration registration) {
        String firstMapping = registration.getMappings().stream()
                .filter(mapping -> !mapping.equals("/VAADIN/*")
                        && !mapping.equals("/vaadinServlet/*"))
                .sorted().findFirst().orElse("/");
        return firstMapping.replace("/*", "/");
    }
}
