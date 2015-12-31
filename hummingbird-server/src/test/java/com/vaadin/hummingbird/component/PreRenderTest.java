package com.vaadin.hummingbird.component;

import org.junit.Assert;
import org.junit.Test;

import org.jsoup.nodes.Document;

import com.vaadin.hummingbird.kernel.BoundElementTemplate;
import com.vaadin.hummingbird.kernel.Element;
import com.vaadin.hummingbird.kernel.ElementTemplate;
import com.vaadin.hummingbird.kernel.StateNode;
import com.vaadin.hummingbird.kernel.TemplateBuilder;
import com.vaadin.hummingbird.parser.TemplateParser;
import com.vaadin.ui.HTML;
import com.vaadin.ui.PreRenderer;
import com.vaadin.ui.Template;

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

    @Test
    public void testConvertToJsoup() {
        Element element = new Element("div");
        element.setTextContent("Hello world");
        element.addClass("test");

        Element prerendered = PreRenderer.preRenderElementTree(element);
        Document document = new Document("");
        org.jsoup.nodes.Element jsoup = (org.jsoup.nodes.Element) PreRenderer
                .toJSoup(document, prerendered);

        Assert.assertEquals("<div class=\"test\">\n Hello world\n</div>",
                jsoup.outerHtml());
    }

    @Test
    public void testConvertBooleanAttributeToJsoup() {
        Element element = new Element("div");
        element.setAttribute("foo", true);

        Element prerendered = PreRenderer.preRenderElementTree(element);
        Document document = new Document("");
        org.jsoup.nodes.Element jsoup = (org.jsoup.nodes.Element) PreRenderer
                .toJSoup(document, prerendered);

        Assert.assertEquals("<div foo></div>", jsoup.outerHtml());
    }

    @Test
    public void testPreRenderHTML() {
        HTML html = new HTML("<p>Hello</p>");

        Element prerendered = PreRenderer
                .preRenderElementTree(html.getElement());
        Document document = new Document("");
        org.jsoup.nodes.Element jsoup = (org.jsoup.nodes.Element) PreRenderer
                .toJSoup(document, prerendered);

        Assert.assertEquals("<p>Hello</p>", jsoup.outerHtml());
    }

    @Test
    public void testPreRenderHTMLClasses() {
        HTML html = new HTML("<p class=\"test\">Hello</p>");

        Element prerendered = PreRenderer
                .preRenderElementTree(html.getElement());
        Document document = new Document("");
        org.jsoup.nodes.Element jsoup = (org.jsoup.nodes.Element) PreRenderer
                .toJSoup(document, prerendered);

        Assert.assertEquals("<p class=\"test\">Hello</p>", jsoup.outerHtml());
    }

    @Test
    public void testPreRenderAttrAttributes() {
        ElementTemplate t = TemplateParser.parse("<div attr.foo='bar'></div>");
        Template template = new Template(t) {
        };

        Element prerendered = PreRenderer
                .preRenderElementTree(template.getElement());
        Document document = new Document("");
        org.jsoup.nodes.Element jsoup = (org.jsoup.nodes.Element) PreRenderer
                .toJSoup(document, prerendered);

        Assert.assertEquals("<div foo=\"bar\"></div>", jsoup.outerHtml());
    }
}
