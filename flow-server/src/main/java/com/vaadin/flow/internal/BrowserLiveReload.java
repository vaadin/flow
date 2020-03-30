/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import org.atmosphere.cpr.AtmosphereResource;

/**
 * Provides a way to reload browser tabs via web socket connection passed as a
 * {@link AtmosphereResource}.
 *
 * @author Vaadin Ltd
 *
 */
public interface BrowserLiveReload {

    /**
     * Sets the web socket connection resource when it's established.
     *
     * @param resource
     *            a web socket connection resource, not <code>null</code>.
     */
    void onConnect(AtmosphereResource resource);

    /**
     * Removes the web socket connection resource, not <code>null</code>.
     *
     * @param resource
     *            a web socket connection resource
     */
    void onDisconnect(AtmosphereResource resource);

    /**
     * Returns whether the passed connection is a browser live-reload
     * connection.
     * 
     * @param resource
     *            a web socket connection resource,  not <code>null</code>.
     * @return whether the web socket connection is for live reload
     */
    boolean isLiveReload(AtmosphereResource resource);

    /**
     * Requests reload via the resource provided via
     * {@link #onConnect(AtmosphereResource)} call.
     */
    void reload();

}
