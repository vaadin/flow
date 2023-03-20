/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;

import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * Helper methods for use in bootstrapping.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
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
        String serviceUrl = getServiceUrl(vaadinRequest);

        String pushURL = vaadinSession.getConfiguration().getPushURL();
        if (pushURL == null) {
            pushURL = serviceUrl;
        }
        String contextPath = vaadinRequest.getService()
                .getContextRootRelativePath(vaadinRequest);

        class UriResolver extends VaadinUriResolver {
            public String resolveVaadinUri(String uri) {
                return resolveVaadinUri(uri, contextPath);
            }
        }
        return new UriResolver().resolveVaadinUri(pushURL);
    }
}
