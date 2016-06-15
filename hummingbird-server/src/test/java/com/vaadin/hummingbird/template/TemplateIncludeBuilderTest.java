/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.template;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.annotations.HtmlTemplate;
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.ui.Template;

public class TemplateIncludeBuilderTest {

    @HtmlTemplate("main.html")
    public static class IncludeTemplate extends Template {

    }

    @HtmlTemplate("includes-subfolder.html")
    public static class MultipleIncludeTemplate extends Template {

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
        Element element = template.getElement();
        Assert.assertEquals("div", element.getTag());

        List<Element> children = filterOutTextChildren(element);
        Assert.assertEquals("span", children.get(0).getTag());
        Assert.assertEquals("Main template",
                element.getChild(1).getTextRecursively());

        Element subTemplateElement = children.get(1);
        Assert.assertEquals("div", subTemplateElement.getTag());

        Element span = filterOutTextChildren(subTemplateElement).get(0);
        Assert.assertEquals("span", span.getTag());
        Assert.assertEquals("Sub template", span.getTextRecursively());
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
        Element element = template.getElement();
        Assert.assertEquals("root-template", element.getTag());
        Element firstSubTemplateElement = filterOutTextChildren(element).get(0);
        Assert.assertEquals("includes-from-parent",
                firstSubTemplateElement.getTag());
        Element secondSubTemplateElement = filterOutTextChildren(
                firstSubTemplateElement).get(0);
        Assert.assertEquals("div", secondSubTemplateElement.getTag());
        Element span = filterOutTextChildren(secondSubTemplateElement).get(0);
        Assert.assertEquals("span", span.getTag());
        Assert.assertEquals("Sub template", span.getTextRecursively());

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
