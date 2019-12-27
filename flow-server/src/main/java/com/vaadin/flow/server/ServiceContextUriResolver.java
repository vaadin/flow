/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.shared.VaadinUriResolver;

/**
 * A URI resolver which resolves paths for loading through VaadinService
 * resource methods.
 *
 * @since 1.0
 */
public class ServiceContextUriResolver extends VaadinUriResolver
        implements Serializable {

    /**
     * Resolves the given uri to a path which can be used with
     * {@link VaadinService#getResource(String)} and
     * {@link VaadinService#getResourceAsStream(String)}.
     *
     * @param uri
     *            the URI to resolve
     * @return the URI resolved to be relative to the context root
     */
    public String resolveVaadinUri(String uri) {
        return super.resolveVaadinUri(uri, "/");
    }

}
