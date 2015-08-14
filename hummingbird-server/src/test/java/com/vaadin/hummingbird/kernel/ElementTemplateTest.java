package com.vaadin.hummingbird.kernel;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Test;

public class ElementTemplateTest {
    @Test
    public void simpleElementTemplate_attributesMirrored() {
        Element element = new Element("span");

        // Mirror element using the same underlying data
        Element mirror = createMirror(element);

        Assert.assertEquals("span", element.getTag());

        element.setAttribute("class", "important");

        Assert.assertEquals("important", mirror.getAttribute("class"));
    }

    @Test
    public void simpleElementTemplate_childManipulation() {
        Element element = new Element("span");

        // Mirror element using the same underlying data
        Element mirror = createMirror(element);

        Assert.assertEquals(0, element.getChildCount());

        element.insertChild(0, new Element("strong"));

        Assert.assertEquals(1, mirror.getChildCount());

        Element child = mirror.getChild(0);
        Assert.assertEquals("strong", child.getTag());
        Assert.assertEquals("span", child.getParent().getTag());

        child.removeFromParent();
        Assert.assertNull(child.getParent());
        Assert.assertEquals(0, element.getChildCount());
    }

    @Test
    public void boundTemplate_attributesMirrored() {
        HashMap<String, String> defaultAttributeValues = new HashMap<String, String>() {
            {
                put("type", "checkbox");
            }
        };
        ElementTemplate elementTemplate = new BoundElementTemplate("input",
                Collections.singleton(
                        new ModelAttributeBinding("value", "modelValue")),
                defaultAttributeValues);
        StateNode node = StateNode.create();
        node.put("modelValue", "My value");

        Element element = Element.getElement(elementTemplate, node);

        Assert.assertEquals("input", element.getTag());
        Assert.assertEquals("My value", element.getAttribute("value"));
        Assert.assertEquals("checkbox", element.getAttribute("type"));
        Assert.assertNull(element.getAttribute("disabled"));
        Assert.assertNull(node.get(elementTemplate));

        // Setting attribute creates node and stores value there
        element.setAttribute("disabled", "");
        StateNode elementData = node.get(elementTemplate, StateNode.class);
        Assert.assertNotNull(elementData);
        Assert.assertEquals("", elementData.get("disabled"));

        // Setting value in element node updates attribute value
        elementData.put("myAttribute", "val");
        Assert.assertEquals("val", element.getAttribute("myAttribute"));

        // Setting default-valued attribute updates element data
        element.setAttribute("type", "radio");
        Assert.assertEquals("radio", elementData.get("type"));

        // Clearing element data restores default value
        elementData.remove("type");
        Assert.assertEquals("checkbox", element.getAttribute("type"));
    }

    @Test(expected = IllegalStateException.class)
    public void boundTemplate_setBoundAttribute_throw() {
        ElementTemplate template = new BoundElementTemplate("input",
                Collections
                        .singleton(new ModelAttributeBinding("value", "value")),
                Collections.emptyMap());

        Element element = Element.getElement(template, StateNode.create());

        Assert.assertNull(element.getAttribute("value"));

        element.setAttribute("value", "foobar");
    }

    @Test
    public void boundTemplate_rawChildren() {
        // <div (class)="root"><span (class)="child" /></div>
        BoundElementTemplate childTemplate = new BoundElementTemplate("span",
                Collections
                        .singleton(new ModelAttributeBinding("class", "child")),
                Collections.emptyMap());

        ElementTemplate rootTemplate = new BoundElementTemplate("div",
                Collections
                        .singleton(new ModelAttributeBinding("class", "root")),
                Collections.emptyMap()) {

            @Override
            public int getChildCount(StateNode node) {
                return 1;
            }

            @Override
            public Element getChild(int index, StateNode node) {
                if (index == 0) {
                    return Element.getElement(childTemplate, node);
                } else {
                    throw new IndexOutOfBoundsException();
                }
            }
        };
        childTemplate.setParentResolver(
                node -> Element.getElement(rootTemplate, node));

        StateNode node = StateNode.create();
        node.put("root", "rootClass");
        node.put("child", "childClass");

        Element rootElement = Element.getElement(rootTemplate, node);

        Assert.assertEquals(1, rootElement.getChildCount());

        Element child1 = rootElement.getChild(0);
        Assert.assertEquals("childClass", child1.getAttribute("class"));
        Assert.assertEquals("span", child1.getTag());
        Assert.assertEquals("div", child1.getParent().getTag());
        Assert.assertEquals("rootClass",
                child1.getParent().getAttribute("class"));
    }

    @Test
    public void boundTemplate_if() {
        Predicate<StateNode> predicate = n -> n.get("test") != null;

        List<BoundElementTemplate> trueChildren = Collections
                .singletonList(new BoundElementTemplate("span", Collections
                        .singletonList(new AttributeBinding("class") {
                            @Override
                            public String getValue(StateNode node) {
                                return node.get("childClass", String.class);
                            }
                        }), Collections.emptyMap()));
        List<BoundElementTemplate> falseChildren = Collections.emptyList();

        IfElementTemplate ifElementTemplate = new IfElementTemplate("div",
                Collections.emptyList(), Collections.emptyMap(), predicate,
                trueChildren, falseChildren);

        StateNode node = StateNode.create();
        Element element = Element.getElement(ifElementTemplate, node);

        Assert.assertEquals("div", element.getTag());
        Assert.assertEquals(0, element.getChildCount());

        node.put("test", Boolean.TRUE);
        node.put("childClass", "true");

        Assert.assertEquals(1, element.getChildCount());

        Element child = element.getChild(0);
        Assert.assertEquals("span", child.getTag());
        Assert.assertEquals("div", child.getParent().getTag());
        Assert.assertEquals("true", child.getAttribute("class"));

        node.remove("test");
        Assert.assertEquals(0, element.getChildCount());
        Assert.assertNull(child.getParent());
    }

    @Test
    public void boundTemplate_for() {
        BoundElementTemplate childTemplate = new BoundElementTemplate("span",
                Collections.singletonList(new AttributeBinding("class") {
                    @Override
                    public String getValue(StateNode node) {
                        return node.get("class", String.class);
                    }
                }), Collections.emptyMap());

        ForElementTemplate forTemplate = new ForElementTemplate("div",
                Collections.emptyList(), Collections.emptyMap(), "todos",
                childTemplate);

        StateNode node = StateNode.create();
        Element element = Element.getElement(forTemplate, node);

        Assert.assertEquals("div", element.getTag());
        Assert.assertEquals(0, element.getChildCount());

        Assert.assertEquals(0, element.getChildCount());

        for (int i = 0; i < 2; i++) {
            StateNode childNode = StateNode.create();
            childNode.put("class", "child" + i);
            node.getMultiValued("todos").add(childNode);
        }

        Assert.assertEquals(2, element.getChildCount());
        for (int i = 0; i < element.getChildCount(); i++) {
            Element child = element.getChild(i);
            Assert.assertEquals("span", child.getTag());
            Assert.assertEquals("div", child.getParent().getTag());
            Assert.assertEquals("child" + i, child.getAttribute("class"));
        }

        Element firstChild = element.getChild(0);
        node.getMultiValued("todos").clear();

        Assert.assertEquals(0, element.getChildCount());
        Assert.assertNull(firstChild.getParent());
    }

    @Test
    public void boundTemplate_static() {
        Element element = createTemplateElementWithStaticChild();

        Assert.assertEquals("div", element.getTag());
        Assert.assertEquals(1, element.getChildCount());
        Assert.assertEquals("span", element.getChild(0).getTag());
        Assert.assertEquals("div", element.getChild(0).getParent().getTag());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void boundStaticTemplate_removeChild_throws() {
        Element element = createTemplateElementWithStaticChild();
        element.getChild(0).removeFromParent();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void boundStaticTemplate_addChild_throws() {
        Element element = createTemplateElementWithStaticChild();

        element.insertChild(0, new Element("div"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void boundStaticTemplate_stealChild_throws() {
        Element element = createTemplateElementWithStaticChild();

        Element newParent = new Element("div");
        newParent.insertChild(0, element.getChild(0));
    }

    private static Element createTemplateElementWithStaticChild() {
        BoundElementTemplate childTemplate = new BoundElementTemplate("span",
                Collections.emptyList(), Collections.emptyMap());

        StaticChildrenElementTemplate rootTemplate = new StaticChildrenElementTemplate(
                "div", Collections.emptyList(), Collections.emptyMap(),
                Collections.singletonList(childTemplate));

        StateNode node = StateNode.create();
        Element element = Element.getElement(rootTemplate, node);
        return element;
    }

    @Test
    public void boundInUnbound_childType() {
        Element parent = new Element("div");

        StateNode node = StateNode.create();
        Element child = Element.getElement(new BoundElementTemplate("span",
                Collections.emptyList(), Collections.emptyMap()), node);

        parent.insertChild(0, child);

        Assert.assertEquals("span", parent.getChild(0).getTag());
        Assert.assertEquals("div", child.getParent().getTag());

        child.removeFromParent();
        Assert.assertNull(null, child.getParent());
        Assert.assertEquals(0, parent.getChildCount());
    }

    @Test
    public void unboundInBound_childType() {
        Element child = new Element("span");

        StateNode node = StateNode.create();
        Element parent = Element.getElement(new BoundElementTemplate("div",
                Collections.emptyList(), Collections.emptyMap()), node);

        parent.insertChild(0, child);

        Assert.assertEquals("span", parent.getChild(0).getTag());
        Assert.assertEquals("div", child.getParent().getTag());

        child.removeFromParent();
        Assert.assertNull(null, child.getParent());
        Assert.assertEquals(0, parent.getChildCount());
    }

    @Test
    public void staticTextTemplate() {
        StaticTextTemplate textTemplate = new StaticTextTemplate("Hello world");

        ElementTemplate template = new StaticChildrenElementTemplate("span",
                Collections.emptyList(), Collections.emptyMap(),
                Collections.singletonList(textTemplate));

        Element element = Element.getElement(template, StateNode.create());

        Assert.assertEquals("<span>Hello world</span>", element.toString());
    }

    @Test
    public void dynamicTextTemplate() {
        DynamicTextTemplate boundText = new DynamicTextTemplate("bound");

        DynamicTextTemplate dynamicText = new DynamicTextTemplate(
                new Function<StateNode, String>() {
                    @Override
                    public String apply(StateNode n) {
                        return n.get("dynamic", String.class).toLowerCase();
                    }
                });

        ElementTemplate template = new StaticChildrenElementTemplate("span",
                Collections.emptyList(), Collections.emptyMap(),
                Arrays.asList(boundText, dynamicText));

        StateNode node = StateNode.create();
        node.put("bound", "Hello ");
        node.put("dynamic", "WORLD");

        Element element = Element.getElement(template, node);

        Assert.assertEquals("<span>Hello world</span>", element.toString());
    }

    private static Element createMirror(Element element) {
        return Element.getElement(element.getTemplate(), element.getNode());
    }
}
