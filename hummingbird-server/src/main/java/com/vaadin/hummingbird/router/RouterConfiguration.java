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
 * Configuration for a {@link Router}. To avoid race conditions when the
 * framework uses the data in the configuration concurrently for handling
 * requests, the configuration must be updated using
 * {@link Router#reconfigure(RouterConfigurator)}.
 *
 * @since
 * @author Vaadin Ltd
 */
public class RouterConfiguration {
    private Resolver resolver;

    /**
     * Creates a new empty configuration.
     */
    public RouterConfiguration() {
        resolver = e -> null;
    }

    /**
     * Creates a new configuration as a copy of the given configuration.
     *
     * @param original
     *            the original configuration to copy settings from
     */
    public RouterConfiguration(RouterConfiguration original) {
        resolver = original.resolver;
    }

    /**
     * Sets the resolver to use for resolving what to show for a given
     * navigation event.
     *
     * @param resolver
     *            the resolver, not <code>null</code>
     */
    public void setResolver(Resolver resolver) {
        if (resolver == null) {
            throw new IllegalArgumentException("Resolver cannot be null");
        }
        this.resolver = resolver;
    }

    /**
     * Gets the resolver to use for resolving what to show for a given
     * navigation event.
     *
     * @return the resolver, not <code>null</code>
     */
    public Resolver getResolver() {
        return resolver;
    }

}
