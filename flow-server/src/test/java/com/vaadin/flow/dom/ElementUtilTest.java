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
package com.vaadin.flow.dom;

import java.util.Optional;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.InertData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElementUtilTest {

    private Element parent;
    private Element child;
    private Element grandchild;
    private StateTree stateTree;

    @Test
    public void isNullValidAttribute() {
        assertFalse(ElementUtil.isValidAttributeName(null));
    }

    @Test
    public void isEmptyValidAttribute() {
        assertFalse(ElementUtil.isValidAttributeName(""));
    }

    @Test
    public void isUpperCaseValidAttribute() {
        assertThrows(AssertionError.class, () -> {
            // isValidAttributeName is designed to only be called with lowercase
            // attribute names
            ElementUtil.isValidAttributeName("FOO");
        });
    }

    @Test
    public void componentNotInitiallyAttached() {
        Element e = ElementFactory.createDiv();
        assertFalse(e.getComponent().isPresent());
    }

    @Test
    public void attachToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        assertEquals(c, e.getComponent().get());
    }

    @Test
    public void attachComponentToTextElement() {
        Element e = Element.createText("Text text");
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        assertEquals(c, e.getComponent().get());
    }

    @Test
    public void attachTwiceToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        assertThrows(IllegalStateException.class,
                () -> ElementUtil.setComponent(e, c));
    }

    @Test
    public void attachToNull() {
        Element e = ElementFactory.createDiv();
        assertThrows(IllegalArgumentException.class,
                () -> ElementUtil.setComponent(e, null));
    }

    @Test
    public void attachTwoComponents() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        Component c2 = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        assertThrows(IllegalStateException.class,
                () -> ElementUtil.setComponent(e, c2));
    }

    @Test
    public void toAndFromJsoup() {
        final String EXPECTED_TEXT_1 = "Some text";
        final String EXPECTED_TEXT_2 = "Other text";

        Element originalElement = ElementFactory.createDiv();
        originalElement.appendChild(
                ElementFactory.createParagraph(EXPECTED_TEXT_1).appendChild(
                        ElementFactory.createDiv(EXPECTED_TEXT_2)));

        Document jDocument = Document.createShell("http://example.com");

        Node jNode = ElementUtil.toJsoup(jDocument, originalElement);

        Optional<Element> optionalElement = ElementUtil.fromJsoup(jNode);

        assertTrue(optionalElement.isPresent(),
                "Element should have been created from jNode");

        Element recreatedElement = optionalElement.get();

        // root
        assertEquals("div", recreatedElement.getTag(),
                "Root element should be div");
        // child
        assertEquals("p", recreatedElement.getChild(0).getTag(),
                "Child element should be a paragraph");
        assertEquals(EXPECTED_TEXT_1, recreatedElement.getChild(0).getText(),
                "Child element should have text");
        // grand-child (#1, since #0 is the text node)
        assertEquals("div", recreatedElement.getChild(0).getChild(1).getTag(),
                "Grand-child element should be a div");
        assertEquals(EXPECTED_TEXT_2,
                recreatedElement.getChild(0).getChild(1).getText(),
                "Grand-child element should have text");
    }

    @Test
    public void isValidTagName_validTagNames() {
        assertTrue(ElementUtil.isValidTagName("foo"));
        assertTrue(ElementUtil.isValidTagName("foo-bar"));
        assertTrue(ElementUtil.isValidTagName("foo_bar"));
        assertTrue(ElementUtil.isValidTagName("foo_bar-baz"));
        assertTrue(ElementUtil.isValidTagName("foo12.bar3"));
        assertTrue(ElementUtil.isValidTagName("foo-._"));
        assertTrue(ElementUtil.isValidTagName("x"));
    }

    @Test
    public void isValidTagName_invalidTagNames() {
        assertFalse(ElementUtil.isValidTagName("1foo"));
        assertFalse(ElementUtil.isValidTagName("-foo"));
        assertFalse(ElementUtil.isValidTagName("_foo"));
        assertFalse(ElementUtil.isValidTagName(".foo"));
        assertFalse(ElementUtil.isValidTagName("foo>"));
        assertFalse(ElementUtil.isValidTagName("foo$bar"));
    }

    @Test
    public void parentIsInert_childIgnoresParentInert_allThePermutations() {
        setupElementHierarchy();

        assertFalse(isIgnoreParentInert(child),
                "by default parent inert state is not ignored");
        assertFalse(isInert(child), "by default element should not be inert");

        ElementUtil.setIgnoreParentInert(child, true);
        assertFalse(isInert(child));

        ElementUtil.setInert(parent, true);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertFalse(isInert(child));
        assertFalse(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(child, false);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertTrue(isInert(child));
        assertTrue(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(child, true);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertFalse(isInert(child));
        assertFalse(isInert(grandchild));

        ElementUtil.setInert(child, true);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertTrue(isInert(child));
        assertTrue(isInert(grandchild));

        ElementUtil.setInert(parent, false);
        simulateWritingChangesToClient();

        assertFalse(isInert(parent));
        assertTrue(isInert(child));
        assertTrue(isInert(grandchild));
    }

    @Test
    public void parentInert_grandChildIgnoresInert_notInert() {
        setupElementHierarchy();

        ElementUtil.setInert(parent, true);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertTrue(isInert(child));
        assertTrue(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(grandchild, true);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertTrue(isInert(child));
        assertFalse(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(grandchild, false);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertTrue(isInert(child));
        assertTrue(isInert(grandchild));
    }

    @Test
    public void parentInertGrandChildIgnores_statesChangedAtSameTime_changesApplied() {
        setupElementHierarchy();

        ElementUtil.setInert(parent, true);
        ElementUtil.setIgnoreParentInert(grandchild, true);
        simulateWritingChangesToClient();

        assertTrue(isInert(parent));
        assertTrue(isInert(child));
        assertFalse(isInert(grandchild));
    }

    @Test
    public void parentInert_siblingIgnoresInheritingInert_siblingInert() {
        final Element sibling = ElementFactory.createDiv();
    }

    @Test
    public void elementsUpdateSameData() {
        Element te = new Element("testelem");
        Element e = ElementUtil.from(te.getNode()).orElse(null);

        // Elements must be equal but not necessarily the same
        assertEquals(te, e);
    }

    @Test
    public void getElementFromInvalidNode() {
        StateNode node = new StateNode(ElementPropertyMap.class);
        assertFalse(ElementUtil.from(node).isPresent());
    }

    private void setupElementHierarchy() {
        parent = ElementFactory.createDiv();
        child = ElementFactory.createDiv();
        grandchild = ElementFactory.createDiv();
        parent.appendChild(child.appendChild(grandchild));
        stateTree = new StateTree(new UI().getInternals(),
                ElementChildrenList.class, InertData.class);
        final StateNode rootNode = stateTree.getRootNode();
        rootNode.getFeature(ElementChildrenList.class).add(0, parent.getNode());
    }

    private boolean isIgnoreParentInert(Element element) {
        return element.getNode().getFeatureIfInitialized(InertData.class)
                .map(InertData::isIgnoreParentInert).orElse(false);
    }

    private boolean isInert(Element element) {
        return element.getNode().isInert();
    }

    private void simulateWritingChangesToClient() {
        stateTree.collectChanges(nodeChanges -> {
        });
    }

}
