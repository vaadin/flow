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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.impl.TemplateElementStateProvider;
import com.vaadin.ui.AngularTemplate;

public class AngularTemplateIncludeBuilderTest {

    @HtmlTemplate("main.html")
    public static class IncludeTemplate extends AngularTemplate {

    }

    @HtmlTemplate("includes-subfolder.html")
    public static class MultipleIncludeTemplate extends AngularTemplate {

    }

    @Test
    public void templateWithInclude() {
        // <div>
        // <span>Main template</span>
        // @include sub.html@
        // </div>

        // <div>
        // <span>Sub template</span>
        // </div>
        IncludeTemplate template = new IncludeTemplate();
        ElementTemplateNode parentTemplateNode = ((TemplateElementStateProvider) template
                .getElement().getStateProvider()).getTemplateNode();
        Element element = template.getElement();
        assertEquals("div", element.getTag());

        List<Element> children = filterOutTextChildren(element);
        assertEquals("span", children.get(0).getTag());
        assertEquals("Main template",
                element.getChild(1).getTextRecursively());

        Element subTemplateElement = children.get(1);
        assertEquals("div", subTemplateElement.getTag());

        // template node should have the main template as the parent #1176
        ElementTemplateNode includedTemplateNode = ((TemplateElementStateProvider) subTemplateElement
                .getStateProvider()).getTemplateNode();
        assertEquals(parentTemplateNode,
                includedTemplateNode.getParent().get());

        Element span = filterOutTextChildren(subTemplateElement).get(0);
        assertEquals("span", span.getTag());
        assertEquals("Sub template", span.getTextRecursively());
    }

    @Test
    public void templateWithMultipleIncludes() {
        // <div>
        // <span>Main template</span>
        // @include subfolder/includes-from-parent.html@
        // </div>

        // <div>
        // @include ../sub.html @
        // </div>

        // <div>
        // <span>Sub template</span>
        // </div>

        MultipleIncludeTemplate template = new MultipleIncludeTemplate();
        ElementTemplateNode node = ((TemplateElementStateProvider) template
                .getElement().getStateProvider()).getTemplateNode();
        Element element = template.getElement();
        assertEquals("root-template", element.getTag());

        Element firstSubTemplateElement = filterOutTextChildren(element).get(0);
        ElementTemplateNode firstSubTemplateElementNode = ((TemplateElementStateProvider) firstSubTemplateElement
                .getStateProvider()).getTemplateNode();

        assertEquals("includes-from-parent",
                firstSubTemplateElement.getTag());
        assertEquals(node,
                firstSubTemplateElementNode.getParent().get());

        Element secondSubTemplateElement = filterOutTextChildren(
                firstSubTemplateElement).get(0);
        ElementTemplateNode secondSubTemplateElementNode = ((TemplateElementStateProvider) secondSubTemplateElement
                .getStateProvider()).getTemplateNode();

        assertEquals("div", secondSubTemplateElement.getTag());
        assertEquals(firstSubTemplateElementNode,
                secondSubTemplateElementNode.getParent().get());

        Element span = filterOutTextChildren(secondSubTemplateElement).get(0);
        assertEquals("span", span.getTag());
        assertEquals("Sub template", span.getTextRecursively());
        assertEquals(secondSubTemplateElementNode,
                ((TemplateElementStateProvider) span.getStateProvider())
                        .getTemplateNode().getParent().get());

    }

    private List<Element> filterOutTextChildren(Element element) {
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < element.getChildCount(); i++) {
            Element child = element.getChild(i);
            if (!child.isTextNode()) {
                result.add(child);
            }
        }
        return result;
    }
}
