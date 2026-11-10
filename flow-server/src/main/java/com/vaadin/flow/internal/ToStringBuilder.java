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
package com.vaadin.flow.internal;

import java.util.StringJoiner;

/**
 * Builds a {@code toString()} value in the {@code SimpleName[…]} shape, such as
 * {@code ThemeData[themeName=lumo, variant=dark]} or
 * {@code NodeInstallation[/home/user/.vaadin/node-v24.10.0]}.
 * <p>
 * The name is the simple name of the runtime class of the described instance,
 * so that a subclass reports its own type. An anonymous class has no simple
 * name, so the closest named superclass is used instead, or the interface it
 * implements when it extends {@code Object} directly.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public final class ToStringBuilder {

    private final StringJoiner joiner;

    private ToStringBuilder(Object instance) {
        joiner = new StringJoiner(", ", typeName(instance.getClass()) + "[",
                "]");
    }

    /**
     * Starts building the {@code toString()} value of the given instance.
     *
     * @param instance
     *            the instance to describe, not {@code null}
     * @return a new builder, not {@code null}
     */
    public static ToStringBuilder of(Object instance) {
        return new ToStringBuilder(instance);
    }

    /**
     * Adds a value as a {@code name=value} pair.
     *
     * @param name
     *            the name of the value, not {@code null}
     * @param value
     *            the value, may be {@code null}
     * @return this builder, for chaining
     */
    public ToStringBuilder add(String name, Object value) {
        joiner.add(name + "=" + value);
        return this;
    }

    /**
     * Adds a value as a {@code name=value} pair if it is not {@code null}.
     *
     * @param name
     *            the name of the value, not {@code null}
     * @param value
     *            the value, may be {@code null}
     * @return this builder, for chaining
     */
    public ToStringBuilder addIfNotNull(String name, Object value) {
        return value != null ? add(name, value) : this;
    }

    /**
     * Adds a value without a name, for a value that is clear without one.
     *
     * @param value
     *            the value, may be {@code null}
     * @return this builder, for chaining
     */
    public ToStringBuilder addValue(Object value) {
        joiner.add(String.valueOf(value));
        return this;
    }

    @Override
    public String toString() {
        return joiner.toString();
    }

    private static String typeName(Class<?> type) {
        Class<?> named = type;
        while (named.getSimpleName().isEmpty()) {
            Class<?>[] interfaces = named.getInterfaces();
            named = named.getSuperclass() == Object.class
                    && interfaces.length > 0 ? interfaces[0]
                            : named.getSuperclass();
        }
        return named.getSimpleName();
    }
}
