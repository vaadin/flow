/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.DeploymentConfiguration;

/**
 * An abstract base class for DeploymentConfiguration implementations. This
 * class provides default implementation for common config properties.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public abstract class AbstractDeploymentConfiguration
        implements DeploymentConfiguration {

    private volatile Set<String> urlSafeSchemes;

    @Override
    public String getUIClassName() {
        return getStringProperty(InitParameters.UI_PARAMETER,
                UI.class.getName());
    }

    @Override
    public String getClassLoaderName() {
        return getStringProperty("ClassLoader", null);
    }

    @Override
    public Set<String> getUrlSafeSchemes() {
        Set<String> cached = urlSafeSchemes;
        if (cached == null) {
            String configured = getStringProperty(
                    InitParameters.URL_SAFE_SCHEMES, null);
            final Set<String> parsed = parseUrlSafeSchemes(configured);
            cached = parsed != null ? parsed
                    : DeploymentConfiguration.super.getUrlSafeSchemes();
            urlSafeSchemes = cached;
        }
        return cached;
    }

    private static Set<String> parseUrlSafeSchemes(String configured) {
        if (configured == null || configured.trim().isEmpty()) {
            return null;
        }
        Set<String> schemes = new HashSet<>();
        for (String scheme : configured.split(",")) {
            String trimmed = scheme.trim();
            if (!trimmed.isEmpty()) {
                schemes.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return schemes.isEmpty() ? null : Collections.unmodifiableSet(schemes);
    }

}
