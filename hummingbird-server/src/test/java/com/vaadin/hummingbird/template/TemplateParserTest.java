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

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.StateNode;
import com.vaadin.hummingbird.nodefeature.ModelMap;

public class TemplateParserTest {
    @Test
    public void parseBasicTemplate() {
        ElementTemplateNode rootNode = (ElementTemplateNode) TemplateParser
                .parse("<div id=bar>baz<input></div>");

        Assert.assertEquals("div", rootNode.getTag());

        Assert.assertEquals(1, rootNode.getAttributeNames().count());
        Assert.assertEquals("bar",
                rootNode.getAttributeBinding("id").get().getValue(null));

        Assert.assertEquals(2, rootNode.getChildCount());

        TextTemplateNode textChild = (TextTemplateNode) rootNode.getChild(0);
        Assert.assertEquals("baz", textChild.getTextBinding().getValue(null));

        ElementTemplateNode inputChild = (ElementTemplateNode) rootNode
                .getChild(1);
        Assert.assertEquals("input", inputChild.getTag());
        Assert.assertEquals(0, inputChild.getAttributeNames().count());
        Assert.assertEquals(0, inputChild.getChildCount());
    }

    @Test
    public void parseParameterizedTextTemplate() {
        ElementTemplateNode rootNode = (ElementTemplateNode) TemplateParser
                .parse("<div id='foo'>{{bar}}<input></div>");

        Assert.assertEquals("div", rootNode.getTag());

        Assert.assertEquals(1, rootNode.getAttributeNames().count());
        Assert.assertEquals("foo",
                rootNode.getAttributeBinding("id").get().getValue(null));

        Assert.assertEquals(2, rootNode.getChildCount());

        TextTemplateNode textChild = (TextTemplateNode) rootNode.getChild(0);
        BindingValueProvider binding = textChild.getTextBinding();

        StateNode node = new StateNode(ModelMap.class);

        Assert.assertNull(binding.getValue(node));

        String value = "someValue";
        node.getFeature(ModelMap.class).setValue("bar", value);

        Assert.assertEquals(value, binding.getValue(node));
    }

    @Test
    public void parseTemplateProperty() {
        ElementTemplateNode rootNode = (ElementTemplateNode) TemplateParser
                .parse("<input [value]='foo'></input>");

        Assert.assertEquals("input", rootNode.getTag());

        Assert.assertEquals(0, rootNode.getAttributeNames().count());
        Assert.assertEquals(1, rootNode.getPropertyNames().count());

        Optional<BindingValueProvider> binding = rootNode
                .getPropertyBinding("value");
        Assert.assertTrue(binding.isPresent());

        StateNode node = new StateNode(ModelMap.class);

        Assert.assertNull(binding.get().getValue(node));

        node.getFeature(ModelMap.class).setValue("foo", "bar");

        Assert.assertEquals("bar", binding.get().getValue(node));
    }

    @Test(expected = TemplateParseException.class)
    public void parseTemplateIncorrectProperty() {
        TemplateParser.parse("<input [value='foo'></input>");
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

    @Test
    public void parseChildSlot() {
        // intentional whitespace
        TemplateNode rootNode = TemplateParser.parse("<div> @child@</div>");

        Assert.assertEquals(1, rootNode.getChildCount());

        TemplateNode childSlot = rootNode.getChild(0);
        Assert.assertEquals(ChildSlotNode.class, childSlot.getClass());
    }

    @Test(expected = TemplateParseException.class)
    public void multipleChildSlots() {
        TemplateParser.parse("<div>@child@<span>@child@</span></div>");
    }

    @Test
    public void parseTopComment() {
        ElementTemplateNode node = (ElementTemplateNode) TemplateParser
                .parse("<!-- comment --><div></div>");
        Assert.assertEquals(0, node.getChildCount());
        Assert.assertEquals("div", node.getTag());
    }

    @Test
    public void parseInnerComment() {
        ElementTemplateNode node = (ElementTemplateNode) TemplateParser
                .parse("<div> <!-- comment --> <input> </div>");
        Assert.assertEquals(4, node.getChildCount());
        Assert.assertEquals("div", node.getTag());
        Assert.assertEquals("input",
                ((ElementTemplateNode) node.getChild(2)).getTag());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ngForElementAsRoot() {
        TemplateParser.parse(
                "<a class='item' *ngFor='let  item      of list'>{{item.text}}</a>");
    }

    @Test(expected = TemplateParseException.class)
    public void ngForElementMissingCollection() {
        TemplateParser.parse(
                "<div><a class='item' *ngFor='let item'>{{item.text}}</a></div>");
    }

    @Test
    public void ngForElement() {
        TemplateNode node = TemplateParser.parse(
                "<div><a class='item' *ngFor='let  item      of list'>{{item.text}}</a></div>");
        ForTemplateNode forNode = (ForTemplateNode) node.getChild(0);
        Assert.assertEquals("list", forNode.getCollectionVariable());
        Assert.assertEquals("item", forNode.getLoopVariable());
    }

    @Test(expected = TemplateParseException.class)
    public void nestedNgForElement() {
        TemplateParser.parse("<ul>" //
                + "  <li class='item' *ngFor='let  item      of list'>" //
                + "    <a  *ngFor='let  link      of item.links' [href]='link.href'>{{link.text}}</a>" //
                + "  </li>" //
                + "</ul>"); //
    }

}
