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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.accept.HeaderContentNegotiationStrategy;

import com.vaadin.flow.server.HandlerHelper;

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
 * <p>
 * Custom request matchers can be provided using the {@code ignoreRequests}
 * method to fine-tune which URLs should be excluded from being cached, in
 * addition to the default exclusions. This is useful for ignoring
 * application-specific paths that should never be used as redirect targets
 * after authentication.
 */
@Component
public class VaadinDefaultRequestCache implements RequestCache {

    @Autowired
    private RequestUtil requestUtil;

    @Value("${server.error.path:/error}")
    private String configuredErrorPath;

    private RequestCache delegateRequestCache = new HttpSessionRequestCache();

    private final RequestMatcher defaultIgnoreRules = createDefaultIgnoreRules();

    private RequestMatcher ignoreRequestMatcher = null;

    @Override
    public void saveRequest(HttpServletRequest request,
            HttpServletResponse response) {
        if (requestUtil.isFrameworkInternalRequest(request)) {
            getLogger().debug(
                    "Did not save request since it is a Vaadin internal framework request");
            return;
        }
        if (requestUtil.isEndpointRequest(request)) {
            getLogger().debug(
                    "Did not save request since it is a Hilla endpoint request");
            return;
        }
        if (isServiceWorkerInitiated(request)) {
            getLogger().debug(
                    "Did not save request since it is a service worker initiated request");
            return;
        }
        if (isErrorRequest(request)) {
            getLogger().debug("Did not save request since it is an error page");
            return;
        }
        if (HandlerHelper.isNonHtmlInitiatedRequest(request)) {
            getLogger().debug(
                    "Did not save request since its initiator is not a web page");
            return;
        }
        if (defaultIgnoreRules.matches(request)) {
            getLogger().debug(
                    "Did not save request since it matched default ignore rules {}",
                    defaultIgnoreRules);
            return;
        }
        if (ignoreRequestMatcher != null
                && ignoreRequestMatcher.matches(request)) {
            getLogger().debug(
                    "Did not save request since it matched custom ignore rules {}",
                    ignoreRequestMatcher);
            return;
        }

        getLogger().debug("Saving request to {}", request.getRequestURI());

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

    /**
     * Allows further restricting requests to be saved.
     * <p>
     * If set, matching requests will not be cached.
     *
     * @param requestMatcher
     *            a request matching strategy which defines which requests
     *            should not be cached.
     */
    public void ignoreRequests(RequestMatcher requestMatcher) {
        this.ignoreRequestMatcher = requestMatcher;
    }

    /*
     * Rules adapted from Spring Security's RequestCacheConfigurer
     */
    private static RequestMatcher createDefaultIgnoreRules() {
        var matcherBuilder = PathPatternRequestMatcher.withDefaults();
        RequestMatcher favIcon = matcherBuilder.matcher("/favicon.*");
        RequestMatcher wellKnown = matcherBuilder.matcher("/.well-known/**");
        RequestMatcher xhrRequestedWith = new RequestHeaderRequestMatcher(
                "X-Requested-With", "XMLHttpRequest");
        List<RequestMatcher> matchers = new ArrayList<>();
        matchers.add(favIcon);
        matchers.add(wellKnown);
        HeaderContentNegotiationStrategy contentNegotiationStrategy = new HeaderContentNegotiationStrategy();
        matchers.add(matchingMediaType(contentNegotiationStrategy,
                MediaType.APPLICATION_JSON));
        matchers.add(xhrRequestedWith);
        matchers.add(matchingMediaType(contentNegotiationStrategy,
                MediaType.MULTIPART_FORM_DATA));
        matchers.add(matchingMediaType(contentNegotiationStrategy,
                MediaType.TEXT_EVENT_STREAM));
        return new OrRequestMatcher(matchers);
    }

    private static RequestMatcher matchingMediaType(
            ContentNegotiationStrategy contentNegotiationStrategy,
            MediaType mediaType) {
        MediaTypeRequestMatcher mediaRequest = new MediaTypeRequestMatcher(
                contentNegotiationStrategy, mediaType);
        mediaRequest.setIgnoredMediaTypes(Collections.singleton(MediaType.ALL));
        return mediaRequest;
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
