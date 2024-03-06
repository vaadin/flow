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

import com.vaadin.flow.server.HandlerHelper;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

/**
 * A default request cache implementation which aims to ignore requests that are
 * not for routes.
 * <p>
 * For the requests that are not ignored, delegates the actual saving to the
 * other {@link RequestCache} instance. Uses an internal
 * {@link HttpSessionRequestCache} for delegating to, unless a custom delegate
 * is set using the
 * {@link VaadinDefaultRequestCache#setDelegateRequestCache(RequestCache)}
 * method.
 * <p>
 * Using this class helps with redirecting the user to the correct route after
 * login instead of redirecting to some internal URL like a service worker or
 * some data the service worker has fetched.
 */
@Component
public class VaadinDefaultRequestCache implements RequestCache {

    @Autowired
    private RequestUtil requestUtil;

    @Value("${server.error.path:/error}")
    private String configuredErrorPath;

    private RequestCache delegateRequestCache = new HttpSessionRequestCache();

    @Override
    public void saveRequest(HttpServletRequest request,
            HttpServletResponse response) {
        if (requestUtil.isFrameworkInternalRequest(request)) {
            return;
        }
        if (requestUtil.isEndpointRequest(request)) {
            return;
        }
        if (isServiceWorkerInitiated(request)) {
            return;
        }
        if (isErrorRequest(request)) {
            return;
        }

        LoggerFactory.getLogger(getClass())
                .debug("Saving request to " + request.getRequestURI());

        delegateRequestCache.saveRequest(request, response);
    }

    private boolean isErrorRequest(HttpServletRequest request) {
        String pathInContext = HandlerHelper
                .getRequestPathInsideContext(request);
        String errorPath = configuredErrorPath;
        if (errorPath.startsWith("/")) {
            errorPath = errorPath.substring(1);
        }
        return errorPath.equals(pathInContext);
    }

    @Override
    public SavedRequest getRequest(HttpServletRequest request,
            HttpServletResponse response) {
        return delegateRequestCache.getRequest(request, response);
    }

    @Override
    public HttpServletRequest getMatchingRequest(HttpServletRequest request,
            HttpServletResponse response) {
        return delegateRequestCache.getMatchingRequest(request, response);
    }

    @Override
    public void removeRequest(HttpServletRequest request,
            HttpServletResponse response) {
        delegateRequestCache.removeRequest(request, response);
    }

    /**
     * Checks if the request is initiated by a service worker.
     *
     * NOTE This method can never be used for security purposes as the "Referer"
     * header is easy to fake.
     */
    private boolean isServiceWorkerInitiated(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return referer != null && referer.endsWith("sw.js");
    }

    /**
     * Sets the cache implementation that is used for the actual saving of the
     * requests that are not ignored.
     *
     * @param delegateRequestCache
     *            the delegate request cache
     */
    public void setDelegateRequestCache(RequestCache delegateRequestCache) {
        this.delegateRequestCache = delegateRequestCache;
    }
}
