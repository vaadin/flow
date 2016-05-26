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

        Assert.assertEquals("span", element.getChild(1).getTag());
        Assert.assertEquals("Main template",
                element.getChild(1).getTextContent());

        Element subTemplateElement = element.getChild(2);
        Assert.assertEquals("div", subTemplateElement.getTag());
        Assert.assertEquals("span", subTemplateElement.getChild(1).getTag());
        Assert.assertEquals("Sub template",
                subTemplateElement.getChild(1).getTextContent());

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
        Element firstSubTemplateElement = element.getChild(0);
        Assert.assertEquals("includes-from-parent",
                firstSubTemplateElement.getTag());
        Element secondSubTemplateElement = firstSubTemplateElement.getChild(0);
        Assert.assertEquals("div", secondSubTemplateElement.getTag());
        Assert.assertEquals("span",
                secondSubTemplateElement.getChild(1).getTag());
        Assert.assertEquals("Sub template",
                secondSubTemplateElement.getChild(1).getTextContent());

    }
}
