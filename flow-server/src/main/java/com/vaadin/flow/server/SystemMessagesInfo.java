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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.util.Locale;

/**
 * Provides information available for {@link SystemMessagesProvider} when
 * defining what {@link SystemMessages} to use.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class SystemMessagesInfo implements Serializable {

    private Locale locale;
    private transient VaadinRequest request;
    private VaadinService service;

    /**
     * Creates an instance based on the given locale, request and service.
     *
     * @param locale
     *            the locale the desired locale for the system messages
     * @param request
     *            the request we are processing
     * @param service
     *            the service instance
     */
    public SystemMessagesInfo(Locale locale, VaadinRequest request,
            VaadinService service) {
        this.locale = locale;
        this.request = request;
        this.service = service;
    }

    /**
     * The locale of the UI related to the {@link SystemMessages} request.
     *
     * @return The Locale or null if the locale is not known
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Gets the request currently in progress.
     *
     * @return The request currently in progress or null if no request is in
     *         progress.
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Returns the service this SystemMessages request comes from.
     *
     * @return The service which triggered this request or null of not triggered
     *         from a service.
     */
    public VaadinService getService() {
        return service;
    }

}
