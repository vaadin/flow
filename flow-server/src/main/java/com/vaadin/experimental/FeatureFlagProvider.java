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

import java.io.Serializable;
import java.util.List;

/**
 * Service provider interface for modules to declare their feature flags.
 * <p>
 * Implementations should be registered via the Java Service Provider Interface
 * mechanism by creating a file named
 * {@code META-INF/services/com.vaadin.experimental.FeatureFlagProvider}
 * containing the fully qualified class name of the implementation.
 * <p>
 * This allows each module to define its own feature flags that will only be
 * loaded when the module is on the classpath.
 *
 * @since 24.7
 */
public interface FeatureFlagProvider extends Serializable {

    /**
     * Returns the list of features provided by this module.
     * <p>
     * The returned list should be immutable and not change during the lifetime
     * of the application.
     *
     * @return list of features, never null
     */
    List<Feature> getFeatures();
}