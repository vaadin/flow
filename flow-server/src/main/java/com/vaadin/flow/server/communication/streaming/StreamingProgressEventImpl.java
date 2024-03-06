/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.streaming;

import com.vaadin.flow.server.StreamVariable.StreamingProgressEvent;

/**
 * Implementation for {@link StreamingProgressEvent}.
 *
 * @since 1.0
 */
public final class StreamingProgressEventImpl extends AbstractStreamingEvent
        implements StreamingProgressEvent {

    /**
     * Streaming progress event constructor.
     *
     * @param filename
     *            filename
     * @param type
     *            file type
     * @param contentLength
     *            content length
     * @param bytesReceived
     *            current bytes received amount
     */
    public StreamingProgressEventImpl(final String filename, final String type,
            long contentLength, long bytesReceived) {
        super(filename, type, contentLength, bytesReceived);
    }

}
