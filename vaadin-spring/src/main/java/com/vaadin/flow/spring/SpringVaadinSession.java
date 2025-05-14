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
package com.vaadin.flow.spring;

import com.vaadin.flow.server.SessionDestroyListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

/**
 * Vaadin session implementation for Spring.
 *
 * @author Vaadin Ltd
 * @deprecated No replacement planned
 */
@Deprecated(forRemoval = true)
public class SpringVaadinSession extends VaadinSession {

    /**
     * Creates a new SpringVaadinSession tied to a VaadinService.
     *
     * @param service
     *            the Vaadin service for the new session
     */
    public SpringVaadinSession(VaadinService service) {
        super(service);
    }

    /**
     * Handles destruction of the session.
     */
    public void fireSessionDestroy() {
        getService().fireSessionDestroy(this);
    }

    /**
     * Adds a listener that gets notified when the Vaadin service session is
     * destroyed.
     * <p>
     * No need to remove the listener since all listeners are removed
     * automatically once session is destroyed
     * <p>
     * The session being destroyed is locked and its UIs have been removed when
     * the listeners are called.
     *
     * @see VaadinService#addSessionInitListener(SessionInitListener)
     *
     * @param listener
     *            the vaadin service session destroy listener
     */
    public void addDestroyListener(SessionDestroyListener listener) {
        this.addSessionDestroyListener(listener);
    }

}
