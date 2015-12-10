package com.vaadin.hummingbird.kernel;

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
        ElementTemplate elementTemplate = TemplateBuilder.withTag("input")
                .bindAttribute("value", "modelValue")
                .setAttribute("type", "checkbox").build();
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
        ElementTemplate template = TemplateBuilder.withTag("input")
                .bindAttribute("value", "value").build();

        Element element = Element.getElement(template, StateNode.create());

        Assert.assertNull(element.getAttribute("value"));

        element.setAttribute("value", "foobar");
    }

    @Test
    public void boundTemplate_for() {
        TemplateBuilder childTemplate = TemplateBuilder.withTag("span")
                .bindAttribute("class", new Binding() {
                    @Override
                    public String getValue(StateNode node) {
                        return node.get("class", String.class);
                    }
                }).setForDefinition(new StateNodeBinding("todos"), null);

        BoundElementTemplate parentTemplate = TemplateBuilder.withTag("div")
                .addChild(childTemplate).build();

        StateNode node = StateNode.create();
        Element element = Element.getElement(parentTemplate, node);

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
            Assert.assertEquals(element, child.getParent());
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

    @Test(expected = IllegalStateException.class)
    public void boundStaticTemplate_removeChild_throws() {
        Element element = createTemplateElementWithStaticChild();
        element.getChild(0).removeFromParent();
    }

    @Test(expected = IllegalStateException.class)
    public void boundStaticTemplate_addChild_throws() {
        Element element = createTemplateElementWithStaticChild();

        element.insertChild(0, new Element("div"));
    }

    @Test(expected = IllegalStateException.class)
    public void boundStaticTemplate_stealChild_throws() {
        Element element = createTemplateElementWithStaticChild();

        Element newParent = new Element("div");
        newParent.insertChild(0, element.getChild(0));
    }

    private static Element createTemplateElementWithStaticChild() {
        TemplateBuilder childTemplate = TemplateBuilder.withTag("span");

        BoundElementTemplate rootTemplate = TemplateBuilder.withTag("div")
                .addChild(childTemplate).build();

        StateNode node = StateNode.create();
        Element element = Element.getElement(rootTemplate, node);
        return element;
    }

    @Test
    public void boundInUnbound_childType() {
        Element parent = new Element("div");

        StateNode node = StateNode.create();
        Element child = Element
                .getElement(TemplateBuilder.withTag("span").build(), node);

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
        Element parent = Element
                .getElement(TemplateBuilder.withTag("div").build(), node);

        parent.insertChild(0, child);

        Assert.assertEquals("span", parent.getChild(0).getTag());
        Assert.assertEquals("div", child.getParent().getTag());

        child.removeFromParent();
        Assert.assertNull(null, child.getParent());
        Assert.assertEquals(0, parent.getChildCount());
    }

    @Test
    public void staticTextTemplate() {
        TemplateBuilder textTemplate = TemplateBuilder
                .staticText("Hello world");

        ElementTemplate template = TemplateBuilder.withTag("span")
                .addChild(textTemplate).build();
        Element element = Element.getElement(template, StateNode.create());

        Assert.assertEquals("<span>Hello world</span>", element.toString());
    }

    @Test
    public void dynamicTextTemplate() {
        TemplateBuilder boundText = TemplateBuilder.dynamicText("bound");

        TemplateBuilder dynamicText = TemplateBuilder
                .dynamicText(n -> n.get("dynamic", String.class).toLowerCase());

        ElementTemplate template = TemplateBuilder.withTag("span")
                .addChild(boundText).addChild(dynamicText).build();

        StateNode node = StateNode.create();
        node.put("bound", "Hello ");
        node.put("dynamic", "WORLD");

        Element element = Element.getElement(template, node);

        Assert.assertEquals("<span>Hello world</span>", element.toString());
    }

    @Test
    public void boundClassPart() {
        BoundElementTemplate template = TemplateBuilder.withTag("div")
                .bindAttribute("class.completed", "done")
                .setAttribute("class", "baseClass").build();
        StateNode node = StateNode.create();

        Element element = Element.getElement(template, node);

        Assert.assertEquals("baseClass", element.getAttribute("class"));

        node.put("done", Boolean.TRUE);

        Assert.assertEquals("baseClass completed",
                element.getAttribute("class"));

        node.remove("done");

        Assert.assertEquals("baseClass", element.getAttribute("class"));
    }

    private static Element createMirror(Element element) {
        return Element.getElement(element.getTemplate(), element.getNode());
    }
}
