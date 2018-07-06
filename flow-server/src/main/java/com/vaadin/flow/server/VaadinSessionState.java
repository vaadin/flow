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

import com.vaadin.flow.component.UI;

/**
 * The lifecycle state of a VaadinSession.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public enum VaadinSessionState {
    /**
     * The session is active and accepting client requests.
     */
    OPEN,
    /**
     * The {@link VaadinSession#close() close} method has been called; the
     * session will be closed as soon as the current request ends.
     */
    CLOSING,
    /**
     * The session is closed; all the {@link UI}s have been removed and
     * {@link SessionDestroyListener}s have been called.
     */
    CLOSED;

}
