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
 * The callback used by bootstrap handlers in order to know when a request needs
 * to pre-render the UI and include the initial UIDL in the page.
 *
 * @since 3.0
 */
@FunctionalInterface
public interface BootstrapInitialPredicate extends EventListener, Serializable {

    /**
     * Return whether the bootstrap handler has to include initial UIDL in the
     * response.
     *
     * @param request
     *            Vaadin request.
     * @return true if initial should be included
     */
    boolean includeInitialUidl(VaadinRequest request);

}
