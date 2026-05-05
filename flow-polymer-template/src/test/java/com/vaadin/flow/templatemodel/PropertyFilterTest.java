/**
 * Copyright (C) 2022-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyFilterTest {

    @Test
    void simpleFilter() {
        PropertyFilter filter = new PropertyFilter("accept"::equals);

        assertTrue(filter.test("accept"));
        assertFalse(filter.test("reject"));
    }

    @Test
    void filterWithAdditionalLevel() {
        PropertyFilter outerFilter = new PropertyFilter(
                name -> !"middle.inner.foo".equals(name));

        PropertyFilter middleFilter = new PropertyFilter(outerFilter, "middle",
                name -> !"inner.bar".equals(name));

        PropertyFilter innerFilter = new PropertyFilter(middleFilter, "inner",
                name -> !"baz".equals(name));

        // Rejected by outer filter
        assertFalse(innerFilter.test("foo"));
        // Rejected by middle filter
        assertFalse(innerFilter.test("bar"));
        // Rejected by inner filter
        assertFalse(innerFilter.test("baz"));

        // Not rejected by any filter
        assertTrue(innerFilter.test("foobar"));
    }
}
