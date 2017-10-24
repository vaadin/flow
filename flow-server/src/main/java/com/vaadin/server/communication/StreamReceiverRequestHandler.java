/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.communication;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import com.vaadin.flow.StateNode;
import com.vaadin.server.ErrorEvent;
import com.vaadin.server.NoInputStreamException;
import com.vaadin.server.NoOutputStreamException;
import com.vaadin.server.StreamReceiver;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamVariable;
import com.vaadin.server.UploadException;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinResponse;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.communication.streaming.StreamingEndEventImpl;
import com.vaadin.server.communication.streaming.StreamingErrorEventImpl;
import com.vaadin.server.communication.streaming.StreamingProgressEventImpl;
import com.vaadin.server.communication.streaming.StreamingStartEventImpl;
import com.vaadin.ui.UI;

/**
 * Handles {@link StreamResource} instances registered in {@link VaadinSession}.
 *
 * @author Vaadin Ltd
 *
 */
public class StreamReceiverRequestHandler {

    private static final char PATH_SEPARATOR = '/';

    /**
     * Dynamic resource URI prefix.
     */
    static final String DYN_RES_PREFIX = "VAADIN/dynamic/file-upload/";

    public static final String CONTENT_TYPE_TEXT_HTML_UTF_8 = "text/html; charset=utf-8";

    private static final int MAX_UPLOAD_BUFFER_SIZE = 4 * 1024;

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

    public boolean handleRequest(VaadinSession session, VaadinRequest request,
            VaadinResponse response, StreamReceiver streamReceiver, String uiId,
            String securityKey) throws IOException {
        StateNode source;

        session.lock();
        try {
            String secKey = streamReceiver.getId();
            if (secKey == null || !secKey.equals(securityKey)) {
                getLog().warning(
                        "Received incoming stream with faulty security key.");
                return true;
            }

            UI ui = session.getUIById(Integer.parseInt(uiId));
            UI.setCurrent(ui);

            source = streamReceiver.getNode();

        } finally {
            session.unlock();
        }

        String contentType = request.getContentType();
        if (contentType.contains("boundary")) {
            // Multipart requests contain boundary string
            doHandleMultipartFileUpload(session, request, response,
                    streamReceiver, source);
        } else {
            // if boundary string does not exist, the posted file is from
            // XHR2.post(File)
            doHandleXhrFilePost(session, request, response, streamReceiver,
                    source, getContentLength(request));
        }
        return true;
    }

    /**
     * Generates URI string for a dynamic resource using its {@code id} and
     * {@code name}. [0] UIid, [1] cid, [2] name, [3] sec key
     *
     * @param id
     *            unique resource id
     * @return generated URI string
     */
    public static String generateURI(int nodeId, String name, String id) {
        StringBuilder builder = new StringBuilder(DYN_RES_PREFIX);

        try {
            builder.append(UI.getCurrent().getUIId()).append(PATH_SEPARATOR);
            builder.append(nodeId).append(PATH_SEPARATOR);
            builder.append(
                    URLEncoder.encode(name, StandardCharsets.UTF_8.name()))
                    .append(PATH_SEPARATOR);
            builder.append(id);
        } catch (UnsupportedEncodingException e) {
            // UTF8 has to be supported
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    private static Logger getLog() {
        return Logger.getLogger(StreamReceiverRequestHandler.class.getName());
    }

    protected void doHandleMultipartFileUpload(VaadinSession session,
            VaadinRequest request, VaadinResponse response,
            StreamReceiver streamReceiver, StateNode owner) throws IOException {

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload();

        long contentLength = getContentLength(request);
        // Parse the request
        FileItemIterator iter = null;
        try {
            iter = upload.getItemIterator((HttpServletRequest) request);
            while (iter.hasNext()) {
                FileItemStream item = iter.next();
                String name = item.getFieldName();
                InputStream stream = item.openStream();
                if (item.isFormField()) {
                    System.out.println("Form field " + name + " with value "
                            + Streams.asString(stream) + " detected.");
                } else {

                    try {
                        handleFileUploadValidationAndData(session, stream,
                                streamReceiver, name, item.getContentType(),
                                contentLength, owner);
                    } catch (UploadException e) {
                        session.getErrorHandler().error(new ErrorEvent(e));
                    }

                    System.out.println("File field " + name + " with file name "
                            + item.getName() + " detected.");
                    // Process the input stream
                }
            }
        } catch (FileUploadException e) {
            e.printStackTrace();
        }
        sendUploadResponse(request, response);
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
     *            The owner of the stream variable
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
        sendUploadResponse(request, response);
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

    /* Minimum interval which will be used for streaming progress events. */
    public static final int DEFAULT_STREAMING_PROGRESS_EVENT_INTERVAL_MS = 500;

    /**
     * To prevent event storming, streaming progress events are sent in this
     * interval rather than every time the buffer is filled. This fixes #13155.
     * To adjust this value override the method, and register your own handler
     * in VaadinService.createRequestHandlers(). The default is 500ms, and
     * setting it to 0 effectively restores the old behavior.
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
        } catch (IOException e1) {
            // NOP
        }
    }

    /**
     * Removes any possible path information from the filename and returns the
     * filename. Separators / and \\ are used.
     *
     * @param filename
     * @return
     */
    private static String removePath(String filename) {
        if (filename != null) {
            filename = filename.replaceAll("^.*[/\\\\]", "");
        }

        return filename;
    }

    /**
     * TODO document
     *
     * @param request
     * @param response
     * @throws IOException
     */
    protected void sendUploadResponse(VaadinRequest request,
            VaadinResponse response) throws IOException {
        response.setContentType(CONTENT_TYPE_TEXT_HTML_UTF_8);
        try (OutputStream out = response.getOutputStream()) {
            final PrintWriter outWriter = new PrintWriter(
                    new BufferedWriter(new OutputStreamWriter(out, "UTF-8")));
            outWriter.print("<html><body>download handled</body></html>");
            outWriter.flush();
        }
    }

    private void cleanStreamVariable(VaadinSession session,
            StreamReceiver streamReceiver) {
        session.accessSynchronously(() -> {
            session.getResourceRegistry().registerResource(streamReceiver)
                    .unregister();
        });
    }

    protected final boolean streamToReceiver(VaadinSession session,
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

            final byte buffer[] = new byte[MAX_UPLOAD_BUFFER_SIZE];
            long lastStreamingEvent = 0;
            int bytesReadToBuffer = 0;
            do {
                bytesReadToBuffer = in.read(buffer);
                if (bytesReadToBuffer > 0) {
                    out.write(buffer, 0, bytesReadToBuffer);
                    totalBytes += bytesReadToBuffer;
                }
                if (listenProgress) {
                    long now = System.currentTimeMillis();
                    // to avoid excessive session locking and event storms,
                    // events are sent in intervals, or at the end of the file.
                    if (lastStreamingEvent + getProgressEventInterval() <= now
                            || bytesReadToBuffer <= 0) {
                        lastStreamingEvent = now;
                        session.lock();
                        try {
                            StreamingProgressEventImpl progressEvent = new StreamingProgressEventImpl(
                                    filename, type, contentLength, totalBytes);
                            streamVariable.onProgress(progressEvent);
                        } finally {
                            session.unlock();
                        }
                    }
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
}
