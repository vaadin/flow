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
