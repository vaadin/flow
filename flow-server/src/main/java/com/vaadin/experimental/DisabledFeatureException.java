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
