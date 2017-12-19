/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.server;

import java.io.Serializable;

import javax.servlet.ServletContext;

import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * Factory for {@link VaadinUriResolver}.
 * <p>
 * Produces a {@link VaadinUriResolver} for the provided {@link VaadinRequest}
 * instance.
 * <p>
 * The instance of the factory may be retrieved from {@link VaadinSession}:
 *
 * <pre>
 * <code>
 * VaadinUriResolverFactory factory = VaadinSession.getCurrent.getAttribute(VaadinUriResolverFactory.class);
 * </code>
 * </pre>
 *
 * @see VaadinSession#getAttribute(Class)
 *
 * @author Vaadin Ltd
 *
 */
@FunctionalInterface
public interface VaadinUriResolverFactory extends Serializable {

    /**
     * Gets a resolver by the given {@code request}.
     *
     * @param request
     *            the VaadinRequest instance to produce a resolver for
     * @return the URI resolver instance
     */
    VaadinUriResolver getUriResolver(VaadinRequest request);

    /**
     * Resolves the {@code path} to the path which can be used by a
     * {@link ServletContext} to get a resource content.
     * <p>
     * The factory method {@link #getUriResolver(VaadinRequest)} is used to get
     * URI resolver which resolves the {@code path}. This path works on the
     * client side considering <code>&lt;base href&gt;</code> attribute value
     * (the class {@link VaadinUriResolver} is a shared class between client and
     * server, so it cannot contain any server-side specific logic). But it
     * requires additional logic on the server side to be able to use it with
     * the {@link ServletContext#getResource(String)} or
     * {@link ServletContext#getResourceAsStream(String)} methods. This logic is
     * implemented in this method so it can be reused on the server-side in
     * various places.
     *
     * @see #getUriResolver(VaadinRequest)
     *
     * @param request
     *            a request as a context to get a VaadinUriResolver
     * @param path
     *            a resource path to resolve
     * @return resolved path which can be used by ServletContext to get a
     *         content
     */
    default String toServletContextPath(VaadinRequest request, String path) {
        VaadinUriResolver uriResolver = getUriResolver(request);
        assert uriResolver != null;

        String uri = getUriResolver(request).resolveVaadinUri(path);
        assert uri != null;
        if (request instanceof VaadinServletRequest) {
            VaadinServletRequest servletRequest = (VaadinServletRequest) request;

            String servletPath = servletRequest.getServletPath();
            assert servletPath != null;
            if (!servletPath.endsWith("/") && !uri.startsWith("/")) {
                servletPath += "/";
            } else if (servletPath.endsWith("/") && uri.startsWith("/")) {
                servletPath = servletPath.substring(0,
                        servletPath.length() - 1);
            }
            // "Revert" the `../` from uri resolver so that we point to the
            // context root.
            //
            // We won't take pathinfo into account because we are on the server
            // and the pathinfo doesn't matter for the resolved path
            if (uri.contains("../")) {
                return servletPath + uri;
            }
        }
        return uri.startsWith("/") ? uri : "/" + uri;
    }
}
