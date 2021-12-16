/*
 * Copyright 2000-2022 Vaadin Ltd.
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

/**
 * Provides a way to reload browser tabs via web socket connection passed as a
 * AtmosphereResource.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
public interface BrowserLiveReload {

    /**
     * Live reload enabling technology detected.
     */
    enum Backend {
        HOTSWAP_AGENT, JREBEL, SPRING_BOOT_DEVTOOLS;
    }

    /**
     * Detects and return enabling live reload backend technology.
     * 
     * @return enabling technology, or <code>null</code> if none
     */
    Backend getBackend();

    /**
     * Sets the live reload backend technology explicitly.
     *
     * @param backend
     *            enabling technology, not <code>null</code>.
     */
    void setBackend(Backend backend);

    /**
     * Requests reload via the resource provided via
     * onConnect(AtmosphereResource).
     */
    void reload();

    /**
     * Called when any message is received through the connection.
     */
    void onMessage(String msg);

}
