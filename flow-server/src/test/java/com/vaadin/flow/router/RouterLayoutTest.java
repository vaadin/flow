/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.router;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.ComponentTest;
import com.vaadin.flow.dom.Element;

public class RouterLayoutTest {

    private static final String NEW_ID = "newId";
    private TestRouterLayout testRouterLayout;

    @Before
    public void setup() {
        testRouterLayout = new TestRouterLayout();
    }

    @Test
    public void show_nonNull_childrenUpdated() {
        Assert.assertEquals(0, testRouterLayout.getElement().getChildCount());

        ComponentTest.TestDiv content = new ComponentTest.TestDiv();
        testRouterLayout.showRouterLayoutContent(content);

        Assert.assertEquals(1, testRouterLayout.getElement().getChildCount());

        ComponentTest.TestDiv newContent = new ComponentTest.TestDiv();
        newContent.setId(NEW_ID);
        content.getElement().removeFromParent();
        testRouterLayout.showRouterLayoutContent(newContent);

        Assert.assertEquals(1, testRouterLayout.getElement().getChildCount());
        Assert.assertSame(NEW_ID,
                testRouterLayout.getElement().getChild(0).getAttribute("id"));
    }

    @Test
    public void show_null_noChildren() {
        testRouterLayout.showRouterLayoutContent(null);
        Assert.assertEquals(0, testRouterLayout.getElement().getChildCount());
    }

    @Test
    public void remove_removesContent() {
        ComponentTest.TestDiv content = new ComponentTest.TestDiv();
        testRouterLayout.element.appendChild(content.getElement());

        testRouterLayout.removeRouterLayoutContent(content);

        Assert.assertEquals(0, testRouterLayout.getElement().getChildCount());
    }

    private static class TestRouterLayout implements RouterLayout {
        private final Element element = new Element("span");

        @Override
        public Element getElement() {
            return element;
        }
    }
}
