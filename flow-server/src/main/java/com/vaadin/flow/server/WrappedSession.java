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

import java.io.Serializable;
import java.util.Set;

/**
 * A generic session, wrapping a more specific session implementation, e.g.
 * {@link jakarta.servlet.http.HttpSession}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface WrappedSession extends Serializable {
    /**
     * Returns the maximum time interval, in seconds, that this session will be
     * kept open between client accesses.
     *
     * @return an integer specifying the number of seconds this session remains
     *         open between client requests
     *
     * @see jakarta.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    int getMaxInactiveInterval();

    /**
     * Gets an attribute from this session.
     *
     * @param name
     *            the name of the attribute
     * @return the attribute value, or <code>null</code> if the attribute is not
     *         defined in the session
     *
     * @see jakarta.servlet.http.HttpSession#getAttribute(String)
     */
    Object getAttribute(String name);

    /**
     * Saves an attribute value in this session.
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the attribute value
     *
     * @see jakarta.servlet.http.HttpSession#setAttribute(String, Object)
     */
    void setAttribute(String name, Object value);

    /**
     * Gets the current set of attribute names stored in this session.
     *
     * @return an unmodifiable set of the current attribute names
     *
     * @see jakarta.servlet.http.HttpSession#getAttributeNames()
     */
    Set<String> getAttributeNames();

    /**
     * Invalidates this session then unbinds any objects bound to it.
     *
     * @see jakarta.servlet.http.HttpSession#invalidate()
     */
    void invalidate();

    /**
     * Gets a string with a unique identifier for the session.
     *
     * @return a unique session id string
     *
     * @see jakarta.servlet.http.HttpSession#getId()
     */
    String getId();

    /**
     * Returns the time when this session was created, measured in milliseconds
     * since midnight January 1, 1970 GMT.
     *
     * @return a long specifying when this session was created, expressed in
     *         milliseconds since 1/1/1970 GMT
     *
     * @throws IllegalStateException
     *             if this method is called on an invalidated session
     * @see jakarta.servlet.http.HttpSession#getCreationTime()
     */
    long getCreationTime();

    /**
     * Returns the last time the client sent a request associated with this
     * session, as the number of milliseconds since midnight January 1, 1970
     * GMT, and marked by the time the container received the request.
     * <p>
     * Actions that your application takes, such as getting or setting a value
     * associated with the session, do not affect the access time.
     *
     * @return a long representing the last time the client sent a request
     *         associated with this session, expressed in milliseconds since
     *         1/1/1970 GMT
     *
     * @throws IllegalStateException
     *             if this method is called on an invalidated session
     *
     * @see jakarta.servlet.http.HttpSession#getLastAccessedTime()
     */
    long getLastAccessedTime();

    /**
     * Returns true if the client does not yet know about the session or if the
     * client chooses not to join the session. For example, if the server used
     * only cookie-based sessions, and the client had disabled the use of
     * cookies, then a session would be new on each request.
     *
     * @return true if the server has created a session, but the client has not
     *         yet joined
     * @throws IllegalStateException
     *             if this method is called on an invalidated session
     * @see jakarta.servlet.http.HttpSession#isNew()
     */
    boolean isNew();

    /**
     * Removes the object bound with the specified name from this session. If
     * the session does not have an object bound with the specified name, this
     * method does nothing.
     *
     * @param name
     *            the name of the object to remove from this session
     * @throws IllegalStateException
     *             if this method is called on an invalidated session
     * @see jakarta.servlet.http.HttpSession#removeAttribute(String)
     */
    void removeAttribute(String name);

    /**
     * Specifies the time, in seconds, between client requests before the
     * servlet container will invalidate this session. A negative time indicates
     * the session should never timeout.
     *
     * @param interval
     *            An integer specifying the number of seconds
     * @see jakarta.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    void setMaxInactiveInterval(int interval);

}
