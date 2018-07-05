/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.templatemodel;

import java.util.function.Predicate;

/**
 * Property name filter that supports composition for resolving sub properties.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class PropertyFilter implements Predicate<String> {
    private final String prefix;

    private final Predicate<String> predicate;

    /**
     * An unwrapped filter that accepts all property names.
     */
    public static final PropertyFilter ACCEPT_ALL = new PropertyFilter(
            name -> true);

    /**
     * Creates a new unwrapped filter from the given predicate.
     *
     * @param predicate
     *            the predicate to use for this filter, not <code>null</code>
     */
    public PropertyFilter(Predicate<String> predicate) {
        this("", predicate);
    }

    /**
     * Creates a new filter by combining a predicate with a filter for an outer
     * scope.
     *
     * @param outerFilter
     *            the filter of the outer scope, not <code>null</code>
     * @param scopeName
     *            the name used in the outer filter when referencing properties
     *            in the inner scope, not <code>null</code>
     * @param predicate
     *            a predicate matching property names in the inner scope
     */
    public PropertyFilter(PropertyFilter outerFilter, String scopeName,
                          Predicate<String> predicate) {
        this(composePrefix(outerFilter, scopeName),
                predicate.and(composeFilter(outerFilter, scopeName)));
    }

    /**
     * Creates a new filter by adapting a filter from an outer scope.
     *
     * @param outerFilter
     *            the filter of the outer scope, not <code>null</code>
     * @param scopeName
     *            the name used in the outer filter when referencing properties
     *            in the inner scope, not <code>null</code>
     */
    public PropertyFilter(PropertyFilter outerFilter, String scopeName) {
        this(outerFilter, scopeName, name -> true);
    }

    private PropertyFilter(String prefix, Predicate<String> predicate) {
        assert predicate != null;
        assert prefix != null;
        assert prefix.isEmpty() || prefix.endsWith(".");
        assert !prefix.startsWith(".");

        this.prefix = prefix;
        this.predicate = predicate;
    }

    private static String composePrefix(PropertyFilter outerFilter,
            String scopeName) {
        assert scopeName != null;
        assert !scopeName.isEmpty();
        assert !scopeName.contains(".");

        return outerFilter.prefix + scopeName + ".";
    }

    private static Predicate<? super String> composeFilter(
            PropertyFilter outerFilter, String scopeName) {
        return name -> outerFilter.test(scopeName + "." + name);
    }

    @Override
    public boolean test(String propertyName) {
        return predicate.test(propertyName);
    }

    /**
     * Get the full path prefix of this property filter. The prefix is
     * accumulated when constructing new property filters from outer filters and
     * scope names.
     * 
     * @return the prefix string of this filter
     */
    public String getPrefix() {
        return prefix;
    }
}
