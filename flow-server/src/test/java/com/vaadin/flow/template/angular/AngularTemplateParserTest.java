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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.dom.TemplateElementStateProviderTest.NullTemplateResolver;
import com.vaadin.flow.internal.StateNode;
import com.vaadin.flow.internal.nodefeature.ModelMap;
import com.vaadin.flow.template.angular.BindingValueProvider;
import com.vaadin.flow.template.angular.ChildSlotNode;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.template.angular.ForTemplateNode;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TemplateParseException;
import com.vaadin.flow.template.angular.TextTemplateNode;
import com.vaadin.flow.template.angular.parser.TemplateParser;
import com.vaadin.flow.template.angular.parser.TemplateResolver;

public class AngularTemplateParserTest {
    private TemplateResolver nullTemplateResolver = new NullTemplateResolver();

    @Test
    public void parseBasicTemplate() {
        ElementTemplateNode rootNode = (ElementTemplateNode) parse(
                "<div id=bar>baz<input></div>");

        Assert.assertEquals("div", rootNode.getTag());

        Assert.assertEquals(0, rootNode.getPropertyNames().count());
        Assert.assertEquals(0, rootNode.getClassNames().count());

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

    private TemplateNode parse(String html) {
        return TemplateParser.parse(html, nullTemplateResolver);
    }

    @Test
    public void parseParameterizedTextTemplate() {
        ElementTemplateNode rootNode = (ElementTemplateNode) parse(
                "<div id='foo'>{{bar}}<input></div>");

        Assert.assertEquals("div", rootNode.getTag());

        Assert.assertEquals(1, rootNode.getAttributeNames().count());
        Assert.assertEquals("foo",
                rootNode.getAttributeBinding("id").get().getValue(null));

        Assert.assertEquals(2, rootNode.getChildCount());

        TextTemplateNode textChild = (TextTemplateNode) rootNode.getChild(0);
        BindingValueProvider binding = textChild.getTextBinding();

        StateNode node = new StateNode(ModelMap.class);

        // Explicitly set "bar" property to null. So model has property "bar".
        // See #970
        ModelMap.get(node).setValue("bar", null);
        Assert.assertNull(binding.getValue(node));

        String value = "someValue";
        ModelMap.get(node).setValue("bar", value);

        Assert.assertEquals(value, binding.getValue(node));
    }

    @Test
    public void parseTemplateProperty() {
        ElementTemplateNode rootNode = (ElementTemplateNode) parse(
                "<input [value]='foo'></input>");

        Assert.assertEquals("input", rootNode.getTag());

        Assert.assertEquals(0, rootNode.getAttributeNames().count());
        Assert.assertEquals(0, rootNode.getClassNames().count());
        Assert.assertEquals(1, rootNode.getPropertyNames().count());

        Optional<BindingValueProvider> binding = rootNode
                .getPropertyBinding("value");
        Assert.assertTrue(binding.isPresent());

        StateNode node = new StateNode(ModelMap.class);

        // Explicitly set "foo" property to null. So model has property "foo".
        // See #970
        ModelMap.get(node).setValue("foo", null);

        Assert.assertNull(binding.get().getValue(node));

        ModelMap.get(node).setValue("foo", "bar");

        Assert.assertEquals("bar", binding.get().getValue(node));
    }

    @Test(expected = TemplateParseException.class)
    public void parseTemplateIncorrectProperty() {
        parse("<input [value='foo'></input>");
    }

    @Test
    public void parseTemplateAttribute() {
        ElementTemplateNode rootNode = (ElementTemplateNode) parse(
                "<input [attr.value]='foo'></input>");

        Assert.assertEquals("input", rootNode.getTag());

        Assert.assertEquals(1, rootNode.getAttributeNames().count());
        Assert.assertEquals(0, rootNode.getClassNames().count());
        Assert.assertEquals(0, rootNode.getPropertyNames().count());

        Optional<BindingValueProvider> binding = rootNode
                .getAttributeBinding("value");
        Assert.assertTrue(binding.isPresent());

        StateNode node = new StateNode(ModelMap.class);

        // Explicitly set "foo" property to null. So model has property "foo".
        // See #970
        ModelMap.get(node).setValue("foo", null);
        Assert.assertNull(binding.get().getValue(node));

        ModelMap.get(node).setValue("foo", "bar");

        Assert.assertEquals("bar", binding.get().getValue(node));
    }

    @Test(expected = TemplateParseException.class)
    public void parseTemplateIncorrectAttribute() {
        parse("<input [attr.value]='foo' value='bar'>");
    }

    @Test(expected = TemplateParseException.class)
    public void parseTemplateIncorrectEmptyAttribute() {
        parse("<input [attr.value]='foo' value>");
    }

    @Test
    public void parseClassName() {
        ElementTemplateNode rootNode = (ElementTemplateNode) parse(
                "<input [class.foo]=bar [class.camelCase]=baz></input>");

        Assert.assertEquals(0, rootNode.getAttributeNames().count());
        Assert.assertEquals(0, rootNode.getPropertyNames().count());
        Assert.assertEquals(2, rootNode.getClassNames().count());

        Optional<BindingValueProvider> fooBinding = rootNode
                .getClassNameBinding("foo");
        Assert.assertTrue(fooBinding.isPresent());

        Optional<BindingValueProvider> camelCaseBinding = rootNode
                .getClassNameBinding("camelCase");
        Assert.assertTrue(camelCaseBinding.isPresent());

        StateNode node = new StateNode(ModelMap.class);

        // Explicitly set "bar" property to null. So model has property "bar".
        // See #970
        ModelMap.get(node).setValue("bar", null);
        Assert.assertNull(fooBinding.get().getValue(node));
        Assert.assertNull(camelCaseBinding.get().getValue(node));

        ModelMap.get(node).setValue("bar", "value");
        ModelMap.get(node).setValue("baz", "value2");

        Assert.assertEquals("value", fooBinding.get().getValue(node));
        Assert.assertEquals("value2", camelCaseBinding.get().getValue(node));
    }

    @Test(expected = TemplateParseException.class)
    public void parseClassOverlaps() {
        parse("<input class=foo [class.foo]=bar>");
    }

    @Test(expected = TemplateParseException.class)
    public void parseEmptyTemplate() {
        parse("Just some text, no HTML");
    }

    @Test(expected = TemplateParseException.class)
    public void parseMultipleRoots() {
        parse("<br><input>");
    }

    @Test
    public void parseWithWhitespacePadding() {
        ElementTemplateNode rootNode = (ElementTemplateNode) parse(
                " \n<input \r> \t ");

        Assert.assertEquals("input", rootNode.getTag());
        Assert.assertEquals(0, rootNode.getPropertyNames().count());
        Assert.assertEquals(0, rootNode.getChildCount());
    }

    @Test
    public void parseChildSlot() {
        // intentional whitespace
        TemplateNode rootNode = parse("<div> @child@</div>");

        Assert.assertEquals(2, rootNode.getChildCount());

        Assert.assertEquals(TextTemplateNode.class,
                rootNode.getChild(0).getClass());

        TemplateNode childSlot = rootNode.getChild(1);

        Assert.assertEquals(ChildSlotNode.class, childSlot.getClass());
    }

    @Test(expected = TemplateParseException.class)
    public void multipleChildSlots() {
        parse("<div>@child@<span>@child@</span></div>");
    }

    @Test
    public void parseTopComment() {
        ElementTemplateNode node = (ElementTemplateNode) parse(
                "<!-- comment --><div></div>");
        Assert.assertEquals(0, node.getChildCount());
        Assert.assertEquals("div", node.getTag());
    }

    @Test
    public void parseInnerComment() {
        ElementTemplateNode node = (ElementTemplateNode) parse(
                "<div> <!-- comment --> <input> </div>");
        Assert.assertEquals(4, node.getChildCount());
        Assert.assertEquals("div", node.getTag());
        Assert.assertEquals("input",
                ((ElementTemplateNode) node.getChild(2)).getTag());
    }

    @Test(expected = IllegalArgumentException.class)
    public void ngForElementAsRoot() {
        parse("<a class='item' *ngFor='let  item      of list'>{{item.text}}</a>");

    }

    @Test(expected = TemplateParseException.class)
    public void ngForElementMissingCollection() {
        parse("<div><a class='item' *ngFor='let item'>{{item.text}}</a></div>");
    }

    @Test
    public void ngForElement() {
        TemplateNode node = parse(
                "<div><a class='item' *ngFor='let  item      of list'>{{item.text}}</a></div>");
        ForTemplateNode forNode = (ForTemplateNode) node.getChild(0);
        Assert.assertEquals("list", forNode.getCollectionVariable());
        Assert.assertEquals("item", forNode.getLoopVariable());
    }

    @Test(expected = TemplateParseException.class)
    public void nestedNgForElement() {
        parse("<ul>" //
                + "  <li class='item' *ngFor='let  item      of list'>" //
                + "    <a  *ngFor='let  link      of item.links' [href]='link.href'>{{link.text}}</a>" //
                + "  </li>" //
                + "</ul>"); //
    }

    @Test
    public void parseEventHandler() {
        ElementTemplateNode node = (ElementTemplateNode) parse(
                "<button (click)='handle($event)'>");
        Assert.assertEquals(1, node.getEventNames().count());
        Optional<String> event = node.getEventNames().findAny();
        Assert.assertTrue(event.isPresent());
        Assert.assertEquals("click", event.get());

        Optional<String> eventHandler = node.getEventHandlerExpression("click");
        Assert.assertTrue(eventHandler.isPresent());
        Assert.assertEquals("handle($event)", eventHandler.get());
    }

    @Test(expected = TemplateParseException.class)
    public void parseWrongEventHandler() {
        parse("<button (click='handle($event)'>");
    }

    @Test
    public void parseMixedCaseAttributeStyle() {
        ElementTemplateNode node = (ElementTemplateNode) parse(
                "<button someTag='value'>");
        Assert.assertEquals(1, node.getAttributeNames().count());
        Assert.assertEquals("sometag",
                node.getAttributeNames().findFirst().get());
        Assert.assertEquals(0, node.getPropertyNames().count());
        Assert.assertEquals(0, node.getClassNames().count());

        Optional<BindingValueProvider> binding = node
                .getAttributeBinding("sometag");
        Assert.assertTrue(binding.isPresent());
        Assert.assertEquals("value", binding.get().getValue(null));

    }

    @Test
    public void parseStyle() {
        ElementTemplateNode node = (ElementTemplateNode) parse(
                "<button style='background-color:red'>");

        Assert.assertEquals(1, node.getAttributeNames().count());
        Assert.assertEquals(0, node.getPropertyNames().count());
        Assert.assertEquals(0, node.getClassNames().count());

        Optional<BindingValueProvider> binding = node
                .getAttributeBinding("style");
        Assert.assertTrue(binding.isPresent());
        Assert.assertEquals("background-color:red",
                binding.get().getValue(null));
    }

    @Test
    public void tempalteNodesReused() {
        String templateString = "<div></div>";

        TemplateNode a = TemplateParser.parse(templateString, null);
        TemplateNode b = TemplateParser.parse(new ByteArrayInputStream(
                templateString.getBytes(StandardCharsets.UTF_8)), null);

        Assert.assertSame(a, b);
    }

}
