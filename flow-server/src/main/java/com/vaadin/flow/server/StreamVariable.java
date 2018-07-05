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

import java.io.OutputStream;
import java.io.Serializable;

/**
 * StreamVariable is a special kind of variable whose value is streamed to an
 * {@link OutputStream} provided by the {@link #getOutputStream()} method. E.g.
 * in web terminals {@link StreamVariable} can be used to send large files from
 * browsers to the server without consuming large amounts of memory.
 * <p>
 * Note, writing to the {@link OutputStream} is not synchronized by the handler
 * (to avoid stalls in other operations when e.g. streaming to a slow network
 * service or file system). If UI is changed as a side effect of writing to the
 * output stream, developer must handle synchronization manually.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 */
public interface StreamVariable extends Serializable {

    /**
     * Invoked when a new upload arrives, after
     * {@link #streamingStarted(StreamingStartEvent)} method has been called.
     * The implementation will write the streamed variable to the
     * returned output stream.
     *
     * @return Stream to which the uploaded file should be written.
     */
    OutputStream getOutputStream();

    /**
     * Whether the {@link #onProgress(StreamingProgressEvent)} method should be
     * called during the upload.
     * <p>
     * {@link #onProgress(StreamingProgressEvent)} is called in a synchronized
     * block when the content is being received. This is potentially bit slow,
     * so we are calling that method only if requested. The value is requested
     * after the {@link #streamingStarted(StreamingStartEvent)} event, but not
     * after reading each buffer.
     *
     * @return true if this {@link StreamVariable} wants to by notified during
     *         the upload of the progress of streaming.
     * @see #onProgress(StreamingProgressEvent)
     */
    boolean listenProgress();

    /**
     * This method is called if {@link #listenProgress()}
     * returns true when the streaming starts.
     * 
     * @param event
     *            streaming progress event
     */
    void onProgress(StreamingProgressEvent event);

    /**
     * This method is called when the streaming starts.
     *
     * @param event
     *            streaming start event
     */
    void streamingStarted(StreamingStartEvent event);

    /**
     * This method is called when the streaming has finished.
     *
     * @param event
     *            streaming end event
     */
    void streamingFinished(StreamingEndEvent event);

    /**
     * This method is called when the streaming has failed.
     *
     * @param event
     *            streaming error event
     */
    void streamingFailed(StreamingErrorEvent event);

    /*
     * Not synchronized to avoid stalls (caused by UIDL requests) while
     * streaming the content. Implementations also most commonly atomic even
     * without the restriction.
     */
    /**
     * If this method returns true while the content is being streamed the
     * handler is told to stop receiving the current upload.
     * <p>
     * Note, the usage of this method is not synchronized over the Application
     * instance like other methods. The implementation should
     * only return a boolean field and especially not modify UI or implement a
     * synchronization by itself.
     *
     * @return true if the streaming should be interrupted as soon as possible.
     */
    boolean isInterrupted();

    /**
     * Streaming event interface.
     */
    interface StreamingEvent extends Serializable {

        /**
         * Get the file name for the stream.
         * 
         * @return the file name of the streamed file if known
         */
        String getFileName();

        /**
         * Get the mime type for the stream.
         * 
         * @return the mime type of the streamed file if known
         */
        String getMimeType();

        /**
         * Get the content length.
         * 
         * @return the length of the stream (in bytes) if known, else -1
         */
        long getContentLength();

        /**
         * Get the number of bytes streamed.
         * 
         * @return then number of bytes streamed to StreamVariable
         */
        long getBytesReceived();
    }

    /**
     * Event passed to {@link #streamingStarted(StreamingStartEvent)} method
     * before the streaming of the content to {@link StreamVariable} starts.
     */
    interface StreamingStartEvent extends StreamingEvent {
        /**
         * The owner of the StreamVariable can call this method to inform the
         * implementation that this StreamVariable will not be used to
         * accept more post.
         */
        void disposeStreamVariable();
    }

    /**
     * Event passed to {@link #onProgress(StreamingProgressEvent)} method during
     * the streaming progresses.
     */
    interface StreamingProgressEvent extends StreamingEvent {
    }

    /**
     * Event passed to {@link #streamingFinished(StreamingEndEvent)} method the
     * contents have been streamed to StreamVariable successfully.
     */
    interface StreamingEndEvent extends StreamingEvent {
    }

    /**
     * Event passed to {@link #streamingFailed(StreamingErrorEvent)} method when
     * the streaming ended before the end of the input. The streaming may fail
     * due an interruption or due an other unknown exception in communication.
     * 
     * In the latter case the exception is also passed to
     * {@link ErrorHandler#error(ErrorEvent)}.
     */
    interface StreamingErrorEvent extends StreamingEvent {

        /**
         * Get the exception that failed the stream.
         * 
         * @return the exception that caused the receiving not to finish cleanly
         */
        Exception getException();

    }

}
