/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion.generator;

/**
 * Basic information of the OpenApi spec.
 */
public class OpenAPIConfiguration {

    private final String applicationTitle;
    private final String applicationApiVersion;
    private final String serverUrl;
    private final String serverDescription;

    /**
     * Create a new OpenApi configuration.
     *
     * @param applicationTitle
     *            Title of the application
     * @param applicationApiVersion
     *            api version of the application
     * @param serverUrl
     *            Base url of the application
     * @param serverDescription
     *            Description of the server
     */
    public OpenAPIConfiguration(String applicationTitle,
            String applicationApiVersion, String serverUrl,
            String serverDescription) {
        this.applicationTitle = applicationTitle;
        this.applicationApiVersion = applicationApiVersion;
        this.serverUrl = serverUrl;
        this.serverDescription = serverDescription;
    }

    /**
     * Get application title.
     *
     * @return application title
     */
    public String getApplicationTitle() {
        return applicationTitle;
    }

    /**
     * Get application api version.
     *
     * @return application api version
     */
    public String getApplicationApiVersion() {
        return applicationApiVersion;
    }

    /**
     * Get server url.
     *
     * @return server url
     */
    public String getServerUrl() {
        return serverUrl;
    }

    /**
     * Get server description.
     *
     * @return server description
     */
    public String getServerDescription() {
        return serverDescription;
    }

}
