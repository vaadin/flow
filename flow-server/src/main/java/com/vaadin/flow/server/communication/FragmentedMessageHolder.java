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
package com.vaadin.flow.server.communication;

import java.io.Serializable;

import org.atmosphere.cpr.AtmosphereResource;

import com.vaadin.flow.server.communication.AtmospherePushConnection.FragmentedMessage;

/**
 * Manages fragmented messages during Atmosphere push communication.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public interface FragmentedMessageHolder extends Serializable {

    /**
     * Gets the partial message that is currently being received, if any.
     *
     * @param resource
     *            the resource to get the partial message from
     * @return the fragmented message being received or a new empty instance
     */
    public FragmentedMessage getOrCreateFragmentedMessage(
            AtmosphereResource resource);

    /**
     * Clears the partial message that is currently being received. Should be
     * called when the whole message has been received.
     *
     * @param resource
     *            the related resource
     */
    public void clearFragmentedMessage(AtmosphereResource resource);
}
