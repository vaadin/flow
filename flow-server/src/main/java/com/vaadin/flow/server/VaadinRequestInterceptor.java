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

package com.vaadin.flow.server;

import java.io.Serializable;

/**
 * Used to provide an around-like aspect option around request processing.
 *
 * @author Marcin Grzejszczak
 * @since 24.2
 */
public interface VaadinRequestInterceptor extends Serializable {

    /**
     * Called when request is about to be processed.
     *
     * @param request
     *            request
     * @param response
     *            response
     */
    void requestStart(VaadinRequest request, VaadinResponse response);

    /**
     * Called when an exception occurred
     *
     * @param request
     *            request
     * @param response
     *            response
     * @param vaadinSession
     *            session
     * @param t
     *            exception
     */
    void handleException(VaadinRequest request, VaadinResponse response,
            VaadinSession vaadinSession, Exception t);

    /**
     * Called in the finally block of processing a request. Will be called
     * regardless of whether there was an exception or not.
     *
     * @param request
     *            request
     * @param response
     *            response
     * @param session
     *            session
     */
    void requestEnd(VaadinRequest request, VaadinResponse response,
            VaadinSession session);
}
