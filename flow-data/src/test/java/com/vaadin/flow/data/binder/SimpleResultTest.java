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
