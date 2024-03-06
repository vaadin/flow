/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.streaming;

import com.vaadin.flow.server.StreamVariable.StreamingErrorEvent;

/**
 * Implementation for {@link StreamingErrorEvent}.
 *
 * @since 1.0
 */
public final class StreamingErrorEventImpl extends AbstractStreamingEvent
        implements StreamingErrorEvent {

    private final Exception exception;

    /**
     * Streaming error event constructor.
     *
     * @param filename
     *            filename
     * @param type
     *            file type
     * @param contentLength
     *            content length
     * @param bytesReceived
     *            amount of bytes received
     * @param exception
     *            exception for stream failure
     */
    public StreamingErrorEventImpl(final String filename, final String type,
            long contentLength, long bytesReceived, final Exception exception) {
        super(filename, type, contentLength, bytesReceived);
        this.exception = exception;
    }

    @Override
    public Exception getException() {
        return exception;
    }

}
