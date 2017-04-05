/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.hummingbird.dom;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.dom.impl.PolymerElementStateProvider;
import com.vaadin.hummingbird.nodefeature.ModelMap;

public class PolymerElementStateProviderTest {

    private StateNode stateNode;

    @Before
    public void setUp() {
        stateNode = new StateNode(ModelMap.class);
    }

    @Test
    public void setProperty_propertyStoredInModelMap() {
        PolymerElementStateProvider.get().setProperty(stateNode, "foo", "bar",
                false);
        Assert.assertEquals("bar",
                stateNode.getFeature(ModelMap.class).getValue("foo"));
    }

    @Test
    public void getProperty_propertyIsReadFromModelMap() {
        stateNode.getFeature(ModelMap.class).setValue("foo", "bar");
        Assert.assertEquals("bar", PolymerElementStateProvider.get()
                .getProperty(stateNode, "foo"));
    }

    @Test
    public void hasProperty_propertyIsCheckedInModelMap() {
        Assert.assertFalse(PolymerElementStateProvider.get()
                .hasProperty(stateNode, "foo"));
        stateNode.getFeature(ModelMap.class).setValue("foo", "bar");
        Assert.assertTrue(PolymerElementStateProvider.get()
                .hasProperty(stateNode, "foo"));
    }

    @Test
    public void removeProperty_propertyIsRemovedFromModelMap() {
        stateNode.getFeature(ModelMap.class).setValue("foo", "bar");
        PolymerElementStateProvider.get().removeProperty(stateNode, "foo");
        Assert.assertFalse(PolymerElementStateProvider.get()
                .hasProperty(stateNode, "foo"));
    }

    @Test
    public void getPropertyNames_propertiesAreReeadFromModelMap() {
        stateNode.getFeature(ModelMap.class).setValue("foo", "bar");
        List<String> propertyNames = PolymerElementStateProvider.get()
                .getPropertyNames(stateNode).collect(Collectors.toList());
        Assert.assertEquals(1, propertyNames.size());
        String name = propertyNames.get(0);
        Assert.assertEquals("foo", name);
    }
}
