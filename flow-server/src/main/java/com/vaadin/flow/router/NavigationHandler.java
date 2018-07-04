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
package com.vaadin.flow.router;

import java.io.Serializable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;

/**
 * Handles navigation to a location e.g. by showing a navigation target
 * component in a {@link UI} or by redirecting the user to another location.
 * <p>
 * Subclasses using external data should take care to avoid synchronization
 * issues since the same navigation handler instances may be used concurrently
 * from multiple threads. Data provided in the navigation event should be safe
 * to use without synchronization since the associated {@link VaadinSession} and
 * everything related to it will be locked.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface NavigationHandler extends Serializable {
    /**
     * Handles the navigation event.
     *
     * @param event
     *            the navigation event to handle
     * @return the HTTP status code to return to the client if handling an
     *         initial rendering request
     */
    int handle(NavigationEvent event);

}
