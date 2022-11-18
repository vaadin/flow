/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Commercial Vaadin Developer License
 * 4.0 (CVDLv4).
 *
 *
 * For the full License, see <https://vaadin.com/license/cvdl-4.0>.
 */
package com.vaadin.flow.templatemodel;

import org.junit.Assert;
import org.junit.Test;

public class PropertyFilterTest {

    @Test
    public void simpleFilter() {
        PropertyFilter filter = new PropertyFilter("accept"::equals);

        Assert.assertTrue(filter.test("accept"));
        Assert.assertFalse(filter.test("reject"));
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
        Assert.assertFalse(innerFilter.test("foo"));
        // Rejected by middle filter
        Assert.assertFalse(innerFilter.test("bar"));
        // Rejected by inner filter
        Assert.assertFalse(innerFilter.test("baz"));

        // Not rejected by any filter
        Assert.assertTrue(innerFilter.test("foobar"));
    }
}
