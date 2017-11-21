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
package com.vaadin.flow.nodefeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.StateNode;
import com.vaadin.flow.dom.impl.TemplateElementStateProvider;

public class ModelMapTest {
    private StateNode rootModelNode;
    private ModelMap rootMap;

    @Before
    public void setup() {
        rootModelNode = TemplateElementStateProvider
                .createSubModelNode(ModelMap.class);
        rootMap = ModelMap.get(rootModelNode);
    }

    @Test
    public void putGet() {
        ModelMap map = new ModelMap(new StateNode());
        map.setValue("foo", "bar");
        assertEquals("bar", map.get("foo"));
    }

    @Test
    public void hasValue() {
        ModelMap map = new ModelMap(new StateNode());
        assertFalse(map.hasValue("foo"));
        map.setValue("foo", "bar");
        assertTrue(map.hasValue("foo"));
        map.remove("foo");
        assertFalse(map.hasValue("foo"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void dotInvalidInKey() {
        ModelMap map = new ModelMap(new StateNode());
        map.setValue("foo.bar", "a");
    }

    @Test
    public void resolveAndCreateImmediateChildMap() {
        ModelMap child = rootMap.resolveModelMap("child");
        ModelMap parent = getParentMapAndAssertMapping(child, "child");
        assertEquals(rootMap, parent);
    }

    @Test
    public void resolveAndCreateImmediateChildList() {
        ModelList child = rootMap.resolveModelList("child");
        ModelMap parent = getParentMapAndAssertMapping(child, "child");
        assertEquals(rootMap, parent);
    }

    @Test
    public void resolveAndCreateSubChildMap() {
        ModelMap child = rootMap.resolveModelMap("parent.child");
        ModelMap parent = getParentMapAndAssertMapping(child, "child");
        ModelMap resolvedRoot = getParentMapAndAssertMapping(parent, "parent");
        assertEquals(rootMap, resolvedRoot);
    }

    @Test
    public void resolveAndCreateSubChildList() {
        ModelList child = rootMap.resolveModelList("parent.child");
        ModelMap parent = getParentMapAndAssertMapping(child, "child");
        ModelMap resolvedRoot = getParentMapAndAssertMapping(parent, "parent");
        assertEquals(rootMap, resolvedRoot);
    }

    @Test
    public void resolveAndCreateSubSubChildMap() {
        ModelMap child = rootMap.resolveModelMap("grand.parent.child");
        ModelMap parent = getParentMapAndAssertMapping(child, "child");
        ModelMap grand = getParentMapAndAssertMapping(parent, "parent");
        ModelMap resolvedRoot = getParentMapAndAssertMapping(grand, "grand");
        assertEquals(rootMap, resolvedRoot);
    }

    @Test
    public void resolveAndCreateSubSubChildList() {
        ModelList child = rootMap.resolveModelList("grand.parent.child");
        ModelMap parent = getParentMapAndAssertMapping(child, "child");
        ModelMap grand = getParentMapAndAssertMapping(parent, "parent");
        ModelMap resolvedRoot = getParentMapAndAssertMapping(grand, "grand");
        assertEquals(rootMap, resolvedRoot);
    }

    @Test
    public void resolveEmptyPath() {
        assertEquals(rootMap, rootMap.resolveModelMap(""));
    }

    @Test(expected = AssertionError.class)
    public void resolvePathEndsInDot() {
        rootMap.resolveModelMap("foo.");
    }

    @Test(expected = AssertionError.class)
    public void resolvePathStartsWithDot() {
        rootMap.resolveModelMap(".foo");
    }

    private ModelMap getParentMapAndAssertMapping(ModelList child,
            String childKey) {
        assert child != null;
        assert child.getNode() != null;
        ModelMap parentMap = ModelMap.get(child.getNode().getParent());
        assert parentMap != null;
        assertEquals(child.getNode(), parentMap.getValue(childKey));
        return parentMap;
    }

    private ModelMap getParentMapAndAssertMapping(ModelMap child,
            String childKey) {
        assert child != null;
        assert child.getNode() != null;
        ModelMap parentMap = ModelMap.get(child.getNode().getParent());
        assert parentMap != null;
        assertEquals(child.getNode(), parentMap.getValue(childKey));
        return parentMap;
    }

    @Test
    public void getLastPartEmptyString() {
        assertEquals("", ModelMap.getLastPart(""));
    }

    @Test
    public void getLastPartWithoutDots() {
        assertEquals("foo", ModelMap.getLastPart("foo"));
    }

    @Test
    public void getLastPartOneLevel() {
        assertEquals("bar", ModelMap.getLastPart("foo.bar"));
    }

    @Test
    public void getLastPartTwoLevels() {
        assertEquals("baz", ModelMap.getLastPart("foo.bar.baz"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNaN_throws() {
        rootMap.setValue("value", Double.NaN);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setPositiveInfinity_throws() {
        rootMap.setValue("value", Double.POSITIVE_INFINITY);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setNegativeInfinity_throws() {
        rootMap.setValue("value", Double.NEGATIVE_INFINITY);
    }

}
