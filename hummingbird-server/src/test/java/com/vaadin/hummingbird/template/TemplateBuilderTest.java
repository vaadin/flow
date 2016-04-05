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
        // <div foo="bar">baz</div>
        ElementTemplateBuilder builder = new ElementTemplateBuilder("div")
                .addAttribute("foo", new StaticBinding("bar"))
                .addChild(new TextTemplateBuilder(new StaticBinding("baz")));

        ElementTemplateNode node = builder.build();
        node.init(null);

        Assert.assertNull(node.getParent());
        Assert.assertEquals("div", node.getTag());

        Assert.assertArrayEquals(new String[] { "foo" },
                node.getAttributeNames().toArray());
        Assert.assertEquals("bar",
                node.getAttributeBinding("foo").get().getValue(null));

        Assert.assertEquals(1, node.getChildCount());

        TextTemplateNode child = (TextTemplateNode) node.getChild(0);
        Assert.assertSame(node, child.getParent());
        Assert.assertEquals("baz", child.getTextBinding().getValue(null));
        Assert.assertEquals(0, child.getChildCount());
    }
}
