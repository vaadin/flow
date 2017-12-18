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
package com.vaadin.flow.template.angular;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.TemplateElementStateProviderTest.NullTemplateResolver;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TextTemplateNode;
import com.vaadin.flow.template.angular.parser.TemplateParser;

public class ScriptParsingTest {

    private TemplateNode parse(String html) {
        return TemplateParser.parse(html, new NullTemplateResolver());
    }

    @Test
    public void inlineStyles() {
        String contents = ".foo {\n"//
                + "  font-weight: bold;\n" //
                + "}\n";
        TemplateNode templateNode = parse("<style>" //
                + contents + "</style>" //
        ); //
        Assert.assertEquals(ElementTemplateNode.class, templateNode.getClass());
        ElementTemplateNode elementTemplate = (ElementTemplateNode) templateNode;
        Assert.assertEquals("style", elementTemplate.getTag());

        TextTemplateNode textNode = ((TextTemplateNode) elementTemplate
                .getChild(0));
        String nodeContents = (String) textNode.getTextBinding()
                .getValue(new StateNode());
        Assert.assertEquals(contents, nodeContents);
    }

    @Test
    public void inlineScript() {
        String script = "window.alert('hello');\n" + "window.alert('world');\n";
        TemplateNode templateNode = parse("<script>" //
                + script + "</script>" //
        ); //
        Assert.assertEquals(ElementTemplateNode.class, templateNode.getClass());
        ElementTemplateNode elementTemplate = (ElementTemplateNode) templateNode;
        Assert.assertEquals("script", elementTemplate.getTag());

        TextTemplateNode textNode = ((TextTemplateNode) elementTemplate
                .getChild(0));
        String nodeContents = (String) textNode.getTextBinding()
                .getValue(new StateNode());
        Assert.assertEquals(script, nodeContents);
    }

    @Test
    public void emptyInlineStyle() {
        TemplateNode templateNode = parse("<style></style>");
        Assert.assertEquals(ElementTemplateNode.class, templateNode.getClass());
        ElementTemplateNode elementTemplate = (ElementTemplateNode) templateNode;
        Assert.assertEquals("style", elementTemplate.getTag());
        Assert.assertEquals(0, elementTemplate.getChildCount());
    }

    @Test
    public void emptyInlineScript() {
        TemplateNode templateNode = parse("<script></script>");
        Assert.assertEquals(ElementTemplateNode.class, templateNode.getClass());
        ElementTemplateNode elementTemplate = (ElementTemplateNode) templateNode;
        Assert.assertEquals("script", elementTemplate.getTag());
        Assert.assertEquals(0, elementTemplate.getChildCount());
    }

    @Test
    public void scriptWithAttributes() {
        TemplateNode templateNode = parse(
                "<script type='text/javascript' src='file://foobar'/>");
        Assert.assertEquals(ElementTemplateNode.class, templateNode.getClass());
        ElementTemplateNode elementTemplate = (ElementTemplateNode) templateNode;
        Assert.assertEquals("script", elementTemplate.getTag());
        Assert.assertEquals("text/javascript", elementTemplate
                .getAttributeBinding("type").get().getValue(new StateNode()));
        Assert.assertEquals("file://foobar", elementTemplate
                .getAttributeBinding("src").get().getValue(new StateNode()));

    }
}
