/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
 * <p>
 * For internal use only. May be renamed or removed in a future release.
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
