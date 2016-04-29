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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.router.Location;
import com.vaadin.hummingbird.router.Router;
import com.vaadin.hummingbird.router.ViewRendererTest.TestView;
import com.vaadin.ui.Template;
import com.vaadin.ui.UI;

public class TemplateTest {
    /**
     * Template initialized from HTML passed to the constructor.
     */
    public static class TestTemplate extends Template {
        public TestTemplate(String templateString) {
            super(new ByteArrayInputStream(
                    templateString.getBytes(Charset.forName("UTF-8"))));
        }
    }

    public static class TemplateParentView extends TestTemplate {
        public TemplateParentView() {
            super("<div><h1>Header</h1>@child@</div>");
        }
    }

    @Test
    public void useTemplateAsParentView() {
        Router router = new Router();
        router.reconfigure(c -> {
            c.setRoute("", TestView.class, TemplateParentView.class);
            c.setRoute("empty", TemplateParentView.class);
        });

        UI ui = new UI();
        router.navigate(ui, new Location(""));

        Assert.assertEquals(
                Arrays.asList(TestView.class, TemplateParentView.class),
                ui.getActiveViewChain().stream().map(Object::getClass)
                        .collect(Collectors.toList()));
        Element uiContent = ui.getElement().getChild(0);

        Assert.assertEquals("div", uiContent.getTag());

        Assert.assertEquals(2, uiContent.getChildCount());
        Assert.assertEquals("h1", uiContent.getChild(0).getTag());
        Assert.assertEquals("div", uiContent.getChild(1).getTag());

        router.navigate(ui, new Location("empty"));

        Assert.assertEquals(Arrays.asList(TemplateParentView.class),
                ui.getActiveViewChain().stream().map(Object::getClass)
                        .collect(Collectors.toList()));

        Assert.assertEquals(1, uiContent.getChildCount());
        Assert.assertEquals("h1", uiContent.getChild(0).getTag());
    }
}
