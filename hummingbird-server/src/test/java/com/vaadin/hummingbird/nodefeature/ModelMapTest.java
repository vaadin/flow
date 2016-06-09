/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.nodefeature;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;

public class ModelMapTest {

    @Test
    public void putGet() {
        ModelMap map = new ModelMap(new StateNode());
        map.put("foo", "bar");
        Assert.assertEquals("bar", map.get("foo"));
    }

    @Test
    public void hasValue() {
        ModelMap map = new ModelMap(new StateNode());
        Assert.assertFalse(map.hasValue("foo"));
        map.put("foo", "bar");
        Assert.assertTrue(map.hasValue("foo"));
        map.remove("foo");
        Assert.assertFalse(map.hasValue("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void dotInvalidInKey() {
        ModelMap map = new ModelMap(new StateNode());
        map.put("foo.bar", "a");
    }
}
