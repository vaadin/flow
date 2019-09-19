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
