/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.experimental;

/**
 * Exception thrown when attempting to use a feature controlled by a feature
 * flag that is not enabled at runtime.
 * <p>
 * This exception is thrown when code attempts to use experimental functionality
 * that requires an explicit opt-in via the FeatureFlags system. To resolve this
 * exception, ensure the corresponding feature is enabled before using the
 * functionality.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class DisabledFeatureException extends RuntimeException {

    /**
     * Constructs an exception for when an attempt is made to use a feature that
     * is disabled.
     *
     * @param feature
     *            the disabled feature that was attempted to be used
     */
    public DisabledFeatureException(Feature feature) {
        super("""
                '%s' is currently an experimental feature and needs to be \
                explicitly enabled. The feature can be enabled using Copilot, in the \
                experimental features tab, or by adding a \
                `src/main/resources/vaadin-featureflags.properties` file with the following content: \
                `com.vaadin.experimental.%s=true`"""
                .formatted(feature.getTitle(), feature.getId()));

    }
}
