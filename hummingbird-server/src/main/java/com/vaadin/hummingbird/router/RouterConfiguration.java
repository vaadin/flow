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
package com.vaadin.hummingbird.router;

/**
 * Configuration for a {@link Router}. Use
 * {@link Router#reconfigure(RouterConfigurator)} to update the configuration
 * used by a {@link Router}.
 *
 * @since
 * @author Vaadin Ltd
 */
public interface RouterConfiguration {
    /**
     * Gets the resolver to use for resolving what to show for a given
     * navigation event.
     *
     * @return the resolver, not <code>null</code>
     */
    Resolver getResolver();
}
