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

import jakarta.servlet.http.Cookie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import com.vaadin.flow.internal.CurrentInstance;

/**
 * A generic request to the server, wrapping a more specific request type, e.g.
 * HttpServletRequest.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface VaadinRequest {
    /**
     * Gets the named request parameter This is typically a HTTP GET or POST
     * parameter, though other request types might have other ways of
     * representing parameters.
     *
     * @see jakarta.servlet.ServletRequest#getParameter(String)
     *
     * @param parameter
     *            the name of the parameter
     * @return The parameter value, or <code>null</code> if no parameter with
     *         the given name is present
     */
    String getParameter(String parameter);

    /**
     * Gets all the parameters of the request.
     *
     * @see #getParameter(String)
     *
     * @see jakarta.servlet.ServletRequest#getParameterMap()
     *
     * @return A mapping of parameter names to arrays of parameter values
     */
    Map<String, String[]> getParameterMap();

    /**
     * Returns the length of the request content that can be read from the input
     * stream returned by {@link #getInputStream()}.
     *
     * @see jakarta.servlet.ServletRequest#getContentLength()
     *
     * @return content length in bytes
     */
    int getContentLength();

    /**
     * Returns the length of the request content that can be read from the input
     * stream returned by {@link #getInputStream()}.
     *
     * @see jakarta.servlet.ServletRequest#getContentLengthLong()
     *
     * @return a long containing the length of the request body or -1L if the
     *         length is not known
     */
    default long getContentLengthLong() {
        return getContentLength();
    }

    /**
     * Returns an input stream from which the request content can be read. The
     * request content length can be obtained with {@link #getContentLength()}
     * without reading the full stream contents.
     *
     * @see jakarta.servlet.ServletRequest#getInputStream()
     *
     * @return the input stream from which the contents of the request can be
     *         read
     * @throws IOException
     *             if the input stream can not be opened
     */
    InputStream getInputStream() throws IOException;

    /**
     * Gets a request attribute.
     *
     * @param name
     *            the name of the attribute
     * @return the value of the attribute, or <code>null</code> if there is no
     *         attribute with the given name
     *
     * @see jakarta.servlet.ServletRequest#getAttribute(String)
     */
    Object getAttribute(String name);

    /**
     * Defines a request attribute.
     *
     * @param name
     *            the name of the attribute
     * @param value
     *            the attribute value
     *
     * @see jakarta.servlet.ServletRequest#setAttribute(String, Object)
     */
    void setAttribute(String name, Object value);

    /**
     * Gets the path of the requested resource relative to the application. The
     * path is <code>null</code> if no path information is available. Does
     * always start with / if the path isn't <code>null</code>.
     *
     * @return a string with the path relative to the application.
     *
     * @see jakarta.servlet.http.HttpServletRequest#getPathInfo()
     */
    String getPathInfo();

    /**
     * Returns the portion of the request URI that indicates the context of the
     * request. The context path always comes first in a request URI.
     *
     * @see jakarta.servlet.http.HttpServletRequest#getContextPath()
     *
     * @return a String specifying the portion of the request URI that indicates
     *         the context of the request
     */
    String getContextPath();

    /**
     * Gets the session associated with this request, creating a new if there is
     * no session.
     *
     * @see WrappedSession
     * @see jakarta.servlet.http.HttpServletRequest#getSession()
     *
     * @return the wrapped session for this request
     */
    WrappedSession getWrappedSession();

    /**
     * Gets the session associated with this request, optionally creating a new
     * if there is no session.
     *
     * @param allowSessionCreation
     *            <code>true</code> to create a new session for this request if
     *            necessary; <code>false</code> to return <code>null</code> if
     *            there's no current session
     *
     * @see WrappedSession
     * @see jakarta.servlet.http.HttpServletRequest#getSession(boolean)
     *
     * @return the wrapped session for this request
     */
    WrappedSession getWrappedSession(boolean allowSessionCreation);

    /**
     * Returns the MIME type of the body of the request, or null if the type is
     * not known.
     *
     * @return a string containing the name of the MIME type of the request, or
     *         null if the type is not known
     *
     * @see jakarta.servlet.ServletRequest#getContentType()
     *
     */
    String getContentType();

    /**
     * Gets locale information from the query, e.g. using the Accept-Language
     * header.
     *
     * @return the preferred Locale
     *
     * @see jakarta.servlet.ServletRequest#getLocale()
     */
    Locale getLocale();

    /**
     * Returns the IP address from which the request came. This might also be
     * the address of a proxy between the server and the original requester.
     *
     * @return a string containing the IP address, or <code>null</code> if the
     *         address is not available
     *
     * @see jakarta.servlet.ServletRequest#getRemoteAddr()
     */
    String getRemoteAddr();

    /**
     * Checks whether the request was made using a secure channel, e.g. using
     * https.
     *
     * @return a boolean indicating if the request is secure
     *
     * @see jakarta.servlet.ServletRequest#isSecure()
     */
    boolean isSecure();

    /**
     * Gets the value of a request header, e.g. a http header for a
     * {@link jakarta.servlet.http.HttpServletRequest}.
     *
     * @param headerName
     *            the name of the header
     * @return the header value, or <code>null</code> if the header is not
     *         present in the request
     *
     * @see jakarta.servlet.http.HttpServletRequest#getHeader(String)
     */
    String getHeader(String headerName);

    /**
     * Gets the vaadin service for the context of this request.
     *
     * @return the vaadin service
     *
     * @see VaadinService
     */
    VaadinService getService();

    /**
     * Returns an array containing all of the <code>Cookie</code> objects the
     * client sent with this request. This method returns <code>null</code> if
     * no cookies were sent.
     *
     * @return an array of all the <code>Cookies</code> included with this
     *         request, or <code>null</code> if the request has no cookies
     *
     * @see jakarta.servlet.http.HttpServletRequest#getCookies()
     */
    Cookie[] getCookies();

    /**
     * Returns the name of the authentication scheme used for the connection
     * between client and server, for example, <code>BASIC_AUTH</code>,
     * <code>CLIENT_CERT_AUTH</code>, a custom one or <code>null</code> if there
     * was no authentication.
     *
     * @return a string indicating the authentication scheme, or
     *         <code>null</code> if the request was not authenticated.
     *
     * @see jakarta.servlet.http.HttpServletRequest#getAuthType()
     */
    String getAuthType();

    /**
     * Returns the login of the user making this request, if the user has been
     * authenticated, or null if the user has not been authenticated. Whether
     * the user name is sent with each subsequent request depends on the browser
     * and type of authentication.
     *
     * @return a String specifying the login of the user making this request, or
     *         <code>null</code> if the user login is not known.
     *
     * @see jakarta.servlet.http.HttpServletRequest#getRemoteUser()
     */
    String getRemoteUser();

    /**
     * Returns a <code>java.security.Principal</code> object containing the name
     * of the current authenticated user. If the user has not been
     * authenticated, the method returns <code>null</code>.
     *
     * @return a <code>java.security.Principal</code> containing the name of the
     *         user making this request; <code>null</code> if the user has not
     *         been authenticated
     *
     * @see jakarta.servlet.http.HttpServletRequest#getUserPrincipal()
     */
    Principal getUserPrincipal();

    /**
     * Returns a boolean indicating whether the authenticated user is included
     * in the specified logical "role". Roles and role membership can be defined
     * using deployment descriptors. If the user has not been authenticated, the
     * method returns <code>false</code>.
     *
     * @param role
     *            a String specifying the name of the role
     * @return a boolean indicating whether the user making this request belongs
     *         to a given role; <code>false</code> if the user has not been
     *         authenticated
     *
     * @see jakarta.servlet.http.HttpServletRequest#isUserInRole(String)
     */
    boolean isUserInRole(String role);

    /**
     * Removes an attribute from this request. This method is not generally
     * needed as attributes only persist as long as the request is being
     * handled.
     *
     * @param name
     *            a String specifying the name of the attribute to remove
     *
     * @see jakarta.servlet.ServletRequest#removeAttribute(String)
     */
    void removeAttribute(String name);

    /**
     * Returns an Enumeration containing the names of the attributes available
     * to this request. This method returns an empty Enumeration if the request
     * has no attributes available to it.
     *
     * @return an Enumeration of strings containing the names of the request's
     *         attributes
     *
     * @see jakarta.servlet.ServletRequest#getAttributeNames()
     */
    Enumeration<String> getAttributeNames();

    /**
     * Returns an Enumeration of Locale objects indicating, in decreasing order
     * starting with the preferred locale, the locales that are acceptable to
     * the client based on the Accept-Language header. If the client request
     * doesn't provide an Accept-Language header, this method returns an
     * Enumeration containing one Locale, the default locale for the server.
     *
     * @return an Enumeration of preferred Locale objects for the client
     *
     * @see jakarta.servlet.http.HttpServletRequest#getLocales()
     */
    Enumeration<Locale> getLocales();

    /**
     * Returns the fully qualified name of the client or the last proxy that
     * sent the request. If the engine cannot or chooses not to resolve the
     * hostname (to improve performance), this method returns the dotted-string
     * form of the IP address.
     *
     * @return a String containing the fully qualified name of the client, or
     *         <code>null</code> if the information is not available.
     *
     * @see jakarta.servlet.http.HttpServletRequest#getRemoteHost()
     */
    String getRemoteHost();

    /**
     * Returns the Internet Protocol (IP) source port of the client or last
     * proxy that sent the request.
     *
     * @return an integer specifying the port number, or -1 if the information
     *         is not available.
     *
     * @see jakarta.servlet.ServletRequest#getRemotePort()
     */
    int getRemotePort();

    /**
     * Returns the name of the character encoding used in the body of this
     * request. This method returns <code>null</code> if the request does not
     * specify a character encoding.
     *
     * @return a String containing the name of the character encoding, or null
     *         if the request does not specify a character encoding
     *
     * @see jakarta.servlet.ServletRequest#getCharacterEncoding()
     */
    String getCharacterEncoding();

    /**
     * Retrieves the body of the request as character data using a
     * <code>BufferedReader</code>. The reader translates the character data
     * according to the character encoding used on the body. Either this method
     * or {@link #getInputStream()} may be called to read the body, not both.
     *
     * @return a BufferedReader containing the body of the request
     *
     * @throws UnsupportedEncodingException
     *             - if the character set encoding used is not supported and the
     *             text cannot be decoded
     * @throws IllegalStateException
     *             - if {@link #getInputStream()} method has been called on this
     *             request
     * @throws IOException
     *             if an input or output exception occurred
     *
     * @see jakarta.servlet.ServletRequest#getReader()
     */
    BufferedReader getReader() throws IOException;

    /**
     * Returns the name of the HTTP method with which this request was made, for
     * example, GET, POST, or PUT.
     *
     * @return a String specifying the name of the method with which this
     *         request was made
     *
     * @see jakarta.servlet.http.HttpServletRequest#getMethod()
     */
    String getMethod();

    /**
     * Returns the value of the specified request header as a long value that
     * represents a Date object. Use this method with headers that contain
     * dates, such as If-Modified-Since.
     * <p>
     * The date is returned as the number of milliseconds since January 1, 1970
     * GMT. The header name is case insensitive.
     * <p>
     * If the request did not have a header of the specified name, this method
     * returns -1. If the header can't be converted to a date, the method throws
     * an IllegalArgumentException.
     *
     * @param name
     *            a String specifying the name of the header
     * @return a long value representing the date specified in the header
     *         expressed as the number of milliseconds since January 1, 1970
     *         GMT, or -1 if the named header was not included with the request
     * @throws IllegalArgumentException
     *             If the header value can't be converted to a date
     * @see jakarta.servlet.http.HttpServletRequest#getDateHeader(String)
     */
    long getDateHeader(String name);

    /**
     * Returns an enumeration of all the header names this request contains. If
     * the request has no headers, this method returns an empty enumeration.
     * <p>
     * Some implementations do not allow access headers using this method, in
     * which case this method returns <code>null</code>
     *
     * @return an enumeration of all the header names sent with this request; if
     *         the request has no headers, an empty enumeration; if the
     *         implementation does not allow this method, <code>null</code>
     * @see jakarta.servlet.http.HttpServletRequest#getHeaderNames()
     */
    Enumeration<String> getHeaderNames();

    /**
     * Returns all the values of the specified request header as an Enumeration
     * of String objects.
     * <p>
     * Some headers, such as <code>Accept-Language</code> can be sent by clients
     * as several headers each with a different value rather than sending the
     * header as a comma separated list.
     * <p>
     * If the request did not include any headers of the specified name, this
     * method returns an empty Enumeration. If the request does not support
     * accessing headers, this method returns <code>null</code>.
     * <p>
     * The header name is case insensitive. You can use this method with any
     * request header.
     *
     *
     * @param name
     *            a String specifying the header name
     * @return an Enumeration containing the values of the requested header. If
     *         the request does not have any headers of that name return an
     *         empty enumeration. If the header information is not available,
     *         return <code>null</code>
     * @see jakarta.servlet.http.HttpServletRequest#getHeaders(String)
     */
    Enumeration<String> getHeaders(String name);

    /**
     * Gets the currently processed Vaadin request. The current request is
     * automatically defined when the request is started. The current request
     * can not be used in e.g. background threads because of the way server
     * implementations reuse request instances.
     *
     * @return the current Vaadin request instance if available, otherwise
     *         <code>null</code>
     */
    static VaadinRequest getCurrent() {
        return CurrentInstance.get(VaadinRequest.class);
    }
}
