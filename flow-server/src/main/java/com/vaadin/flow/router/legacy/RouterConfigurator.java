/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.router.legacy;

/**
 * Callback interface for updating the configuration for a {@link Router}.
 *
 * @author Vaadin Ltd
 * @deprecated do not use! feature is to be removed in the near future
 */
@Deprecated
@FunctionalInterface
public interface RouterConfigurator {
    /**
     * Updates the router configuration. The updated configuration will be put
     * into use immediately after this method returns. Changes made to the
     * configuration instance after returning from this method will be silently
     * ignored.
     *
     * @param configuration
     *            the router configuration to update
     */
    void configure(RouterConfiguration configuration);
}
