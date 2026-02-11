/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PropertyFilterTest {

    @Test
    public void simpleFilter() {
        PropertyFilter filter = new PropertyFilter("accept"::equals);

        Assertions.assertTrue(filter.test("accept"));
        Assertions.assertFalse(filter.test("reject"));
    }

    @Test
    public void filterWithAdditionalLevel() {
        PropertyFilter outerFilter = new PropertyFilter(
                name -> !"middle.inner.foo".equals(name));

        PropertyFilter middleFilter = new PropertyFilter(outerFilter, "middle",
                name -> !"inner.bar".equals(name));

        PropertyFilter innerFilter = new PropertyFilter(middleFilter, "inner",
                name -> !"baz".equals(name));

        // Rejected by outer filter
        Assertions.assertFalse(innerFilter.test("foo"));
        // Rejected by middle filter
        Assertions.assertFalse(innerFilter.test("bar"));
        // Rejected by inner filter
        Assertions.assertFalse(innerFilter.test("baz"));

        // Not rejected by any filter
        Assertions.assertTrue(innerFilter.test("foobar"));
    }
}
