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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

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
        assertFalse(nodes.isEmpty());
        ElementTemplateNode node = (ElementTemplateNode) nodes.get(0);

        assertFalse(node.getParent().isPresent());
        assertEquals("div", node.getTag());

        assertArrayEquals(new String[] { "foo" },
                node.getPropertyNames().toArray());
        assertEquals("bar",
                node.getPropertyBinding("foo").get().getValue(null));

        assertArrayEquals(new String[] { "baz" },
                node.getAttributeNames().toArray());
        assertEquals("lorem",
                node.getAttributeBinding("baz").get().getValue(null));

        assertArrayEquals(new String[] { "a-name" },
                node.getClassNames().toArray());
        assertEquals("a-value",
                node.getClassNameBinding("a-name").get().getValue(null));

        assertEquals(1, node.getChildCount());

        TextTemplateNode child = (TextTemplateNode) node.getChild(0);
        assertSame(node, child.getParent().get());
        assertEquals("baz", child.getTextBinding().getValue(null));
        assertEquals(0, child.getChildCount());
    }
}
