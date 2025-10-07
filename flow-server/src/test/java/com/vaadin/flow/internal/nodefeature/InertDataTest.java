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
package com.vaadin.flow.internal.nodefeature;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.StateNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class InertDataTest extends AbstractNodeFeatureTest<InertData> {

    private StateNode parent;
    private StateNode child;
    private StateNode grandchild;

    @Before
    public void setup() {
        parent = createNode(null);
        child = createNode(parent);
        grandchild = createNode(child);
    }

    @Test
    public void inertData_defaults() {
        Element element = ElementFactory.createDiv();
        Assert.assertFalse("Elements should not have InertData by default",
                element.getNode().getFeatureIfInitialized(InertData.class)
                        .isPresent());
        final InertData inertData = element.getNode()
                .getFeature(InertData.class);
        Assert.assertFalse(inertData.isInertSelf());
        Assert.assertFalse(inertData.isIgnoreParentInert());
        Assert.assertTrue(inertData.allowsChanges());
    }

    @Test
    public void inertData_hasShadowRoot_handlesInertCheck() {
        parent = createNode(null);
        StateNode shadow = new StateNode(ElementChildrenList.class,
                ShadowRootHost.class);
        parent.getFeature(ElementChildrenList.class).add(0, shadow);

        grandchild = createNode(shadow);

        final InertData parentFeature = parent.getFeature(InertData.class);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        Assert.assertTrue(parentFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());
    }

    @Test
    public void inertData_inheritingInert_allPermutations() {
        final InertData childFeature = child.getFeature(InertData.class);
        childFeature.setIgnoreParentInert(true);
        Assert.assertFalse(childFeature.isInert());
        childFeature.generateChangesFromEmpty();

        final InertData parentFeature = parent.getFeature(InertData.class);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        Assert.assertTrue(parentFeature.isInert());
        Assert.assertFalse(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setIgnoreParentInert(false);
        childFeature.generateChangesFromEmpty();

        Assert.assertTrue(parentFeature.isInert());
        Assert.assertTrue(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setIgnoreParentInert(true);
        childFeature.generateChangesFromEmpty();

        Assert.assertTrue(parentFeature.isInert());
        Assert.assertFalse(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setInertSelf(true);
        childFeature.generateChangesFromEmpty();

        Assert.assertTrue(parentFeature.isInert());
        Assert.assertTrue(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        parentFeature.setInertSelf(false);
        parentFeature.generateChangesFromEmpty();

        Assert.assertFalse(parentFeature.isInert());
        Assert.assertTrue(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setInertSelf(false);
        childFeature.generateChangesFromEmpty();

        Assert.assertFalse(parentFeature.isInert());
        Assert.assertFalse(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        // both parent and child have inert data and it should cascade
        // top->down
        childFeature.setIgnoreParentInert(false);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        Assert.assertTrue(parentFeature.isInert());
        Assert.assertTrue(childFeature.isInert());
        Assert.assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());
    }

    @Test
    public void inertDataUpdates_hierarchyWithGaps_updatesCascaded() {
        StateNode greatgrandchild = createNode(grandchild);

        final InertData greatgrandchildFeature = greatgrandchild
                .getFeature(InertData.class);
        greatgrandchildFeature.setInertSelf(false);
        greatgrandchildFeature.generateChangesFromEmpty();

        Assert.assertFalse(greatgrandchild.isInert());

        final InertData parentFeature = parent.getFeature(InertData.class);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        Assert.assertTrue(parent.isInert());
        Assert.assertTrue(child.isInert());
        Assert.assertTrue(grandchild.isInert());
        Assert.assertTrue(greatgrandchild.isInert());

        final InertData grandchildFeature = grandchild
                .getFeature(InertData.class);
        grandchildFeature.setIgnoreParentInert(true);
        grandchildFeature.generateChangesFromEmpty();

        Assert.assertTrue(parent.isInert());
        Assert.assertTrue(child.isInert());
        Assert.assertFalse(grandchild.isInert());
        Assert.assertFalse(greatgrandchild.isInert());

        parentFeature.setInertSelf(false);
        parentFeature.generateChangesFromEmpty();

        Assert.assertFalse(parent.isInert());
        Assert.assertFalse(child.isInert());
        Assert.assertFalse(grandchild.isInert());
        Assert.assertFalse(greatgrandchild.isInert());

        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        Assert.assertTrue(parent.isInert());
        Assert.assertTrue(child.isInert());
        Assert.assertFalse(grandchild.isInert());
        Assert.assertFalse(greatgrandchild.isInert());

        grandchildFeature.setIgnoreParentInert(false);
        grandchildFeature.generateChangesFromEmpty();

        Assert.assertTrue(parent.isInert());
        Assert.assertTrue(child.isInert());
        Assert.assertTrue(grandchild.isInert());
        Assert.assertTrue(greatgrandchild.isInert());

        parentFeature.setInertSelf(false);
        parentFeature.generateChangesFromEmpty();

        Assert.assertFalse(parent.isInert());
        Assert.assertFalse(child.isInert());
        Assert.assertFalse(grandchild.isInert());
        Assert.assertFalse(greatgrandchild.isInert());

        parentFeature.setInertSelf(true);
        grandchildFeature.setIgnoreParentInert(true);

        parentFeature.generateChangesFromEmpty();
        // even though changes are not yet collected from grandchild,
        // value is correct due to parent changes cascading
        Assert.assertTrue(parent.isInert());
        Assert.assertTrue(child.isInert());
        Assert.assertFalse(grandchild.isInert());
        Assert.assertFalse(greatgrandchild.isInert());
    }

    private static StateNode createNode(StateNode parent) {
        final StateNode stateNode = new StateNode(ElementChildrenList.class,
                InertData.class);
        if (parent != null) {
            parent.getFeature(ElementChildrenList.class).add(0, stateNode);
        }
        return stateNode;
    }
}
