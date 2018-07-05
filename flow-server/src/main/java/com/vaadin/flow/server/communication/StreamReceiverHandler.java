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
package com.vaadin.flow.server.communication;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.NoInputStreamException;
import com.vaadin.flow.server.NoOutputStreamException;
import com.vaadin.flow.server.StreamReceiver;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamVariable;
import com.vaadin.flow.server.UploadException;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.streaming.StreamingEndEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingErrorEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingProgressEventImpl;
import com.vaadin.flow.server.communication.streaming.StreamingStartEventImpl;
import com.vaadin.flow.shared.ApplicationConstants;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class StreamReceiverHandler implements Serializable {

    private static final int MAX_UPLOAD_BUFFER_SIZE = 4 * 1024;

    /* Minimum interval which will be used for streaming progress events. */
    public static final int DEFAULT_STREAMING_PROGRESS_EVENT_INTERVAL_MS = 500;

    /**
     * An UploadInterruptedException will be thrown by an ongoing upload if
     * {@link StreamVariable#isInterrupted()} returns <code>true</code>.
     *
     * By checking the exception of an
     * {@link StreamVariable.StreamingErrorEvent} or {link FailedEvent} against
     * this class, it is possible to determine if an upload was interrupted by
     * code or aborted due to any other exception.
     */
    public static class UploadInterruptedException extends Exception {

        /**
         * Constructs an instance of <code>UploadInterruptedException</code>.
         */
        public UploadInterruptedException() {
            super("Upload interrupted by other thread");
        }
    }

    /**
     * Handle reception of incoming stream from the client.
     * 
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     * @param response
     *            The response object to which a response can be written.
     * @param streamReceiver
     *            the receiver containing the destination stream variable
     * @param uiId
     *            id of the targeted ui
     * @param securityKey
     *            security from the request that should match registered stream
     *            receiver id
     * @throws IOException
     *             if an IO error occurred
     */
    public void handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response, StreamReceiver streamReceiver, String uiId,
            String securityKey) throws IOException {
        StateNode source;

        session.lock();
        try {
            String secKey = streamReceiver.getId();
            if (secKey == null || !secKey.equals(securityKey)) {
                getLogger().warn(
                        "Received incoming stream with faulty security key.");
                return;
            }

            UI ui = session.getUIById(Integer.parseInt(uiId));
            UI.setCurrent(ui);

            source = streamReceiver.getNode();

        } finally {
            session.unlock();
        }

        try {
            if (ServletFileUpload
                    .isMultipartContent((HttpServletRequest) request)) {
                doHandleMultipartFileUpload(session, request, response,
                        streamReceiver, source);
            } else {
                // if boundary string does not exist, the posted file is from
                // XHR2.post(File)
                doHandleXhrFilePost(session, request, response, streamReceiver,
                        source, getContentLength(request));
            }
        } finally {
            UI.setCurrent(null);
        }
    }

    /**
     * Method used to stream content from a multipart request to given
     * StreamVariable.
     * <p>
     * This method takes care of locking the session as needed and does not
     * assume the caller has locked the session. This allows the session to be
     * locked only when needed and not when handling the upload data.
     *
     * @param session
     *            The session containing the stream variable
     * @param request
     *            The upload request
     * @param response
     *            The upload response
     * @param streamReceiver
     *            the receiver containing the destination stream variable
     * @param owner
     *            The owner of the stream
     * @throws IOException
     *             If there is a problem reading the request or writing the
     *             response
     */
    protected void doHandleMultipartFileUpload(VaadinSession session,
            VaadinRequest request, VaadinResponse response,
            StreamReceiver streamReceiver, StateNode owner) throws IOException {

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload();

        long contentLength = getContentLength(request);
        // Parse the request
        FileItemIterator iter;
        try {
            iter = upload.getItemIterator((HttpServletRequest) request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                handleStream(session, streamReceiver, owner, contentLength,
                        item);
            }
        } catch (FileUploadException e) {
            getLogger().warn("File upload failed.", e);
        }
        sendUploadResponse(response);
    }

    private void handleStream(VaadinSession session,
            StreamReceiver streamReceiver, StateNode owner, long contentLength,
            FileItemStream item) throws IOException {
        String name = item.getName();
        InputStream stream = item.openStream();
        try {
            handleFileUploadValidationAndData(session, stream, streamReceiver,
                    name, item.getContentType(), contentLength, owner);
        } catch (UploadException e) {
            session.getErrorHandler().error(new ErrorEvent(e));
        }
    }

    /**
     * Used to stream plain file post (aka XHR2.post(File))
     * <p>
     * This method takes care of locking the session as needed and does not
     * assume the caller has locked the session. This allows the session to be
     * locked only when needed and not when handling the upload data.
     * </p>
     *
     * @param session
     *            The session containing the stream variable
     * @param request
     *            The upload request
     * @param response
     *            The upload response
     * @param streamReceiver
     *            the receiver containing the destination stream variable
     * @param owner
     *            The owner of the stream
     * @param contentLength
     *            The length of the request content
     * @throws IOException
     *             If there is a problem reading the request or writing the
     *             response
     */
    protected void doHandleXhrFilePost(VaadinSession session,
            VaadinRequest request, VaadinResponse response,
            StreamReceiver streamReceiver, StateNode owner, long contentLength)
            throws IOException {

        // These are unknown in filexhr ATM, maybe add to Accept header that
        // is accessible in portlets
        final String filename = "unknown";
        final String mimeType = filename;
        final InputStream stream = request.getInputStream();

        try {
            handleFileUploadValidationAndData(session, stream, streamReceiver,
                    filename, mimeType, contentLength, owner);
        } catch (UploadException e) {
            session.getErrorHandler().error(new ErrorEvent(e));
        }
        sendUploadResponse(response);
    }

    private void handleFileUploadValidationAndData(VaadinSession session,
            InputStream inputStream, StreamReceiver streamReceiver,
            String filename, String mimeType, long contentLength,
            StateNode node) throws UploadException {
        session.lock();
        try {
            if (node == null) {
                throw new UploadException(
                        "File upload ignored because the node for the stream variable was not found");
            }
            if (!node.isAttached()) {
                throw new UploadException("Warning: file upload ignored for "
                        + node.getId() + " because the component was disabled");
            }
        } finally {
            session.unlock();
        }
        try {
            // Store ui reference so we can do cleanup even if node is
            // detached in some event handler
            boolean forgetVariable = streamToReceiver(session, inputStream,
                    streamReceiver, filename, mimeType, contentLength);
            if (forgetVariable) {
                cleanStreamVariable(session, streamReceiver);
            }
        } catch (Exception e) {
            session.lock();
            try {
                session.getErrorHandler().error(new ErrorEvent(e));
            } finally {
                session.unlock();
            }
        }
    }

    /**
     * To prevent event storming, streaming progress events are sent in this
     * interval rather than every time the buffer is filled. This fixes #13155.
     * To adjust this value override the method, and register your own handler
     * in VaadinService.createRequestHandlers(). The default is 500ms, and
     * setting it to 0 effectively restores the old behavior.
     * 
     * @return the minimum interval to be used for streaming progress events
     */
    protected int getProgressEventInterval() {
        return DEFAULT_STREAMING_PROGRESS_EVENT_INTERVAL_MS;
    }

    static void tryToCloseStream(OutputStream out) {
        try {
            // try to close output stream (e.g. file handle)
            if (out != null) {
                out.close();
            }
        } catch (IOException ioe) {
            getLogger().debug("Exception closing stream", ioe);
        }
    }

    /**
     * Build response for handled download.
     *
     * @param response
     *            response to write to
     * @throws IOException
     *             exception when writing to stream
     */
    private void sendUploadResponse(VaadinResponse response)
            throws IOException {
        response.setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        try (OutputStream out = response.getOutputStream()) {
            final PrintWriter outWriter = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(out, UTF_8)));
            try {
                outWriter.print("<html><body>download handled</body></html>");
            } finally {
                outWriter.flush();
            }
        }
    }

    private void cleanStreamVariable(VaadinSession session,
            StreamReceiver streamReceiver) {
        session.lock();
        try {
            session.getResourceRegistry().unregisterResource(streamReceiver);
        } finally {
            session.unlock();
        }
    }

    private final boolean streamToReceiver(VaadinSession session,
            final InputStream in, StreamReceiver streamReceiver,
            String filename, String type, long contentLength)
            throws UploadException {
        StreamVariable streamVariable = streamReceiver.getStreamVariable();
        if (streamVariable == null) {
            throw new IllegalStateException(
                    "StreamVariable for the post not found");
        }

        OutputStream out = null;
        long totalBytes = 0;
        StreamingStartEventImpl startedEvent = new StreamingStartEventImpl(
                filename, type, contentLength);
        try {
            boolean listenProgress;
            session.lock();
            try {
                streamVariable.streamingStarted(startedEvent);
                out = streamVariable.getOutputStream();
                listenProgress = streamVariable.listenProgress();
            } finally {
                session.unlock();
            }

            // Gets the output target stream
            if (out == null) {
                throw new NoOutputStreamException();
            }

            if (null == in) {
                // No file, for instance non-existent filename in html upload
                throw new NoInputStreamException();
            }

            final byte[] buffer = new byte[MAX_UPLOAD_BUFFER_SIZE];
            long lastStreamingEvent = 0;
            int bytesReadToBuffer;
            do {
                bytesReadToBuffer = in.read(buffer);
                if (bytesReadToBuffer > 0) {
                    out.write(buffer, 0, bytesReadToBuffer);
                    totalBytes += bytesReadToBuffer;
                }
                if (listenProgress) {
                    StreamingProgressEventImpl progressEvent = new StreamingProgressEventImpl(
                            filename, type, contentLength, totalBytes);

                    lastStreamingEvent = updateProgress(session, streamVariable,
                            progressEvent, lastStreamingEvent,
                            bytesReadToBuffer);
                }
                if (streamVariable.isInterrupted()) {
                    throw new UploadInterruptedException();
                }
            } while (bytesReadToBuffer > 0);

            // upload successful
            out.close();
            StreamVariable.StreamingEndEvent event = new StreamingEndEventImpl(
                    filename, type, totalBytes);
            session.lock();
            try {
                streamVariable.streamingFinished(event);
            } finally {
                session.unlock();
            }

        } catch (UploadInterruptedException e) {
            // Download interrupted by application code
            tryToCloseStream(out);
            StreamVariable.StreamingErrorEvent event = new StreamingErrorEventImpl(
                    filename, type, contentLength, totalBytes, e);
            session.lock();
            try {
                streamVariable.streamingFailed(event);
            } finally {
                session.unlock();
            }
            // Note, we are not throwing interrupted exception forward as it is
            // not a terminal level error like all other exception.
        } catch (final Exception e) {
            tryToCloseStream(out);
            session.lock();
            try {
                StreamVariable.StreamingErrorEvent event = new StreamingErrorEventImpl(
                        filename, type, contentLength, totalBytes, e);
                streamVariable.streamingFailed(event);
                // throw exception for terminal to be handled (to be passed to
                // terminalErrorHandler)
                throw new UploadException(e);
            } finally {
                session.unlock();
            }
        }
        return startedEvent.isDisposed();
    }

    private long updateProgress(VaadinSession session,
            StreamVariable streamVariable,
            StreamingProgressEventImpl progressEvent, long lastStreamingEvent,
            int bytesReadToBuffer) {
        long now = System.currentTimeMillis();
        // to avoid excessive session locking and event storms,
        // events are sent in intervals, or at the end of the file.
        if (lastStreamingEvent + getProgressEventInterval() <= now
                || bytesReadToBuffer <= 0) {
            session.lock();
            try {
                streamVariable.onProgress(progressEvent);
            } finally {
                session.unlock();
            }
        }
        return now;
    }

    /**
     * The request.getContentLength() is limited to "int" by the Servlet
     * specification. To support larger file uploads manually evaluate the
     * Content-Length header which can contain long values.
     */
    private long getContentLength(VaadinRequest request) {
        try {
            return Long.parseLong(request.getHeader("Content-Length"));
        } catch (NumberFormatException e) {
            return -1l;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StreamReceiverHandler.class.getName());
    }
}
