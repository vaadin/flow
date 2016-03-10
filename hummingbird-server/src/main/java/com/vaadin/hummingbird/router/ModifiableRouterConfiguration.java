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
 * A {@link Router} configuration object that may be in a modifiable state.
 * Since a configuration is used concurrently when handling requests, the
 * framework only provides access to an unsealed instance through the
 * {@link Router#reconfigure(RouterConfigurator)} method. This also means that
 * you should never need to create your own configuration instances.
 *
 * @since
 * @author Vaadin Ltd
 */
public class ModifiableRouterConfiguration
        implements Serializable, RouterConfiguration {
    private final boolean modifiable;

    private Resolver resolver;

    /**
     * Creates a new empty immutable configuration.
     */
    public ModifiableRouterConfiguration() {
        resolver = e -> null;
        modifiable = false;
    }

    /**
     * Creates a new configuration as a copy of the given configuration.
     *
     * @param original
     *            the original configuration to copy settings from, not
     *            <code>null</code>
     * @param modifiable
     *            <code>true</code> to set the instance as modifiable,
     *            <code>false</code> to set it as immutable
     */
    public ModifiableRouterConfiguration(ModifiableRouterConfiguration original,
            boolean modifiable) {
        assert original != null;

        resolver = original.resolver;
        this.modifiable = modifiable;
    }

    /**
     * Sets the resolver to use for resolving what to show for a given
     * navigation event.
     *
     * @param resolver
     *            the resolver, not <code>null</code>
     */
    public void setResolver(Resolver resolver) {
        throwIfImmutable();
        if (resolver == null) {
            throw new IllegalArgumentException("Resolver cannot be null");
        }
        this.resolver = resolver;
    }

    private void throwIfImmutable() {
        if (!isModifiable()) {
            throw new IllegalStateException("Configuration is immutable");
        }
    }

    @Override
    public Resolver getResolver() {
        return resolver;
    }

    /**
     * Checks whether this configuration can be modified.
     *
     * @return <code>true</code> if it is modifiable, <code>false</code> if it
     *         immutable
     */
    public boolean isModifiable() {
        return modifiable;
    }

}
