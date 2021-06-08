/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.html;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.AbstractStreamResource;

public class AnchorTest extends ComponentTest {

    private UI ui;

    @After
    public void tearDown() {
        ui = null;
        UI.setCurrent(null);
    }

    @Test
    public void removeHref() {
        Anchor anchor = new Anchor();
        anchor.setHref("foo");
        Assert.assertTrue(anchor.getElement().hasAttribute("href"));

        anchor.removeHref();
        Assert.assertFalse(anchor.getElement().hasAttribute("href"));
    }

    @Test
    public void createWithComponent() {
        Anchor anchor = new Anchor("#", new Text("Home"));
        Assert.assertEquals(anchor.getElement().getAttribute("href"), "#");
        Assert.assertEquals(anchor.getElement().getText(), "Home");
    }

    @Test
    public void disabledAnchor_hrefIsRemoved_enableAnchor_hrefIsRestored() {
        Anchor anchor = new Anchor("foo", "bar");
        anchor.setEnabled(false);

        Assert.assertFalse(anchor.getElement().hasAttribute("href"));

        anchor.setEnabled(true);
        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
        Assert.assertEquals("foo", anchor.getHref());
    }

    @Test
    public void disabledAnchor_setHrefWhenDisabled_enableAnchor_hrefIsPreserved() {
        Anchor anchor = new Anchor("foo", "bar");
        anchor.setEnabled(false);

        anchor.setHref("baz");

        anchor.setEnabled(true);

        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
        Assert.assertEquals("baz", anchor.getHref());
    }

    @Test
    public void disabledAnchor_setResourceWhenDisabled_enableAnchor_resourceIsPreserved() {
        Anchor anchor = new Anchor("foo", "bar");
        anchor.setEnabled(false);

        mockUI();
        anchor.setHref(new AbstractStreamResource() {

            @Override
            public String getName() {
                return "baz";
            }
        });
        String href = anchor.getHref();

        anchor.setEnabled(true);

        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
        Assert.assertEquals(href, anchor.getHref());
    }

    @Test
    public void disabledAnchor_setResource_hrefIsRemoved_enableAnchor_hrefIsRestored() {
        mockUI();
        AbstractStreamResource resource = new AbstractStreamResource() {

            @Override
            public String getName() {
                return "foo";
            }

        };
        Anchor anchor = new Anchor(resource, "bar");
        String href = anchor.getHref();
        anchor.setEnabled(false);

        Assert.assertFalse(anchor.getElement().hasAttribute("href"));

        anchor.setEnabled(true);
        Assert.assertEquals(href, anchor.getHref());
    }

    @Test
    public void disabledAnchor_setResourceWhenDisabled_hrefIsPreserved() {
        mockUI();
        AbstractStreamResource resource = new AbstractStreamResource() {

            @Override
            public String getName() {
                return "foo";
            }

        };
        Anchor anchor = new Anchor(resource, "bar");
        String href = anchor.getHref();
        anchor.setEnabled(false);

        anchor.setHref(new AbstractStreamResource() {

            @Override
            public String getName() {
                return "baz";
            }
        });

        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
        Assert.assertNotEquals(href, anchor.getHref());
    }

    @Test
    public void disabledAnchor_setResource_setHrefViaElementWhenDisabled_enableAnchor_hrefIsRestored() {
        mockUI();
        AbstractStreamResource resource = new AbstractStreamResource() {

            @Override
            public String getName() {
                return "foo";
            }

        };
        Anchor anchor = new Anchor(resource, "bar");
        anchor.setEnabled(false);

        anchor.getElement().setAttribute("href", "baz");

        anchor.setEnabled(true);
        Assert.assertEquals("baz", anchor.getHref());
    }

    // Other test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("href", "", false);
        addOptionalStringProperty("target");
    }

    private void mockUI() {
        ui = new UI();
        UI.setCurrent(ui);
    }

}
