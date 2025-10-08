/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.dom;

import java.util.Optional;

import org.mockito.Mockito;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ElementChildrenList;
import com.vaadin.flow.internal.nodefeature.ElementPropertyMap;
import com.vaadin.flow.internal.nodefeature.InertData;

import com.vaadin.flow.component.Component;

public class ElementUtilTest {

    private Element parent;
    private Element child;
    private Element grandchild;
    private StateTree stateTree;

    @Test
    public void isNullValidAttribute() {
        Assert.assertFalse(ElementUtil.isValidAttributeName(null));
    }

    @Test
    public void isEmptyValidAttribute() {
        Assert.assertFalse(ElementUtil.isValidAttributeName(""));
    }

    @Test(expected = AssertionError.class)
    public void isUpperCaseValidAttribute() {
        // isValidAttributeName is designed to only be called with lowercase
        // attribute names
        ElementUtil.isValidAttributeName("FOO");
    }

    @Test
    public void componentNotInitiallyAttached() {
        Element e = ElementFactory.createDiv();
        Assert.assertFalse(e.getComponent().isPresent());
    }

    @Test
    public void attachToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        Assert.assertEquals(c, e.getComponent().get());
    }

    @Test
    public void attachComponentToTextElement() {
        Element e = Element.createText("Text text");
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        Assert.assertEquals(c, e.getComponent().get());
    }

    @Test(expected = IllegalStateException.class)
    public void attachTwiceToComponent() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        ElementUtil.setComponent(e, c);
    }

    @Test(expected = IllegalArgumentException.class)
    public void attachToNull() {
        Element e = ElementFactory.createDiv();
        ElementUtil.setComponent(e, null);
    }

    @Test(expected = IllegalStateException.class)
    public void attachTwoComponents() {
        Element e = ElementFactory.createDiv();
        Component c = Mockito.mock(Component.class);
        Component c2 = Mockito.mock(Component.class);
        ElementUtil.setComponent(e, c);
        ElementUtil.setComponent(e, c2);
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

        Assert.assertTrue("Element should have been created from jNode",
                optionalElement.isPresent());

        Element recreatedElement = optionalElement.get();

        // root
        Assert.assertEquals("Root element should be div", "div",
                recreatedElement.getTag());
        // child
        Assert.assertEquals("Child element should be a paragraph", "p",
                recreatedElement.getChild(0).getTag());
        Assert.assertEquals("Child element should have text", EXPECTED_TEXT_1,
                recreatedElement.getChild(0).getText());
        // grand-child (#1, since #0 is the text node)
        Assert.assertEquals("Grand-child element should be a div", "div",
                recreatedElement.getChild(0).getChild(1).getTag());
        Assert.assertEquals("Grand-child element should have text",
                EXPECTED_TEXT_2,
                recreatedElement.getChild(0).getChild(1).getText());
    }

    @Test
    public void isValidTagName_validTagNames() {
        Assert.assertTrue(ElementUtil.isValidTagName("foo"));
        Assert.assertTrue(ElementUtil.isValidTagName("foo-bar"));
        Assert.assertTrue(ElementUtil.isValidTagName("foo_bar"));
        Assert.assertTrue(ElementUtil.isValidTagName("foo_bar-baz"));
        Assert.assertTrue(ElementUtil.isValidTagName("foo12.bar3"));
        Assert.assertTrue(ElementUtil.isValidTagName("foo-._"));
        Assert.assertTrue(ElementUtil.isValidTagName("x"));
    }

    @Test
    public void isValidTagName_invalidTagNames() {
        Assert.assertFalse(ElementUtil.isValidTagName("1foo"));
        Assert.assertFalse(ElementUtil.isValidTagName("-foo"));
        Assert.assertFalse(ElementUtil.isValidTagName("_foo"));
        Assert.assertFalse(ElementUtil.isValidTagName(".foo"));
        Assert.assertFalse(ElementUtil.isValidTagName("foo>"));
        Assert.assertFalse(ElementUtil.isValidTagName("foo$bar"));
    }

    @Test
    public void parentIsInert_childIgnoresParentInert_allThePermutations() {
        setupElementHierarchy();

        Assert.assertFalse("by default parent inert state is not ignored",
                isIgnoreParentInert(child));
        Assert.assertFalse("by default element should not be inert",
                isInert(child));

        ElementUtil.setIgnoreParentInert(child, true);
        Assert.assertFalse(isInert(child));

        ElementUtil.setInert(parent, true);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertFalse(isInert(child));
        Assert.assertFalse(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(child, false);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertTrue(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(child, true);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertFalse(isInert(child));
        Assert.assertFalse(isInert(grandchild));

        ElementUtil.setInert(child, true);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertTrue(isInert(grandchild));

        ElementUtil.setInert(parent, false);
        simulateWritingChangesToClient();

        Assert.assertFalse(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertTrue(isInert(grandchild));
    }

    @Test
    public void parentInert_grandChildIgnoresInert_notInert() {
        setupElementHierarchy();

        ElementUtil.setInert(parent, true);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertTrue(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(grandchild, true);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertFalse(isInert(grandchild));

        ElementUtil.setIgnoreParentInert(grandchild, false);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertTrue(isInert(grandchild));
    }

    @Test
    public void parentInertGrandChildIgnores_statesChangedAtSameTime_changesApplied() {
        setupElementHierarchy();

        ElementUtil.setInert(parent, true);
        ElementUtil.setIgnoreParentInert(grandchild, true);
        simulateWritingChangesToClient();

        Assert.assertTrue(isInert(parent));
        Assert.assertTrue(isInert(child));
        Assert.assertFalse(isInert(grandchild));
    }

    @Test
    public void parentInert_siblingIgnoresInheritingInert_siblingInert() {
        final Element sibling = ElementFactory.createDiv();
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

    @Test
    public void elementsUpdateSameData() {
        Element te = new Element("testelem");
        Element e = ElementUtil.from(te.getNode()).orElse(null);

        // Elements must be equal but not necessarily the same
        Assert.assertEquals(te, e);
    }

    @Test
    public void getElementFromInvalidNode() {
        StateNode node = new StateNode(ElementPropertyMap.class);
        Assert.assertFalse(ElementUtil.from(node).isPresent());
    }

}
