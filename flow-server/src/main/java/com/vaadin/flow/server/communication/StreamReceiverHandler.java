/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.Pair;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.HttpStatusCode;
import com.vaadin.flow.server.NoInputStreamException;
import com.vaadin.flow.server.NoOutputStreamException;
import com.vaadin.flow.server.StreamReceiver;
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

import static com.vaadin.flow.server.Constants.DEFAULT_FILE_COUNT_MAX;
import static com.vaadin.flow.server.Constants.DEFAULT_FILE_SIZE_MAX;
import static com.vaadin.flow.server.Constants.DEFAULT_REQUEST_SIZE_MAX;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Handles {@link StreamReceiver} instances registered in {@link VaadinSession}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class StreamReceiverHandler implements Serializable {

    private static final int MAX_UPLOAD_BUFFER_SIZE = 4 * 1024;

    /* Minimum interval which will be used for streaming progress events. */
    public static final int DEFAULT_STREAMING_PROGRESS_EVENT_INTERVAL_MS = 500;

    private long requestSizeMax = DEFAULT_REQUEST_SIZE_MAX;

    private long fileSizeMax = DEFAULT_FILE_SIZE_MAX;

    private long fileCountMax = DEFAULT_FILE_COUNT_MAX;

    /**
     * An UploadInterruptedException will be thrown by an ongoing upload if
     * {@link StreamVariable#isInterrupted()} returns <code>true</code>.
     * <p>
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

    enum UploadStatus {
        OK, ERROR
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
            if (secKey == null || !MessageDigest.isEqual(
                    secKey.getBytes(StandardCharsets.UTF_8),
                    securityKey.getBytes(StandardCharsets.UTF_8))) {
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
            if (isMultipartUpload(request)) {
                doHandleMultipartFileUpload(session, request, response,
                        streamReceiver, source);
            } else {
                // if boundary string does not exist, the posted file is from
                // XHR2.post(File)
                doHandleXhrFilePost(session, request, response, streamReceiver,
                        source, request.getContentLengthLong());
            }
        } finally {
            UI.setCurrent(null);
        }
    }

    /**
     * Streams content from a multipart request to given StreamVariable.
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
        boolean success = false;
        try {
            success = handleMultipartFileUploadFromParts(session, request,
                    streamReceiver, owner);
        } catch (IOException exception) {
            getLogger().warn(
                    "IO Exception during file upload, fired as StreamingErrorEvent",
                    exception);
        } catch (Exception exception) {
            session.lock();
            try {
                session.getErrorHandler().error(new ErrorEvent(exception));
            } finally {
                session.unlock();
            }
        }
        sendUploadResponse(response, success);
    }

    private boolean handleMultipartFileUploadFromParts(VaadinSession session,
            VaadinRequest request, StreamReceiver streamReceiver,
            StateNode owner) throws IOException {
        boolean success = true;
        try {
            Collection<Part> parts = getParts(request);

            if (parts.isEmpty()) {
                getLogger().warn("Multipart request has no parts");
                return false;
            }

            validateUploadLimits(request, parts);

            Iterator<Part> iter = parts.iterator();
            while (iter.hasNext()) {
                Part part = iter.next();
                boolean partSuccess = handleStream(session, streamReceiver,
                        owner, part);
                success = success && partSuccess;
            }
        } catch (UploadSizeLimitExceededException
                | UploadFileSizeLimitExceededException
                | UploadFileCountLimitExceededException e) {
            String limitInfoStr = "{} limit exceeded. To increase the limit "
                    + "extend StreamRequestHandler, override {} method and "
                    + "provide a higher limit. The extended class needs to be "
                    + "added to request handlers with "
                    + "ServiceInitEvent::addRequestHandler in an extension of "
                    + "VaadinServiceInitListener.";
            if (e instanceof UploadSizeLimitExceededException) {
                getLogger().warn(limitInfoStr, "Request size",
                        "getRequestSizeMax");
            } else if (e instanceof UploadFileSizeLimitExceededException) {
                getLogger().warn(limitInfoStr, "File size", "getFileSizeMax");
            } else if (e instanceof UploadFileCountLimitExceededException) {
                getLogger().warn(limitInfoStr, "File count", "getFileCountMax");
            }
            success = false;
            getLogger().warn("File upload failed.", e);
        } catch (Exception e) {
            success = false;
            getLogger().warn("File upload failed.", e);
        }
        return success;
    }

    private boolean handleStream(VaadinSession session,
            StreamReceiver streamReceiver, StateNode owner, Part part)
            throws IOException {
        String name = part.getSubmittedFileName();
        InputStream stream = part.getInputStream();
        try {
            return handleFileUploadValidationAndData(session, stream,
                    streamReceiver, name, part.getContentType(), part.getSize(),
                    owner);
        } catch (UploadException e) {
            session.getErrorHandler().error(new ErrorEvent(e));
        }
        return false;
    }

    /**
     * Used to stream plain file post (aka XHR2.post(File))
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

        boolean success = false;
        try {
            success = handleFileUploadValidationAndData(session, stream,
                    streamReceiver, filename, mimeType, contentLength, owner);
        } catch (UploadException e) {
            session.getErrorHandler().error(new ErrorEvent(e));
        }
        sendUploadResponse(response, success);
    }

    /**
     * Validate that stream target is in a valid state for receiving data and
     * send stream to receiver. Handles cleanup and error in reading stream
     *
     * @param session
     *            The session containing the stream variable
     * @param inputStream
     *            the request content input stream
     * @param streamReceiver
     *            the receiver containing the destination stream variable
     * @param filename
     *            name of the streamed file
     * @param mimeType
     *            file mime type
     * @param contentLength
     *            The length of the request content
     * @param node
     *            The owner of the stream
     * @return true if upload successful, else false
     * @throws UploadException
     *             Thrown for illegal target node state
     */
    protected boolean handleFileUploadValidationAndData(VaadinSession session,
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
        try (InputStream handledStream = inputStream) {
            // Store ui reference so we can do cleanup even if node is
            // detached in some event handler
            Pair<Boolean, UploadStatus> result = streamToReceiver(session,
                    handledStream, streamReceiver, filename, mimeType,
                    contentLength);
            if (result.getFirst()) {
                cleanStreamVariable(session, streamReceiver);
            }
            return result.getSecond() == UploadStatus.OK;
        } catch (IOException ioe) {
            // Mostly premature closing of stream from client that throws on
            // close
            getLogger().debug("Exception closing inputStream", ioe);
        } catch (UploadException e) {
            session.lock();
            try {
                session.getErrorHandler().error(new ErrorEvent(e));
            } finally {
                session.unlock();
            }
        }
        return false;
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
     * @param success
     *            whether the upload was successful
     * @throws IOException
     *             exception when writing to stream
     */
    protected void sendUploadResponse(VaadinResponse response, boolean success)
            throws IOException {
        response.setContentType(
                ApplicationConstants.CONTENT_TYPE_TEXT_HTML_UTF_8);
        if (success) {
            try (OutputStream out = response.getOutputStream()) {
                final PrintWriter outWriter = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(out, UTF_8)));
                try {
                    outWriter.print(
                            "<html><body>download handled</body></html>");
                } finally {
                    outWriter.flush();
                }
            }
        } else {
            response.setStatus(HttpStatusCode.INTERNAL_SERVER_ERROR.getCode());
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

    private final Pair<Boolean, UploadStatus> streamToReceiver(
            VaadinSession session, final InputStream in,
            StreamReceiver streamReceiver, String filename, String type,
            long contentLength) throws UploadException {
        StreamVariable streamVariable = streamReceiver.getStreamVariable();
        if (streamVariable == null) {
            throw new IllegalStateException(
                    "StreamVariable for the post not found");
        }

        OutputStream out = null;
        long totalBytes = 0;
        StreamingStartEventImpl startedEvent = new StreamingStartEventImpl(
                filename, type, contentLength);
        boolean success = false;
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
            success = true;
        } catch (UploadInterruptedException | IOException e) {
            // Download is either interrupted by application code or some
            // IOException happens
            onStreamingFailed(session, filename, type, contentLength,
                    streamVariable, out, totalBytes, e);
            // Interrupted exception and IOException are not thrown forward:
            // it's enough to fire them via streamVariable
        } catch (final Exception e) {
            onStreamingFailed(session, filename, type, contentLength,
                    streamVariable, out, totalBytes, e);
            // Throw not IOException and interrupted exception for terminal to
            // be handled (to be passed to terminalErrorHandler): such
            // exceptions mean mistakes in the implementation logic (not upload
            // I/O operations).
            throw new UploadException(e);
        }
        return new Pair<>(startedEvent.isDisposed(),
                success ? UploadStatus.OK : UploadStatus.ERROR);
    }

    private void onStreamingFailed(VaadinSession session, String filename,
            String type, long contentLength, StreamVariable streamVariable,
            OutputStream out, long totalBytes, final Exception exception) {
        tryToCloseStream(out);
        session.lock();
        try {
            streamVariable.streamingFailed(new StreamingErrorEventImpl(filename,
                    type, contentLength, totalBytes, exception));
        } finally {
            session.unlock();
        }
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
            return now;
        }
        return lastStreamingEvent;
    }

    protected boolean isMultipartUpload(VaadinRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return false;
        }
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase().startsWith("multipart/");
    }

    protected Collection<Part> getParts(VaadinRequest request)
            throws Exception {
        return ((HttpServletRequest) request).getParts();
    }

    /**
     * Validates upload limits for the given parts.
     *
     * @param request
     *            the request
     * @param parts
     *            the parts to validate
     * @throws UploadSizeLimitExceededException
     *             if the total size exceeds the limit
     * @throws UploadFileSizeLimitExceededException
     *             if a file size exceeds the limit
     * @throws UploadFileCountLimitExceededException
     *             if the file count exceeds the limit
     */
    protected void validateUploadLimits(VaadinRequest request,
            Collection<Part> parts) throws UploadSizeLimitExceededException,
            UploadFileSizeLimitExceededException,
            UploadFileCountLimitExceededException {
        // Check file count
        if (fileCountMax > -1 && parts.size() > fileCountMax) {
            throw new UploadFileCountLimitExceededException(parts.size(),
                    fileCountMax);
        }

        // Check total request size
        long contentLength = request.getContentLengthLong();
        if (requestSizeMax > -1 && contentLength > requestSizeMax) {
            throw new UploadSizeLimitExceededException(contentLength,
                    requestSizeMax);
        }

        // Check individual file sizes
        if (fileSizeMax > -1) {
            for (Part part : parts) {
                long size = part.getSize();
                if (size > fileSizeMax) {
                    throw new UploadFileSizeLimitExceededException(
                            part.getSubmittedFileName(), size, fileSizeMax);
                }
            }
        }
    }

    public void setRequestSizeMax(long requestSizeMax) {
        this.requestSizeMax = requestSizeMax;
    }

    public void setFileSizeMax(long fileSizeMax) {
        this.fileSizeMax = fileSizeMax;
    }

    public void setFileCountMax(long fileCountMax) {
        this.fileCountMax = fileCountMax;
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(StreamReceiverHandler.class.getName());
    }
}
