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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.template.angular.ElementTemplateBuilder;
import com.vaadin.flow.template.angular.ElementTemplateNode;
import com.vaadin.flow.template.angular.StaticBindingValueProvider;
import com.vaadin.flow.template.angular.TemplateNode;
import com.vaadin.flow.template.angular.TextTemplateBuilder;
import com.vaadin.flow.template.angular.TextTemplateNode;

public class TemplateBuilderTest {
    @Test
    public void testBasicTemplate() {
        // <div baz="lorem" foo="bar">baz</div> where baz is an attribute and
        // foo a property
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("foo", new StaticBindingValueProvider("bar"))
                .setAttribute("baz", new StaticBindingValueProvider("lorem"))
                .setClassName("a-name",
                        new StaticBindingValueProvider("a-value"))
                .addChild(new TextTemplateBuilder(
                        new StaticBindingValueProvider("baz")));

        List<TemplateNode> nodes = builder.build(null);
        Assert.assertFalse(nodes.isEmpty());
        ElementTemplateNode node = (ElementTemplateNode) nodes.get(0);

        Assert.assertFalse(node.getParent().isPresent());
        Assert.assertEquals("div", node.getTag());

        Assert.assertArrayEquals(new String[] { "foo" },
                node.getPropertyNames().toArray());
        Assert.assertEquals("bar",
                node.getPropertyBinding("foo").get().getValue(null));

        Assert.assertArrayEquals(new String[] { "baz" },
                node.getAttributeNames().toArray());
        Assert.assertEquals("lorem",
                node.getAttributeBinding("baz").get().getValue(null));

        Assert.assertArrayEquals(new String[] { "a-name" },
                node.getClassNames().toArray());
        Assert.assertEquals("a-value",
                node.getClassNameBinding("a-name").get().getValue(null));

        Assert.assertEquals(1, node.getChildCount());

        TextTemplateNode child = (TextTemplateNode) node.getChild(0);
        Assert.assertSame(node, child.getParent().get());
        Assert.assertEquals("baz", child.getTextBinding().getValue(null));
        Assert.assertEquals(0, child.getChildCount());
    }
}
