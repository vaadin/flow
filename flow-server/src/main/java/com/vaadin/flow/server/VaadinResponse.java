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

package com.vaadin.flow.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import jakarta.servlet.http.Cookie;

import com.vaadin.flow.internal.CurrentInstance;

/**
 * A generic response from the server, wrapping a more specific response type,
 * e.g. HttpServletResponse.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface VaadinResponse {

    /**
     * Sets the (http) status code for the response. If you want to include an
     * error message along the status code, use {@link #sendError(int, String)}
     * instead.
     *
     * @param statusCode
     *            the status code to set
     * @see jakarta.servlet.http.HttpServletResponse#setStatus(int)
     *
     */
    void setStatus(int statusCode);

    /**
     * Sets the content type of this response. If the content type including a
     * charset is set before {@link #getWriter()} is invoked, the returned
     * PrintWriter will automatically use the defined charset.
     *
     * @param contentType
     *            a string specifying the MIME type of the content
     *
     * @see jakarta.servlet.ServletResponse#setContentType(String)
     */
    void setContentType(String contentType);

    /**
     * Sets the value of a generic response header. If the header had already
     * been set, the new value overwrites the previous one.
     *
     * @param name
     *            the name of the header
     * @param value
     *            the header value.
     *
     * @see jakarta.servlet.http.HttpServletResponse#setHeader(String, String)
     */
    void setHeader(String name, String value);

    /**
     * Properly formats a timestamp as a date header. If the header had already
     * been set, the new value overwrites the previous one.
     *
     * @param name
     *            the name of the header
     * @param timestamp
     *            the number of milliseconds since epoch
     *
     * @see jakarta.servlet.http.HttpServletResponse#setDateHeader(String, long)
     */
    void setDateHeader(String name, long timestamp);

    /**
     * Returns a <code>OutputStream</code> for writing binary data in the
     * response.
     * <p>
     * Either this method or getWriter() may be called to write the response,
     * not both.
     *
     * @return a <code>OutputStream</code> for writing binary data
     * @throws IOException
     *             if an input or output exception occurred
     *
     * @see #getWriter()
     * @see jakarta.servlet.ServletResponse#getOutputStream()
     */
    OutputStream getOutputStream() throws IOException;

    /**
     * Returns a <code>PrintWriter</code> object that can send character text to
     * the client. The PrintWriter uses the character encoding defined using
     * setContentType.
     * <p>
     * Either this method or getOutputStream() may be called to write the
     * response, not both.
     *
     * @return a <code>PrintWriter</code> for writing character text
     * @throws IOException
     *             if an input or output exception occurred
     *
     * @see #getOutputStream()
     * @see jakarta.servlet.ServletResponse#getWriter()
     */
    PrintWriter getWriter() throws IOException;

    /**
     * Sets cache time in milliseconds, -1 means no cache at all. All required
     * headers related to caching in the response are set based on the time.
     *
     * @param milliseconds
     *            Cache time in milliseconds
     */
    void setCacheTime(long milliseconds);

    /**
     * Sends an error response to the client using the specified status code and
     * clears the buffer. In some configurations, this can cause a predefined
     * error page to be displayed.
     *
     * @param errorCode
     *            the HTTP status code
     * @param message
     *            a message to accompany the error
     * @throws IOException
     *             if an input or output exception occurs
     *
     * @see jakarta.servlet.http.HttpServletResponse#sendError(int, String)
     */
    void sendError(int errorCode, String message) throws IOException;

    /**
     * Gets the vaadin service for the context of this response.
     *
     * @return the vaadin service
     *
     * @see VaadinService
     */
    VaadinService getService();

    /**
     * Adds the specified cookie to the response. This method can be called
     * multiple times to set more than one cookie.
     *
     * @param cookie
     *            the Cookie to return to the client
     *
     * @see jakarta.servlet.http.HttpServletResponse#addCookie(Cookie)
     */
    void addCookie(Cookie cookie);

    /**
     * Sets the length of the content body in the response In HTTP servlets,
     * this method sets the HTTP Content-Length header.
     *
     * @param len
     *            an integer specifying the length of the content being returned
     *            to the client
     */
    void setContentLength(int len);

    /**
     * Sets the length of the content body in the response In HTTP servlets,
     * this method sets the HTTP Content-Length header.
     *
     * @param len
     *            a long specifying the length of the content being returned to
     *            the client
     */
    void setContentLengthLong(long len);

    /**
     * Sets all conceivable headers that might prevent a response from being
     * stored in any caches.
     *
     */
    default void setNoCacheHeaders() {
        // no-store to disallow storing even if cache would be revalidated
        // must-revalidate to not use stored value even if someone asks for it
        setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        // Also set legacy values in case of old proxies in between
        setHeader("Pragma", "no-cache");
        setHeader("Expires", "0");
    }

    /**
     * Gets the currently processed Vaadin response. The current response is
     * automatically defined when the request is started. The current response
     * can not be used in e.g. background threads because of the way server
     * implementations reuse response instances.
     *
     * @return the current Vaadin response instance if available, otherwise
     *         <code>null</code>
     */
    static VaadinResponse getCurrent() {
        return CurrentInstance.get(VaadinResponse.class);
    }
}
