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

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.shared.ApplicationConstants;

/**
 * Resolves the {@code value()} of
 * {@link com.vaadin.flow.component.dependency.StyleSheet} annotation values to
 * a canonical form that
 * {@link com.vaadin.flow.server.BootstrapHandler.BootstrapUriResolver} can
 * expand at render time. The same rules are used whether the annotation is on
 * an {@link com.vaadin.flow.component.page.AppShellConfigurator} or on an
 * ordinary {@link com.vaadin.flow.component.Component}.
 * <p>
 * For internal framework use only.
 */
public final class FrontendDependencyUrlResolver implements Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(FrontendDependencyUrlResolver.class);

    private FrontendDependencyUrlResolver() {
    }

    /**
     * Normalizes a frontend-dependency URL value to a form that the bootstrap
     * URI resolver can expand.
     * <p>
     * Rules, in order:
     * <ol>
     * <li>{@code null} or blank values return {@code null}.</li>
     * <li>Values containing path traversals ({@code ..}) are rejected with a
     * warning and {@code null} is returned.</li>
     * <li>{@code http://}, {@code https://}, {@code //}, {@code context://} and
     * {@code base://} prefixes are returned unchanged.</li>
     * <li>A leading {@code ./} is stripped.</li>
     * <li>If the value (after the previous step) starts with {@code /}, it is
     * returned unchanged.</li>
     * <li>Otherwise, {@code context://} is prepended so the value resolves
     * against the servlet context root.</li>
     * </ol>
     *
     * @param rawValue
     *            the raw annotation value
     * @return the normalized value, or {@code null} if rejected
     */
    public static String resolveToContextRoot(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String value = rawValue.trim();
        String pathForCheck = value.startsWith("/") ? value : "/" + value;
        if (HandlerHelper.isPathUnsafe(pathForCheck)) {
            LOGGER.warn(
                    "Frontend dependency value containing traversals ('../') is not allowed, ignored: {}",
                    value);
            return null;
        }
        String lower = value.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || lower.startsWith("//")
                || lower.startsWith(
                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)
                || lower.startsWith(
                        ApplicationConstants.BASE_PROTOCOL_PREFIX)) {
            return value;
        }
        if (value.startsWith("./")) {
            value = value.substring(2);
        }
        if (value.startsWith("/")) {
            return value;
        }
        return ApplicationConstants.CONTEXT_PROTOCOL_PREFIX + value;
    }
}
