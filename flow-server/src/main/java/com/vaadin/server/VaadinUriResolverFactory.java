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
package com.vaadin.server;

import java.io.Serializable;

import com.vaadin.shared.VaadinUriResolver;

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
}
