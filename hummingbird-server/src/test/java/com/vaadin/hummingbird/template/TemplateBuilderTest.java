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

public class TemplateBuilderTest {
    @Test
    public void testBasicTemplate() {
        // <div baz="lorem" foo="bar">baz</div> where baz is an attribute and
        // foo a property
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .setProperty("foo", new StaticBinding("bar"))
                .setAttribute("baz", new StaticBinding("lorem"))
                .addChild(new TextTemplateBuilder(new StaticBinding("baz")));

        ElementTemplateNode node = builder.build(null);

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

        Assert.assertEquals(1, node.getChildCount());

        TextTemplateNode child = (TextTemplateNode) node.getChild(0);
        Assert.assertSame(node, child.getParent().get());
        Assert.assertEquals("baz", child.getTextBinding().getValue(null));
        Assert.assertEquals(0, child.getChildCount());
    }
}
