package com.vaadin.hummingbird.component;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.ui.PreRenderer;

public class PreRenderTest {
    @Test
    public void testPreRenderBoundTextTemplate() {
        BoundElementTemplate template = TemplateBuilder.withTag("div")
                .addChild(TemplateBuilder.staticText("hello")).build();

        Element element = Element.getElement(template, StateNode.create());

        Element prerendered = PreRenderer.preRenderElementTree(element);

        Assert.assertEquals("div", prerendered.getTag());
        Assert.assertEquals(1, prerendered.getChildCount());

        Element child = prerendered.getChild(0);

        Assert.assertEquals("#text", child.getTag());
        Assert.assertEquals("hello", child.getTextContent());
    }

    @Test
    public void testPreRenderUnboundTextTemplate() {
        BoundElementTemplate template = TemplateBuilder.withTag("div")
                .addChild(TemplateBuilder.dynamicText("foo")).build();

        Element element = Element.getElement(template, StateNode.create());

        Element prerendered = PreRenderer.preRenderElementTree(element);

        Assert.assertEquals("div", prerendered.getTag());
        Assert.assertEquals(0, prerendered.getChildCount());
    }

    @Test
    public void testPreRenderEmptyTextContent() {
        Element element = new Element("div");
        element.setTextContent("");

        Element prerendered = PreRenderer.preRenderElementTree(element);

        Assert.assertEquals("div", prerendered.getTag());
        Assert.assertEquals(0, prerendered.getChildCount());
    }
}
