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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.shared.Registration;

import elemental.json.JsonArray;

/**
 * A registration for a return channel. A new return channel can be registered
 * for a state node using
 * {@link ReturnChannelMap#registerChannel(com.vaadin.flow.function.SerializableConsumer)}.
 * The registration can be passed as a parameter to various
 * <code>executeJavaScript</code> methods and will be represented on the client
 * as a function that calls the registered handler.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public interface ReturnChannelRegistration extends Registration {
    /**
     * Gets the id of the state node to which the return channel belongs.
     *
     * @return the state node id
     */
    int getStateNodeId();

    /**
     * Gets the id that identifies this channel within its state node.
     *
     * @return the channel id
     */
    int getChannelId();

    /**
     * Invokes the channel handler with the give arguments.
     *
     * @param arguments
     *            a JSON array containing passed from the client, not
     *            <code>null</code>
     */
    void invoke(JsonArray arguments);

    /**
     * Gets the setting for whether this channel will receive updates in case
     * the state node is disabled. By default, updates are allowed only when the
     * state node is enabled.
     *
     * @return the disabled update mode, not <code>null</code>
     */
    DisabledUpdateMode getDisabledUpdateMode();

    /**
     * Sets whether this channel will receive updates in case the state node is
     * disabled.
     *
     * @param disabledUpdateMode
     *            the disabled update mode to use, not <code>null</code>
     * @return this channel registration, for chaining
     */
    ReturnChannelRegistration setDisabledUpdateMode(
            DisabledUpdateMode disabledUpdateMode);

}
