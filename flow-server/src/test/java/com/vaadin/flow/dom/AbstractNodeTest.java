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
package com.vaadin.flow.dom;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.googlecode.gentyref.GenericTypeReflector;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.internal.nodefeature.ElementChildrenList;

public abstract class AbstractNodeTest {

    @Test(expected = IllegalArgumentException.class)
    public void insertWithNullParameter() {
        Node<?> parent = createParentNode();
        parent.insertChild(0, (Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertNullChild() {
        Node<?> parent = createParentNode();
        parent.insertChild(0, new Element[] { null });
    }

    @Test
    public void appendChildren() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);

        assertChildren(parent, child1, child2);
    }

    protected void assertChildren(Node<?> parent, Element... children) {
        Assert.assertEquals(children.length, parent.getChildCount());
        for (int i = 0; i < children.length; i++) {
            assertChild(parent, i, children[i]);
        }
    }

    protected void assertChild(Node<?> parent, int index, Element child) {
        Assert.assertEquals(child, parent.getChild(index));
    }

    @Test
    public void insertChildFirst() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.insertChild(0, child2);

        assertChildren(parent, child2, child1);
    }

    @Test
    public void insertChildMiddle() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(1, child3);

        assertChildren(parent, child1, child3, child2);
    }

    @Test
    public void insertChildAsLast() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(2, child3);

        assertChildren(parent, child1, child2, child3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void insertChildAfterLast() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2);
        parent.insertChild(3, child3);
    }

    @Test
    public void removeChildFirst() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildFirstIndex() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(0);

        assertChildren(parent, child2, child3);
    }

    @Test
    public void removeChildrenFirst() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child1, child2);

        assertChildren(parent, child3);
    }

    @Test
    public void removeChildMiddle() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child2);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildMiddleIndex() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(1);

        assertChildren(parent, child1, child3);
    }

    @Test
    public void removeChildrenMiddle() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeChild(child2, child3);

        assertChildren(parent, child1, child4);
    }

    @Test
    public void removeChildLast() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(child3);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildLastIndex() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        parent.appendChild(child1, child2, child3);
        parent.removeChild(2);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeChildrenLast() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeChild(child3, child4);

        assertChildren(parent, child1, child2);
    }

    @Test
    public void removeAllChildren() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test
    public void removeAllChildrenEmpty() {
        Node<?> parent = createParentNode();
        parent.removeAllChildren();

        assertChildren(parent);
    }

    @Test
    public void testGetChildren() {
        Node<?> parent = createParentNode();

        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        Element child3 = ElementFactory.createDiv();

        parent.appendChild(child1, child2, child3);

        List<Element> children = parent.getChildren()
                .collect(Collectors.toList());
        Assert.assertEquals(Arrays.asList(child1, child2, child3), children);
    }

    @Test
    public void testGetChildren_empty() {
        Node<?> parent = createParentNode();

        Assert.assertEquals(0, parent.getChildren().count());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNonChild() {
        Node<?> parent = createParentNode();
        Element otherElement = new Element("other");
        parent.removeChild(otherElement);
    }

    @Test
    public void getChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        Element child3 = new Element("child3");
        Element child4 = new Element("child4");
        parent.appendChild(child1, child2, child3, child4);
        Assert.assertEquals(child1, parent.getChild(0));
        Assert.assertEquals(child2, parent.getChild(1));
        Assert.assertEquals(child3, parent.getChild(2));
        Assert.assertEquals(child4, parent.getChild(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getNegativeChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);
        parent.getChild(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getAfterLastChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1, child2);
        parent.getChild(2);
    }

    @Test
    public void appendChild() {
        Node<?> parent = createParentNode();
        Element child = new Element("child");
        parent.appendChild(child);

        assertChildren(parent, child);
    }

    @Test(expected = IllegalArgumentException.class)
    public void appendNullChild() {
        Node<?> parent = createParentNode();
        parent.appendChild((Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceNullChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.setChild(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeNullChild() {
        Node<?> parent = createParentNode();
        parent.removeChild((Element[]) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceBeforeFirstChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(-1, child2);
    }

    @Test
    public void setForEmptyParent() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        parent.setChild(0, child1);
        assertChildren(parent, child1);
    }

    @Test
    public void replaceAfterLastChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(1, child2);
        assertChildren(parent, child1, child2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void replaceAfterAfterLastChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(2, child2);
    }

    @Test
    public void replaceChildWithItself() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        parent.appendChild(child1);

        parent.getNode().clearChanges();

        parent.setChild(0, child1);

        AtomicInteger changesCausedBySetChild = new AtomicInteger(0);
        parent.getNode().getFeature(ElementChildrenList.class)
                .collectChanges(change -> {
                    changesCausedBySetChild.incrementAndGet();
                });
        Assert.assertEquals(0, changesCausedBySetChild.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeChildBeforeFirst() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.removeChild(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void removeChildAfterLast() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        parent.appendChild(child1);
        parent.removeChild(1);
    }

    @Test
    public void appendAttachedChild() {
        Node<?> parent = createParentNode();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Element target = ElementFactory.createDiv();

        target.appendChild(child);

        Assert.assertEquals(child.getParent(), target);

        checkIsNotChild(parent, child);
    }

    @Test
    public void indexOfChild_firstChild() {
        Node<?> parent = createParentNode();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Assert.assertEquals(0, parent.indexOfChild(child));
    }

    @Test
    public void indexOfChild_childInTheMiddle() {
        Node<?> parent = createParentNode();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createAnchor();
        Element child3 = ElementFactory.createButton();
        parent.appendChild(child1, child2, child3);

        Assert.assertEquals(1, parent.indexOfChild(child2));
    }

    @Test
    public void indexOfChild_notAChild() {
        Node<?> parent = createParentNode();
        Element child = ElementFactory.createDiv();

        Assert.assertEquals(-1, parent.indexOfChild(child));
    }

    @Test
    public void appendFirstChildToOwnParent() {
        Node<?> parent = createParentNode();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1, child2);

        parent.appendChild(child1);
        assertChildren(parent, child2, child1);
    }

    @Test
    public void appendLastChildToOwnParent() {
        Node<?> parent = createParentNode();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1, child2);

        parent.appendChild(child2);
        assertChildren(parent, child1, child2);
    }

    @Test
    public void appendManyChildrenToOwnParent() {
        Node<?> parent = createParentNode();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1, child2);

        parent.appendChild(child2, child1);
        // Order should be changed
        assertChildren(parent, child2, child1);
    }

    @Test
    public void appendExistingAndNewChildren() {
        Node<?> parent = createParentNode();
        Element child1 = ElementFactory.createDiv();
        Element child2 = ElementFactory.createDiv();
        parent.appendChild(child1);

        parent.appendChild(child2, child1);

        assertChildren(parent, child2, child1);
    }

    @Test
    public void insertAttachedChild() {
        Node<?> parent = createParentNode();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Element target = ElementFactory.createDiv();
        target.appendChild(ElementFactory.createAnchor());

        target.insertChild(0, child);

        Assert.assertEquals(child.getParent(), target);

        checkIsNotChild(parent, child);
    }

    @Test
    public void setAttachedChild() {
        Node<?> parent = createParentNode();
        Element child = ElementFactory.createDiv();
        parent.appendChild(child);

        Element target = ElementFactory.createDiv();
        target.appendChild(ElementFactory.createAnchor());

        target.setChild(0, child);

        Assert.assertEquals(child.getParent(), target);

        checkIsNotChild(parent, child);
    }

    @Test
    public void removeFromParent() {
        Node<?> parent = createParentNode();
        Element otherElement = new Element("other");
        parent.appendChild(otherElement);
        Assert.assertEquals(parent, otherElement.getParentNode());
        otherElement.removeFromParent();
        Assert.assertNull(otherElement.getParentNode());
    }

    @Test
    public void replaceFirstChild() {
        Node<?> parent = createParentNode();
        Element child1 = new Element("child1");
        Element child2 = new Element("child2");
        parent.appendChild(child1);
        parent.setChild(0, child2);
        Assert.assertNull(child1.getParentNode());
        Assert.assertEquals(parent, child2.getParentNode());
    }

    protected void checkIsNotChild(Node<?> parent, Element child) {
        Assert.assertNotEquals(child.getParentNode(), parent);
        Assert.assertFalse(
                parent.getChildren().anyMatch(el -> el.equals(child)));
    }

    protected void assertMethodsReturnType(Class<? extends Node<?>> clazz,
            Set<String> ignore) {
        for (Method method : clazz.getMethods()) {
            if (method.getDeclaringClass().equals(Object.class)) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.isBridge()) {
                continue;
            }
            if (method.getName().startsWith("get")
                    || method.getName().startsWith("has")
                    || method.getName().startsWith("is")
                    || ignore.contains(method.getName())) {
                // Ignore
            } else {
                // Setters and such
                Type returnType = GenericTypeReflector
                        .getExactReturnType(method, clazz);
                Assert.assertEquals(
                        "Method " + method.getName()
                                + " has invalid return type",
                        clazz, returnType);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    protected abstract Node createParentNode();
}
