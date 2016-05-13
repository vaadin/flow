/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.bootstrap.ErrorMessage;

/**
 * Class containing the configuration for an application.
 * <p>
 * This class is effectively immutable although setters exist to assign the
 * values during construction.
 *
 * @author Vaadin Ltd
 */
public class ApplicationConfiguration {

    private String applicationId;
    private String contextRootUrl;
    private String serviceUrl;
    private int uiId;
    private ErrorMessage communicationError;
    private ErrorMessage authorizationError;
    private ErrorMessage sessionExpiredError;
    private int heartbeatInterval;

    private boolean widgetsetVersionSent = false;

    private JavaScriptObject versionInfo;
    private boolean debugMode = false;
    private String servletVersion;
    private String atmosphereVersion;
    private String atmosphereJSVersion;

    /**
     * Gets the id generated for the application.
     *
     * @return the id for the application
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets the id generated for the application.
     *
     * @param applicationId
     *            the id for the application
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets the URL to the server-side VaadinService.
     *
     * @return the URL to the server-side service as a string
     */
    public String getServiceUrl() {
        return serviceUrl;
    }

    /**
     * Sets the URL to the server-side VaadinService.
     *
     * @param serviceUrl
     *            the URL to the server-side service as a string
     */
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    /**
     * Gets the URL of the context root on the server.
     *
     * @return the URL of the context root, ending with a "/"
     */
    public String getContextRootUrl() {
        return contextRootUrl;
    }

    /**
     * Sets the URL of the context root on the server.
     *
     * @param contextRootUrl
     *            the URL of the context root, ending with a "/"
     */
    public void setContextRootUrl(String contextRootUrl) {
        assert contextRootUrl.endsWith("/");
        this.contextRootUrl = contextRootUrl;
    }

    /**
     * Gets the UI id of the server-side UI associated with this client-side
     * instance. The UI id should be included in every request originating from
     * this instance in order to associate the request with the right UI
     * instance on the server.
     *
     * @return the UI id
     */
    public int getUIId() {
        return uiId;
    }

    /**
     * Sets the UI id of the server-side UI associated with this client-side
     * instance.
     *
     * @param uiId
     *            the UI id
     */
    public void setUIId(int uiId) {
        this.uiId = uiId;
    }

    /**
     * Gets the interval for heartbeat requests.
     *
     * @return The interval in seconds between heartbeat requests, or -1 if
     *         heartbeat is disabled.
     */
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * Sets the interval for heartbeat requests.
     *
     * @param heartbeatInterval
     *            The interval in seconds between heartbeat requests, or -1 if
     *            heartbeat is disabled.
     */
    public void setHeartbeatInterval(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    /**
     * Gets the message used when a communication error occurs.
     *
     * @return the communication error message
     */
    public ErrorMessage getCommunicationError() {
        return communicationError;
    }

    /**
     * Sets the message used when a communication error occurs.
     *
     * @param communicationError
     *            the communication error message
     */
    public void setCommunicationError(ErrorMessage communicationError) {
        this.communicationError = communicationError;
    }

    /**
     * Gets the message used when an authorization error occurs.
     *
     * @return the authorization error message
     */
    public ErrorMessage getAuthorizationError() {
        return authorizationError;
    }

    /**
     * Sets the message used when an authorization error occurs.
     *
     * @param authorizationError
     *            the authorization error message
     */
    public void setAuthorizationError(ErrorMessage authorizationError) {
        this.authorizationError = authorizationError;
    }

    /**
     * Gets the message used when a session expiration error occurs.
     *
     * @return the session expiration error message
     */
    public ErrorMessage getSessionExpiredError() {
        return sessionExpiredError;
    }

    /**
     * Sets the message used when a session expiration error occurs.
     *
     * @param sessionExpiredError
     *            the session expiration error message
     */
    public void setSessionExpiredError(ErrorMessage sessionExpiredError) {
        this.sessionExpiredError = sessionExpiredError;
    }

    /**
     * Gets the Vaadin servlet version in use.
     *
     * @return the Vaadin servlet version in use
     */
    public String getServletVersion() {
        return servletVersion;
    }

    /**
     * Sets the Vaadin servlet version in use.
     *
     * @param servletVersion
     *            the Vaadin servlet version in use
     */
    public void setServletVersion(String servletVersion) {
        this.servletVersion = servletVersion;
    }

    /**
     * Gets the Atmosphere runtime version in use.
     *
     * @return the Atmosphere runtime version in use
     */
    public String getAtmosphereVersion() {
        return atmosphereVersion;
    }

    /**
     * Sets the Atmosphere runtime version in use.
     *
     * @param atmosphereVersion
     *            the Atmosphere runtime version in use
     */
    public void setAtmosphereVersion(String atmosphereVersion) {
        this.atmosphereVersion = atmosphereVersion;
    }

    /**
     * Gets the Atmosphere JavaScript version in use.
     *
     * @return the Atmosphere JavaScript version in use
     */
    public String getAtmosphereJSVersion() {
        return atmosphereJSVersion;
    }

    /**
     * Sets the Atmosphere JavaScript version in use.
     *
     * @param atmosphereJSVersion
     *            the Atmosphere JavaScript version in use
     */
    public void setAtmosphereJSVersion(String atmosphereJSVersion) {
        this.atmosphereJSVersion = atmosphereJSVersion;
    }

    /**
     * Checks if we are running in debug mode.
     * <p>
     * With debug mode enabled, a lot more information is logged to the browser
     * console. In production you should always disable debug mode, because
     * logging and other debug features can have a significant performance
     * impact.
     *
     * @return {@code true} if debug mode is enabled, {@code false} otherwise
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Sets whether we are running in debug mode.
     * <p>
     * With debug mode enabled, a lot more information is logged to the browser
     * console. In production you should always disable debug mode, because
     * logging and other debug features can have a significant performance
     * impact.
     *
     * @param debugMode
     *            {@code true} if debug mode is enabled, {@code false} otherwise
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * Checks whether the widget set version has been sent to the server. It is
     * sent in the first UIDL request.
     *
     * @return <code>true</code> if browser information has already been sent
     */
    public boolean isWidgetsetVersionSent() {
        return widgetsetVersionSent;
    }

    /**
     * Registers that the widget set version has been sent to the server.
     */
    public void setWidgetsetVersionSent() {
        widgetsetVersionSent = true;
    }

    /**
     * Sets the version info object which is published to JavaScript.
     *
     * @param versionInfo
     *            the version info object
     */
    public void setVersionInfo(JavaScriptObject versionInfo) {
        this.versionInfo = versionInfo;
    }

    /**
     * Gets the version info object which is published to JavaScript.
     *
     * @return the version info object
     */
    public JavaScriptObject getVersionInfo() {
        return versionInfo;
    }

}
