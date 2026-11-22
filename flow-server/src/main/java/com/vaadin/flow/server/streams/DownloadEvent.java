/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.streams;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.EncodeUtil;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.StreamRequestHandler;

/**
 * Class containing meta-data for handling the requested client download.
 * <p>
 * It gives direct access to the underlying request, response and session as
 * well as various helpers specifically for handling downloads.
 *
 *
 * @since 24.8
 */
public class DownloadEvent {

    private static final String ATTACHMENT = "attachment";
    private static final String INLINE = "inline";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    private final VaadinRequest request;
    private final VaadinResponse response;
    private final VaadinSession session;
    private final Element owningElement;
    private String fileName;
    // inline or attachment as asked for by the handler, null if not asked
    private String requestedType;
    private String defaultType = ATTACHMENT;
    private String writtenContentDisposition;
    private String contentType;
    private long contentLength = -1;
    private Exception exception;
    private final DownloadDisposition disposition;

    public DownloadEvent(VaadinRequest request, VaadinResponse response,
            VaadinSession session, Element owningElement) {
        this.request = request;
        this.response = response;
        this.session = session;
        this.owningElement = owningElement;
        disposition = request == null ? null
                : (DownloadDisposition) request.getAttribute(
                        StreamRequestHandler.DISPOSITION_REQUEST_ATTRIBUTE);
    }

    /**
     * Returns a <code>OutputStream</code> for writing binary data in the
     * response.
     * <p>
     * Either this method or getWriter() may be called to write the response,
     * not both.
     *
     * @return a <code>OutputStream</code> for writing binary data
     * @throws UncheckedIOException
     *             if an I/O error occurred while getting the output stream
     */
    public OutputStream getOutputStream() {
        try {
            return response.getOutputStream();
        } catch (IOException e) {
            LoggerFactory.getLogger(DownloadEvent.class)
                    .error("Error getting output stream", e);
            throw new UncheckedIOException("Error getting output stream", e);
        }
    }

    /**
     * Returns a <code>PrintWriter</code> object that can send character text to
     * the client. The PrintWriter uses the character encoding defined using
     * setContentType.
     * <p>
     * Either this method or getOutputStream() may be called to write the
     * response, not both.
     *
     * @return a <code>PrintWriter</code> for writing character text
     * @throws UncheckedIOException
     *             if an I/O error occurred while getting the writer
     */
    public PrintWriter getWriter() {
        try {
            return response.getWriter();
        } catch (IOException e) {
            LoggerFactory.getLogger(DownloadEvent.class)
                    .error("Error getting print writer");
            throw new UncheckedIOException("Error getting writer", e);
        }
    }

    /**
     * Get {@link VaadinRequest} for download event.
     *
     * @return vaadin request
     */
    public VaadinRequest getRequest() {
        return request;
    }

    /**
     * Get {@link VaadinResponse} for download event.
     *
     * @return vaadin response
     */
    public VaadinResponse getResponse() {
        return response;
    }

    /**
     * Get {@link VaadinSession} for download event.
     *
     * @return vaadin session
     */
    public VaadinSession getSession() {
        return session;
    }

    /**
     * Sets the name of the file to be downloaded and serves it as an
     * attachment. This method utilizes the HTTP Content-Disposition header to
     * specify the name of the file to be downloaded.
     * <p>
     * To be called before the response is committed.
     * <p>
     * If the <code>fileName</code> is <code>null</code>, the
     * Content-Disposition header won't be set. Control characters, such as line
     * breaks, are removed from the file name.
     * <p>
     * If the Content-Disposition header was set directly on the response before
     * any of these methods wrote it, this method will not override it.
     * <p>
     * The header is always {@code attachment}, even when the place where the
     * handler is used asks for the content to be shown inline, for example an
     * {@code IFrame} or a link opened in a new tab.
     *
     * @param fileName
     *            the name to be assigned to the file
     * @deprecated use {@link #attachment(String)}, which behaves the same, or
     *             {@link #fileName(String)} to let the place where the handler
     *             is used decide whether the content is saved or shown
     */
    @Deprecated(since = "25.4", forRemoval = true)
    public void setFileName(String fileName) {
        if (fileName != null) {
            attachment(fileName);
        }
    }

    /**
     * Sets the name of the file, which the browser uses when the content is
     * saved. This does not change whether the content is saved or shown: that
     * is decided by {@link #inline()} or {@link #attachment(String)} if the
     * handler calls them, before or after this method, and otherwise by the
     * place where the handler is used, through the {@link DownloadDisposition}
     * of the resource the handler is registered with. For example, an
     * {@code Image} or a link opened in a new tab shows the content. Without
     * any of these, the content is saved as an attachment.
     * <p>
     * This method utilizes the HTTP Content-Disposition header. To be called
     * before the response is committed.
     * <p>
     * If the <code>fileName</code> is <code>null</code> or blank, this method
     * does nothing. Control characters, such as line breaks, are removed from
     * the file name.
     * <p>
     * If the Content-Disposition header was set directly on the response before
     * any of these methods wrote it, this method will not override it.
     *
     * @param fileName
     *            the name of the file
     * @see #attachment(String)
     * @see #inline(String)
     * @since 25.4
     */
    public void fileName(String fileName) {
        if (setName(fileName)) {
            writeContentDisposition();
        }
    }

    /**
     * Gives the file name of a built-in handler, which shows its content by
     * default when {@code inlineByDefault} is {@code true}. A name given
     * earlier, for example by the handler's callback, is kept.
     */
    void fileName(String fileName, boolean inlineByDefault) {
        defaultType = inlineByDefault ? INLINE : ATTACHMENT;
        if (this.fileName == null) {
            setName(fileName);
        }
        writeContentDisposition();
    }

    /**
     * Sets the Content-Disposition header to attachment with a filename, so
     * that the browser saves the content as a file instead of showing it.
     * <p>
     * The header is {@code attachment} even when the place where the handler is
     * used asks for the content to be shown inline. Use
     * {@link #fileName(String)} to let that place decide.
     * <p>
     * To be called before the response is committed.
     * <p>
     * If the <code>fileName</code> is <code>null</code> or blank, the file name
     * given earlier, if any, is kept. Control characters, such as line breaks,
     * are removed from the file name.
     * <p>
     * If the Content-Disposition header was set directly on the response before
     * any of these methods wrote it, this method will not override it.
     *
     * @param fileName
     *            the name of the saved file
     * @since 25.4
     */
    public void attachment(String fileName) {
        requestedType = ATTACHMENT;
        setName(fileName);
        writeContentDisposition();
    }

    /**
     * Sets the Content-Disposition header to inline, allowing the content to be
     * displayed directly in the browser instead of being downloaded. A file
     * name given with {@link #fileName(String)} is kept.
     * <p>
     * The header is {@code inline} even when the place where the handler is
     * used asks for the content to be saved as a file. Use
     * {@link #fileName(String)} alone to let that place decide.
     * <p>
     * To be called before the response is committed.
     * <p>
     * If the Content-Disposition header was set directly on the response before
     * any of these methods wrote it, this method will not override it.
     *
     * @since 25.0
     */
    public void inline() {
        requestedType = INLINE;
        writeContentDisposition();
    }

    /**
     * Sets the Content-Disposition header to inline with a filename, allowing
     * the content to be displayed directly in the browser with a suggested
     * filename if the user chooses to save it.
     * <p>
     * The header is {@code inline} even when the place where the handler is
     * used asks for the content to be saved as a file. Use
     * {@link #fileName(String)} alone to let that place decide.
     * <p>
     * To be called before the response is committed.
     * <p>
     * If the <code>fileName</code> is <code>null</code> or blank, this behaves
     * the same as calling {@link #inline()}. Control characters, such as line
     * breaks, are removed from the file name.
     * <p>
     * If the Content-Disposition header was set directly on the response before
     * any of these methods wrote it, this method will not override it.
     *
     * @param fileName
     *            the suggested name for the file if saved by the user
     * @since 25.0
     */
    public void inline(String fileName) {
        requestedType = INLINE;
        setName(fileName);
        writeContentDisposition();
    }

    /**
     * Stores the cleaned file name unless it is null or blank.
     *
     * @return whether the name was stored
     */
    private boolean setName(String fileName) {
        if (fileName == null) {
            return false;
        }
        String cleaned = EncodeUtil.removeControlCharacters(fileName);
        if (cleaned.isBlank()) {
            return false;
        }
        this.fileName = cleaned;
        return true;
    }

    private void writeContentDisposition() {
        if (writtenContentDisposition == null
                && response.containsHeader(CONTENT_DISPOSITION)) {
            // set directly on the response
            return;
        }
        String type = requestedType;
        if (type == null) {
            if (fileName == null) {
                return;
            }
            type = disposition != null ? disposition.getHeaderValue()
                    : defaultType;
        }
        String value = buildContentDisposition(type, fileName);
        if (!value.equals(writtenContentDisposition)) {
            response.setHeader(CONTENT_DISPOSITION, value);
            writtenContentDisposition = value;
        }
    }

    private static String buildContentDisposition(String type,
            String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return type;
        }
        StringBuilder value = new StringBuilder();
        value.append(type).append("; ");
        if (EncodeUtil.isPureUSASCII(fileName)) {
            value.append("filename=\"")
                    .append(EncodeUtil.quotedStringEscape(fileName))
                    .append("\"");
        } else {
            value
                    // fallback legacy support
                    .append("filename=\"")
                    .append(EncodeUtil.rfc2047Encode(fileName))
                    // used primarily
                    .append("\"; filename*=UTF-8''")
                    .append(EncodeUtil.rfc5987Encode(fileName));
        }
        return value.toString();
    }

    /**
     * Sets the content type for the current download. These methods utilize the
     * HTTP Content-Type header to specify the type of content being sent to the
     * client.
     * <p>
     * To be called before the response is committed.
     *
     * @param contentType
     *            the MIME type to set as the content type
     */
    public void setContentType(String contentType) {
        response.setContentType(contentType);
        this.contentType = contentType;
    }

    /**
     * Sets the length of the content body in the response if the length is not
     * <code>-1</code>. This method utilizes the HTTP Content-Length header to
     * specify the length of the content being sent to the client.
     * <p>
     * To be called before the response is committed.
     *
     * @param contentLength
     *            the length of the response content in bytes
     */
    public void setContentLength(long contentLength) {
        if (contentLength != -1) {
            response.setContentLengthLong(contentLength);
        }
        this.contentLength = contentLength;
    }

    /**
     * Get owner {@link Component} for this event.
     * <p>
     * The download handler may change the component's state during download,
     * e.g. disable or hide it during download or get the component's own data
     * like id.
     *
     * @return owning component or null in none defined
     */
    public Component getOwningComponent() {
        return owningElement.getComponent().orElse(null);
    }

    /**
     * Get the owning element for the download related to this event.
     * <p>
     * The download handler may use element's attributes or properties to define
     * what to download or change the element, e.g. element's id or data id to
     * fetch a row from a database or disable element once the download is
     * started.
     *
     * @return owning element
     */
    public Element getOwningElement() {
        return owningElement;
    }

    /**
     * Get the current UI instance for this request that can be used to make
     * asynchronous UI updates with
     * {@link UI#access(com.vaadin.flow.server.Command)}.
     *
     * @return Current UI instance
     */
    public UI getUI() {
        Optional<Component> component = owningElement.getComponent();
        return component.map(value -> value.getUI().orElseGet(UI::getCurrent))
                .orElseGet(UI::getCurrent);
    }

    String getFileName() {
        return fileName;
    }

    String getContentType() {
        return contentType;
    }

    long getContentLength() {
        return contentLength;
    }

    Exception getException() {
        return exception;
    }

    void setException(Exception exception) {
        this.exception = exception;
    }
}
