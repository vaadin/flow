/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import org.junit.Assert;
import org.junit.Test;

public class SimpleResultTest {

    @Test
    public void twoEqualSimpleResults_objectsAreEqual() {
        SimpleResult<String> one = new SimpleResult<String>("foo", null);
        SimpleResult<String> two = new SimpleResult<String>("foo", null);
        Assert.assertEquals(one, two);
    }

    @Test
    public void differentValues_objectsAreUnequal() {
        SimpleResult<String> one = new SimpleResult<String>("foo", null);
        SimpleResult<String> two = new SimpleResult<String>("baz", null);
        Assert.assertNotEquals(one, two);
    }

    @Test
    public void differentMessages_objectsAreUnequal() {
        SimpleResult<String> one = new SimpleResult<String>(null, "bar");
        SimpleResult<String> two = new SimpleResult<String>(null, "baz");
        Assert.assertNotEquals(one, two);
    }

    @Test
    public void differentClasses_objectsAreUnequal() {
        SimpleResult<String> one = new SimpleResult<String>("foo", null);
        SimpleResult<String> two = new SimpleResult<String>("foo", null) {
        };
        Assert.assertNotEquals(one, two);
    }

    @Test
    public void nullIsNotEqualToObject() {
        SimpleResult<String> one = new SimpleResult<String>("foo", null);
        Assert.assertNotEquals(one, null);
    }

    @Test
    public void twoEqualSimpleResults_hashCodeIsTheSame() {
        SimpleResult<String> one = new SimpleResult<String>("foo", null);
        SimpleResult<String> two = new SimpleResult<String>("foo", null);
        Assert.assertEquals(one.hashCode(), two.hashCode());
    }
}
