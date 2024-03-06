/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication.streaming;

import com.vaadin.flow.server.StreamVariable.StreamingEndEvent;

/**
 * Implementation of {@link StreamingEndEvent}.
 *
 * @since 1.0
 */
public final class StreamingEndEventImpl extends AbstractStreamingEvent
        implements StreamingEndEvent {

    /**
     * End event constructor.
     *
     * @param filename
     *            filename
     * @param type
     *            file type
     * @param totalBytes
     *            total size in bytes
     */
    public StreamingEndEventImpl(String filename, String type,
            long totalBytes) {
        super(filename, type, totalBytes, totalBytes);
    }

}
