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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class MockServletContext implements ServletContext {

    HashMap<String, Object> sessionAttributes = new HashMap<>();

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getContext(java.lang.String)
     */
    @Override
    public ServletContext getContext(String uripath) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getMajorVersion()
     */
    @Override
    public int getMajorVersion() {
        return 3;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getMinorVersion()
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getMimeType(java.lang.String)
     */
    @Override
    public String getMimeType(String file) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Set getResourcePaths(String path) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String path) throws MalformedURLException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String path) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jakarta.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */
    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#log(java.lang.String)
     */
    @Override
    public void log(String msg) {
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#log(java.lang.String,
     * java.lang.Throwable)
     */
    @Override
    public void log(String message, Throwable throwable) {
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getRealPath(java.lang.String)
     */
    @Override
    public String getRealPath(String path) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getServerInfo()
     */
    @Override
    public String getServerInfo() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getInitParameter(java.lang.String)
     */
    @Override
    public String getInitParameter(String name) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getInitParameterNames()
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getInitParameterNames() {
        return Collections.enumeration(Collections.EMPTY_LIST);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getAttribute(java.lang.String)
     */
    @Override
    public Object getAttribute(String name) {
        return sessionAttributes.get(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getAttributeNames()
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration getAttributeNames() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#setAttribute(java.lang.String,
     * java.lang.Object)
     */
    @Override
    public void setAttribute(String name, Object object) {
        sessionAttributes.put(name, object);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#removeAttribute(java.lang.String)
     */
    @Override
    public void removeAttribute(String name) {
        sessionAttributes.remove(name);
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getServletContextName()
     */
    @Override
    public String getServletContextName() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getContextPath()
     */
    @Override
    public String getContextPath() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getEffectiveMajorVersion()
     */
    @Override
    public int getEffectiveMajorVersion() {
        return 3;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getEffectiveMinorVersion()
     */
    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#setInitParameter(java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean setInitParameter(String name, String value) {
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addServlet(java.lang.String,
     * java.lang.String)
     */
    @Override
    public Dynamic addServlet(String servletName, String className) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addServlet(java.lang.String,
     * jakarta.servlet.Servlet)
     */
    @Override
    public Dynamic addServlet(String servletName, Servlet servlet) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addServlet(java.lang.String,
     * java.lang.Class)
     */
    @Override
    public Dynamic addServlet(String servletName,
            Class<? extends Servlet> servletClass) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#createServlet(java.lang.Class)
     */
    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz)
            throws ServletException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jakarta.servlet.ServletContext#getServletRegistration(java.lang.String)
     */
    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getServletRegistrations()
     */
    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addFilter(java.lang.String,
     * java.lang.String)
     */
    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(
            String filterName, String className) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addFilter(java.lang.String,
     * jakarta.servlet.Filter)
     */
    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(
            String filterName, Filter filter) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addFilter(java.lang.String,
     * java.lang.Class)
     */
    @Override
    public jakarta.servlet.FilterRegistration.Dynamic addFilter(
            String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#createFilter(java.lang.Class)
     */
    @Override
    public <T extends Filter> T createFilter(Class<T> clazz)
            throws ServletException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jakarta.servlet.ServletContext#getFilterRegistration(java.lang.String)
     */
    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getFilterRegistrations()
     */
    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getSessionCookieConfig()
     */
    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * jakarta.servlet.ServletContext#setSessionTrackingModes(java.util.Set)
     */
    @Override
    public void setSessionTrackingModes(
            Set<SessionTrackingMode> sessionTrackingModes) {
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getDefaultSessionTrackingModes()
     */
    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getEffectiveSessionTrackingModes()
     */
    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addListener(java.lang.String)
     */
    @Override
    public void addListener(String className) {
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addListener(java.util.EventListener)
     */
    @Override
    public <T extends EventListener> void addListener(T t) {
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#addListener(java.lang.Class)
     */
    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#createListener(java.lang.Class)
     */
    @Override
    public <T extends EventListener> T createListener(Class<T> clazz)
            throws ServletException {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getJspConfigDescriptor()
     */
    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#getClassLoader()
     */
    @Override
    public ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }

    /*
     * (non-Javadoc)
     *
     * @see jakarta.servlet.ServletContext#declareRoles(java.lang.String[])
     */
    @Override
    public void declareRoles(String... roleNames) {
    }

    @Override
    public String getVirtualServerName() {
        return null;
    }

    @Override
    public Dynamic addJspFile(String servletName, String jspFile) {
        return null;
    }

    @Override
    public int getSessionTimeout() {
        return 0;
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) {
    }

    @Override
    public String getRequestCharacterEncoding() {
        return null;
    }

    @Override
    public void setRequestCharacterEncoding(String encoding) {
    }

    @Override
    public String getResponseCharacterEncoding() {
        return null;
    }

    @Override
    public void setResponseCharacterEncoding(String encoding) {
    }

}
