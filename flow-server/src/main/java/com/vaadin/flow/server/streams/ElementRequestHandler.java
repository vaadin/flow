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
package com.vaadin.flow.server.streams;

import java.io.Serializable;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;

/**
 * Request handler callback for handing client-server or server-client data
 * transfer scoped to a specific (owner) element.
 */
@FunctionalInterface
public interface ElementRequestHandler extends Serializable {

    /**
     * Request handler callback for handing client-server or server-client data
     * transfer scoped to a specific (owner) element.
     *
     * Note: when handling requests via this API, you need to take care of
     * typical stream handling issues, e.g. exceptions yourself. However, you do
     * not need to close the stream yourself, Flow will handle that for you when
     * needed.
     *
     * @param request
     *            VaadinRequest request to handle
     * @param response
     *            VaadinResponse response to handle
     * @param session
     *            VaadinSession current VaadinSession
     * @param owner
     *            Element owner element
     */
    void handleRequest(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owner);

    /**
     * Optional URL postfix allows appending an application-controlled string,
     * e.g. the logical name of the target file, to the end of the otherwise
     * random-looking download URL. If defined, requests that would otherwise be
     * routable are still rejected if the postfix is missing or invalid. Postfix
     * changes the last segment in the resource url.
     *
     * @return String optional URL postfix, or {@code null} for "".
     */
    default String getUrlPostfix() {
        return null;
    }

    /**
     * Whether to invoke this request handler even if the owning element is
     * currently inert.
     *
     * @return {@code true} to invoke for inert elements, {@code false}
     *         otherwise. Defaults to {@code false}.
     */
    default boolean allowInert() {
        return false;
    }

    /**
     * Controls whether request handler is invoked when the owner element is
     * disabled.
     *
     * @return the currently set DisabledUpdateMode for this request handler.
     *         Defaults to {@literal ONLY_WHEN_ENABLED}.
     */
    default DisabledUpdateMode getDisabledUpdateMode() {
        return DisabledUpdateMode.ONLY_WHEN_ENABLED;
    }
}
