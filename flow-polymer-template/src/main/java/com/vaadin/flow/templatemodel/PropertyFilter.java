/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.util.function.Predicate;

/**
 * Property name filter that supports composition for resolving sub properties.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @deprecated This functionality is internal and bound to template model which
 *             is not supported for lit template. Polymer template support is
 *             deprecated - we recommend you to use {@code LitTemplate} instead.
 *             Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
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
