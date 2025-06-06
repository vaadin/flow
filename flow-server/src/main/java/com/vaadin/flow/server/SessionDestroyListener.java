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
 * A listener that gets notified when a Vaadin service session is no longer
 * used.
 *
 * @see VaadinService#addSessionDestroyListener(SessionDestroyListener)
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface SessionDestroyListener extends Serializable {
    /**
     * Called when a Vaadin service session is no longer used.
     *
     * @param event
     *            the event with details about the destroyed session
     */
    void sessionDestroy(SessionDestroyEvent event);
}
