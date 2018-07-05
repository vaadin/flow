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
import java.util.EventListener;

/**
 * This event listener is notified when the bootstrap HTML is about to be
 * generated and sent to the client. The bootstrap HTML is first constructed as
 * an in-memory DOM representation which registered listeners can modify before
 * the final HTML is generated.
 * <p>
 * BootstrapListeners are registered using the {@link ServiceInitEvent} during
 * the initialization of the application.
 * 
 * @see ServiceInitEvent#addBootstrapListener(BootstrapListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface BootstrapListener extends EventListener, Serializable {

    /**
     * Lets this listener make changes to the overall HTML document that will be
     * used as the initial HTML page, as well as the HTTP headers in the
     * response serving the initial HTML.
     *
     * @param response
     *            the bootstrap response that can be modified to cause change in
     *            the generate HTML and in the HTTP headers of the response.
     */
    void modifyBootstrapPage(BootstrapPageResponse response);

}
