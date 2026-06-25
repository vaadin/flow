/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.io;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class FilterableResourceResolverTest {

    private FilterableResourceResolver filterableResourceResolver;

    @Before
    public void setup() {
        filterableResourceResolver = new FilterableResourceResolver(
                Mockito.mock(FilterableResourceResolver.class));
    }

    @Test
    public void jarNamePatternMatch_test() {
        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", ""));
        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "*"));
        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "*-*"));
        assertFalse(
                "'*.*' is not matching 'spring-1.0.0.jar' due to Part[0] 'spring-1' contains '-'.",
                filterableResourceResolver
                        .jarNamePatternMatch("spring-1.0.0.jar", "*.*"));
        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "spring"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "foo"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("foo-1.0.0.jar", "spring"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("vaadin-spring-1.0.0.jar", "spring"));

        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "spring-*"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("spring.1.0.0.jar", "spring-*"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "spring-*-*"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("foo-1.0.0.jar", "spring-*"));

        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "spring-*.*.*.jar"));
        assertTrue(filterableResourceResolver
                .jarNamePatternMatch("spring-abc.1.0.jar", "spring-*.*.*.jar"));
        assertTrue(filterableResourceResolver.jarNamePatternMatch(
                "spring-abc.1.0.0.jar", "spring-*.*.*.jar"));
        assertFalse(filterableResourceResolver.jarNamePatternMatch(
                "spring-abc-1.0.0.jar", "spring-*.*.*.jar"));
        assertFalse(filterableResourceResolver.jarNamePatternMatch(
                "spring.abc.1.0.0.jar", "spring-*.*.*.jar"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.jar", "spring-*.*.*.jar"));

        assertTrue(filterableResourceResolver.jarNamePatternMatch(
                "spring-foo-1.0.0.jar", "spring-*-*.*.*.jar"));
        assertTrue(filterableResourceResolver.jarNamePatternMatch(
                "spring-foo-bar.1.0.jar", "spring-*-*.*.*.jar"));
        assertFalse(filterableResourceResolver.jarNamePatternMatch(
                "spring-foo-bar-1.0.0.jar", "spring-*-*.*.*.jar"));
        assertFalse(filterableResourceResolver
                .jarNamePatternMatch("spring-1.0.0.jar", "spring-*-*.*.*.jar"));
    }

}
