/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.templatemodel.PropertyFilter;

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
