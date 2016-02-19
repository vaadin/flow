/*
 * Copyright 2000-2016 Vaadin Ltd.
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

import com.vaadin.ui.UI;

/**
 * Base class providing common functionality used in different bootstrap
 * responses.
 *
 * @author Vaadin Ltd
 * @since 7.0.0
 */
public abstract class BootstrapResponse {
    private final VaadinRequest request;
    private final VaadinSession session;
    private final UI ui;

    /**
     * Creates a new bootstrap response.
     *
     * @param request
     *            the Vaadin request for which the bootstrap page should be
     *            generated
     * @param session
     *            the session for which the bootstrap page should be generated
     * @param ui
     *            the UI that will be displayed on the page
     */
    public BootstrapResponse(VaadinRequest request, VaadinSession session,
            UI ui) {
        this.request = request;
        this.session = session;
        this.ui = ui;
    }

    /**
     * Gets the request for which the generated bootstrap HTML will be the
     * response. This can be used to read request headers and other additional
     * information.
     *
     * @return the Vaadin request that is being handled
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Gets the service session to which the rendered view belongs.
     *
     * @return the Vaadin service session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Gets the UI that will be displayed on the generated bootstrap page.
     *
     * @return the UI
     */
    public UI getUI() {
        return ui;
    }

}
