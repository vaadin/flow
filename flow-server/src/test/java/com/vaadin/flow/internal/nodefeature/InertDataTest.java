/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.internal.StateNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InertDataTest extends AbstractNodeFeatureTest<InertData> {

    private StateNode parent;
    private StateNode child;
    private StateNode grandchild;

    @BeforeEach
    void setup() {
        parent = createNode(null);
        child = createNode(parent);
        grandchild = createNode(child);
    }

    @Test
    void inertData_defaults() {
        Element element = ElementFactory.createDiv();
        assertFalse(
                element.getNode().getFeatureIfInitialized(InertData.class)
                        .isPresent(),
                "Elements should not have InertData by default");
        final InertData inertData = element.getNode()
                .getFeature(InertData.class);
        assertFalse(inertData.isInertSelf());
        assertFalse(inertData.isIgnoreParentInert());
        assertTrue(inertData.allowsChanges());
    }

    @Test
    void inertData_hasShadowRoot_handlesInertCheck() {
        parent = createNode(null);
        StateNode shadow = new StateNode(ElementChildrenList.class,
                ShadowRootHost.class);
        parent.getFeature(ElementChildrenList.class).add(0, shadow);

        grandchild = createNode(shadow);

        final InertData parentFeature = parent.getFeature(InertData.class);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        assertTrue(parentFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());
    }

    @Test
    void inertData_inheritingInert_allPermutations() {
        final InertData childFeature = child.getFeature(InertData.class);
        childFeature.setIgnoreParentInert(true);
        assertFalse(childFeature.isInert());
        childFeature.generateChangesFromEmpty();

        final InertData parentFeature = parent.getFeature(InertData.class);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        assertTrue(parentFeature.isInert());
        assertFalse(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setIgnoreParentInert(false);
        childFeature.generateChangesFromEmpty();

        assertTrue(parentFeature.isInert());
        assertTrue(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setIgnoreParentInert(true);
        childFeature.generateChangesFromEmpty();

        assertTrue(parentFeature.isInert());
        assertFalse(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setInertSelf(true);
        childFeature.generateChangesFromEmpty();

        assertTrue(parentFeature.isInert());
        assertTrue(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        parentFeature.setInertSelf(false);
        parentFeature.generateChangesFromEmpty();

        assertFalse(parentFeature.isInert());
        assertTrue(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        childFeature.setInertSelf(false);
        childFeature.generateChangesFromEmpty();

        assertFalse(parentFeature.isInert());
        assertFalse(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());

        // both parent and child have inert data and it should cascade
        // top->down
        childFeature.setIgnoreParentInert(false);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        assertTrue(parentFeature.isInert());
        assertTrue(childFeature.isInert());
        assertFalse(grandchild.getFeatureIfInitialized(InertData.class)
                .isPresent());
    }

    @Test
    void inertDataUpdates_hierarchyWithGaps_updatesCascaded() {
        StateNode greatgrandchild = createNode(grandchild);

        final InertData greatgrandchildFeature = greatgrandchild
                .getFeature(InertData.class);
        greatgrandchildFeature.setInertSelf(false);
        greatgrandchildFeature.generateChangesFromEmpty();

        assertFalse(greatgrandchild.isInert());

        final InertData parentFeature = parent.getFeature(InertData.class);
        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        assertTrue(parent.isInert());
        assertTrue(child.isInert());
        assertTrue(grandchild.isInert());
        assertTrue(greatgrandchild.isInert());

        final InertData grandchildFeature = grandchild
                .getFeature(InertData.class);
        grandchildFeature.setIgnoreParentInert(true);
        grandchildFeature.generateChangesFromEmpty();

        assertTrue(parent.isInert());
        assertTrue(child.isInert());
        assertFalse(grandchild.isInert());
        assertFalse(greatgrandchild.isInert());

        parentFeature.setInertSelf(false);
        parentFeature.generateChangesFromEmpty();

        assertFalse(parent.isInert());
        assertFalse(child.isInert());
        assertFalse(grandchild.isInert());
        assertFalse(greatgrandchild.isInert());

        parentFeature.setInertSelf(true);
        parentFeature.generateChangesFromEmpty();

        assertTrue(parent.isInert());
        assertTrue(child.isInert());
        assertFalse(grandchild.isInert());
        assertFalse(greatgrandchild.isInert());

        grandchildFeature.setIgnoreParentInert(false);
        grandchildFeature.generateChangesFromEmpty();

        assertTrue(parent.isInert());
        assertTrue(child.isInert());
        assertTrue(grandchild.isInert());
        assertTrue(greatgrandchild.isInert());

        parentFeature.setInertSelf(false);
        parentFeature.generateChangesFromEmpty();

        assertFalse(parent.isInert());
        assertFalse(child.isInert());
        assertFalse(grandchild.isInert());
        assertFalse(greatgrandchild.isInert());

        parentFeature.setInertSelf(true);
        grandchildFeature.setIgnoreParentInert(true);

        parentFeature.generateChangesFromEmpty();
        // even though changes are not yet collected from grandchild,
        // value is correct due to parent changes cascading
        assertTrue(parent.isInert());
        assertTrue(child.isInert());
        assertFalse(grandchild.isInert());
        assertFalse(greatgrandchild.isInert());
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
