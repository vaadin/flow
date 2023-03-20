/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Class that contains all Vaadin endpoint customizable properties.
 */
@Component
@ConfigurationProperties("vaadin.endpoint")
public class FusionEndpointProperties {

    @Value("${vaadin.endpoint.prefix:/connect}")
    private String vaadinEndpointPrefix;

    /**
     * Customize the prefix for all Vaadin endpoints. See default value in the
     * {@link FusionEndpointProperties#vaadinEndpointPrefix} field annotation.
     *
     * @return prefix that should be used to access any Vaadin endpoint
     */
    public String getVaadinEndpointPrefix() {
        return vaadinEndpointPrefix;
    }

}
