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

public class TemplateParserTest {
    @Test
    public void parseBasicTemplate() {
        ElementTemplateNode rootNode = (ElementTemplateNode) TemplateParser
                .parse("<div id=bar>baz<input></div>");

        Assert.assertEquals("div", rootNode.getTag());

        Assert.assertEquals(1, rootNode.getPropertyNames().count());
        Assert.assertEquals("bar",
                rootNode.getPropertyBinding("id").get().getValue(null));

        Assert.assertEquals(2, rootNode.getChildCount());

        TextTemplateNode textChild = (TextTemplateNode) rootNode.getChild(0);
        Assert.assertEquals("baz", textChild.getTextBinding().getValue(null));

        ElementTemplateNode inputChild = (ElementTemplateNode) rootNode
                .getChild(1);
        Assert.assertEquals("input", inputChild.getTag());
        Assert.assertEquals(0, inputChild.getPropertyNames().count());
        Assert.assertEquals(0, inputChild.getChildCount());
    }

    @Test(expected = TemplateParseException.class)
    public void parseEmptyTemplate() {
        TemplateParser.parse("Just some text, no HTML");
    }

    @Test(expected = TemplateParseException.class)
    public void parseMultipleRoots() {
        TemplateParser.parse("<br><input>");
    }

    @Test
    public void parseWithWhitespacePadding() {
        ElementTemplateNode rootNode = (ElementTemplateNode) TemplateParser
                .parse(" \n<input \r> \t ");

        Assert.assertEquals("input", rootNode.getTag());
        Assert.assertEquals(0, rootNode.getPropertyNames().count());
        Assert.assertEquals(0, rootNode.getChildCount());
    }

}
