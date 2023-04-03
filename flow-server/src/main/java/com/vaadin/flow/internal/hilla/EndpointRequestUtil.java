/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow.internal.hilla;

import jakarta.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * A container for utility methods related with Hilla endpoints.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 23.2
 */
public interface EndpointRequestUtil extends Serializable {
    /**
     * Checks if the request is for an endpoint.
     * <p>
     * Note even if this method returns <code>true</code>, there is no guarantee
     * that an endpoint method will actually be called, e.g. access might be
     * denied.
     *
     * @param request
     *            the HTTP request
     * @return <code>true</code> if the request is for an endpoint,
     *         <code>false</code> otherwise
     */
    boolean isEndpointRequest(HttpServletRequest request);

    /**
     * Checks if the given request goes to an anonymous (public) endpoint.
     *
     * @param request
     *            the HTTP request to check
     * @return <code>true</code> if the request goes to an anonymous endpoint,
     *         <code>false</code> otherwise
     */
    boolean isAnonymousEndpoint(HttpServletRequest request);

    /**
     * Checks if Hilla is available.
     *
     * @return true if Hilla is available, false otherwise
     */
    static boolean isHillaAvailable() {
        try {
            Class.forName("dev.hilla.EndpointController");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
