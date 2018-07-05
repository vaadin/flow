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

import com.vaadin.flow.internal.StateNode;

/**
 * Represents a receiver for data upload from the client.
 * <p>
 * The instance should be registered via
 * {@link StreamResourceRegistry#registerResource(AbstractStreamResource)}. This
 * method returns an object which may be used to get resource URI.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StreamReceiver extends AbstractStreamResource {

    private StateNode node;

    private final String attributeName;

    private final StreamVariable streamVariable;

    /**
     * Creates {@link StreamReceiver} instance for {@code streamVariable} as a
     * data receiver.
     * <p>
     * {@code resourceName} parameter value will be used in URI (generated when resource
     * is registered) in a way that the {@code resourceName} is the last segment of the
     * path. So this is synthetic file name (not real one).
     *
     * @param node
     *            receiver node
     * @param resourceName
     *            resource name for registration URI
     * @param streamVariable
     *            stream variable for controlling the upload stream
     */
    public StreamReceiver(StateNode node, String resourceName,
            StreamVariable streamVariable) {
        assert node != null;
        assert resourceName != null;
        assert streamVariable != null;

        this.node = node;
        this.streamVariable = streamVariable;
        this.attributeName = resourceName;
    }

    /**
     * Get the node that this stream receiver is linked to.
     * 
     * @return bound node
     */
    public StateNode getNode() {
        return node;
    }

    /**
     * Get the {@link StreamVariable} for this stream receiver.
     * 
     * @return stream variable for this receiver
     */
    public StreamVariable getStreamVariable() {
        return streamVariable;
    }

    @Override
    public String getName() {
        return attributeName;
    }
}
