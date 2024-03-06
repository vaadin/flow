/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
     * {@code resourceName} parameter value will be used in URI (generated when
     * resource is registered) in a way that the {@code resourceName} is the
     * last segment of the path. So this is synthetic file name (not real one).
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
