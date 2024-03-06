/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.streaming;

import com.vaadin.flow.server.StreamVariable.StreamingStartEvent;

/**
 * Implementation for {@link StreamingStartEvent}.
 *
 * @since 1.0
 */
public final class StreamingStartEventImpl extends AbstractStreamingEvent
        implements StreamingStartEvent {

    private boolean disposed;

    /**
     * Streaming start event constructor.
     *
     * @param filename
     *            filename
     * @param type
     *            file type
     * @param contentLength
     *            content length
     */
    public StreamingStartEventImpl(final String filename, final String type,
            long contentLength) {
        super(filename, type, contentLength, 0);
    }

    @Override
    public void disposeStreamVariable() {
        disposed = true;
    }

    /**
     * Get if stream resource should be unregistered.
     *
     * @return true if stream variable should be unregistered
     */
    public boolean isDisposed() {
        return disposed;
    }

}
