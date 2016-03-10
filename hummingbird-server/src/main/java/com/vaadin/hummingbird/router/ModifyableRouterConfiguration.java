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

import java.io.Serializable;

/**
 * A {@link Router} configuration object that can be configured until it has
 * been sealed. Since a configuration is used concurrently when handling
 * requests, the framework only provides access to an unsealed instance through
 * the {@link Router#reconfigure(RouterConfigurator)} method. This also means
 * that you should never need to create your own configuration instances.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ModifyableRouterConfiguration
        implements Serializable, RouterConfiguration {
    private Resolver resolver;

    private boolean sealed = false;

    /**
     * Creates a new empty unsealed configuration.
     */
    public ModifyableRouterConfiguration() {
        resolver = e -> null;
    }

    /**
     * Creates a new unsealed configuration as a copy of the given
     * configuration.
     *
     * @param original
     *            the original configuration to copy settings from
     */
    public ModifyableRouterConfiguration(
            ModifyableRouterConfiguration original) {
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
        throwIfSealed();
        if (resolver == null) {
            throw new IllegalArgumentException("Resolver cannot be null");
        }
        this.resolver = resolver;
    }

    private void throwIfSealed() {
        if (isSealed()) {
            throw new IllegalStateException(
                    "Can't modify configuration that has been sealed");
        }
    }

    @Override
    public Resolver getResolver() {
        return resolver;
    }

    /**
     * Seals this configuration so that it doesn't accept any more
     * modifications.
     */
    public void seal() {
        sealed = true;
    }

    /**
     * Checks whether this configuration is sealed.
     *
     * @see #seal()
     *
     * @return <code>true</code> if it is sealed, <code>false</code> if it not
     *         sealed
     */
    public boolean isSealed() {
        return sealed;
    }

}
