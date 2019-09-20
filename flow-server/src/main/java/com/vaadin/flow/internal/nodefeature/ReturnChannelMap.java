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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.StateNode;

import elemental.json.JsonArray;

/**
 * Server-side node feature that keeps track of the return channels registered
 * for a state node.
 * <p>
 * Return channels are only tracked on the server. The client doesn't need to
 * know which channels are registered - it only passes the channel id back to
 * the server. The server checks that the received channel id is (still) valid
 * and logs a warning if it isn't.
 *
 * @author Vaadin Ltd
 * @since 2.0
 */
public class ReturnChannelMap extends ServerSideFeature {

    private class ChannelImpl implements ReturnChannelRegistration {
        private final int channelId;
        private final SerializableBiConsumer<JsonArray, ReturnChannelRegistration> handler;

        private DisabledUpdateMode disabledUpdateMode = DisabledUpdateMode.ONLY_WHEN_ENABLED;

        public ChannelImpl(int channelId,
                SerializableBiConsumer<JsonArray, ReturnChannelRegistration> handler) {
            this.channelId = channelId;
            this.handler = handler;
        }

        @Override
        public void remove() {
            channels.remove(Integer.valueOf(channelId));
        }

        @Override
        public int getStateNodeId() {
            return getNode().getId();
        }

        @Override
        public int getChannelId() {
            return channelId;
        }

        @Override
        public void invoke(JsonArray arguments) {
            handler.accept(arguments, this);
        }

        @Override
        public DisabledUpdateMode getDisabledUpdateMode() {
            return disabledUpdateMode;
        }

        @Override
        public ReturnChannelRegistration setDisabledUpdateMode(
                DisabledUpdateMode disabledUpdateMode) {
            this.disabledUpdateMode = Objects
                    .requireNonNull(disabledUpdateMode);
            return this;
        }
    }

    private int nextId = 0;

    private Map<Integer, ChannelImpl> channels = new HashMap<>();

    /**
     * Creates a new return channel map for the given state node.
     *
     * @param node
     *            the owning state node, not <code>null</code>
     */
    public ReturnChannelMap(StateNode node) {
        super(node);
    }

    /**
     * Registers a new channel based on a callback that receives the provided
     * arguments when a message is passed to the channel.
     * <p>
     * The returned registration can be passed to the client as a parameter to
     * various <code>executeJavaScript</code> methods. The client side
     * representation of the registration will be a function that will send a
     * message to the provided handler.
     *
     * @param handler
     *            the argument handler, not <code>null</code>
     *
     * @return a return channel registration
     */
    public ReturnChannelRegistration registerChannel(
            SerializableConsumer<JsonArray> handler) {
        assert handler != null;

        return registerChannel(
                (arguments, channel) -> handler.accept(arguments));
    }

    /**
     * Registers a new channel based on a callback that receives the provided
     * arguments and the channel registration when a message is passed to the
     * channel. The purpose of passing the registration to the handler is to
     * allow the channel to unregister itself when it receives a message.
     * <p>
     * The returned registration can be passed to the client as a parameter to
     * various <code>executeJavaScript</code> methods. The client side
     * representation of the registration will be a function that will send a
     * message to the provided handler.
     *
     * @param handler
     *            the argument and registration handler, not <code>null</code>
     *
     * @return a return channel registration
     */
    public ReturnChannelRegistration registerChannel(
            SerializableBiConsumer<JsonArray, ReturnChannelRegistration> handler) {
        assert handler != null;

        ChannelImpl channel = new ChannelImpl(nextId++, handler);

        channels.put(Integer.valueOf(channel.getChannelId()), channel);

        return channel;
    }

    /**
     * Gets the return channel registration registered with the provided id, or
     * <code>null</code> if no channel is registered with that id.
     *
     * @param channelId
     *            the channel id to look for
     * @return a return channel registration, or <code>null</code> if no
     *         registration exists for the given id
     */
    public ReturnChannelRegistration get(int channelId) {
        return channels.get(Integer.valueOf(channelId));
    }

}
