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
package com.vaadin.flow.server;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
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
public abstract class AbstractDeploymentConfiguration extends
        AbstractPropertyConfiguration implements DeploymentConfiguration {

    private volatile Set<String> urlSafeSchemes;

    /**
     * Creates a new configuration based on {@code properties}.
     *
     * @param properties
     *            configuration properties
     */
    protected AbstractDeploymentConfiguration(Map<String, String> properties) {
        super(properties);
    }

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
            if (configured == null) {
                configured = getStringProperty(
                        InitParameters.URL_SAFE_SCHEMES_LEGACY, null);
            }
            final Set<String> parsed = parseUrlSafeSchemes(configured);
            cached = parsed != null
                    ? parsed
                    : DeploymentConfiguration.super.getUrlSafeSchemes();
            urlSafeSchemes = cached;
        }
        return cached;
    }

    private static Set<String> parseUrlSafeSchemes(String configured) {
        if (configured == null || configured.isBlank()) {
            return null;
        }
        Set<String> schemes = new HashSet<>();
        for (String scheme : configured.split(",")) {
            String trimmed = scheme.trim();
            if (!trimmed.isEmpty()) {
                schemes.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return schemes.isEmpty() ? null : Set.copyOf(schemes);
    }

}
