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
package com.vaadin.flow.component.html;

import java.beans.IntrospectionException;
import java.util.Optional;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.ServletResourceDownloadHandler;

public class AnchorTest extends ComponentTest {

    private UI ui;

    @After
    public void tearDown() {
        ui = null;
        UI.setCurrent(null);
    }

    @Override
    public void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        whitelistProperty("download");
        super.setup();
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
        Assert.assertEquals(anchor.getHref(), "#");
        Assert.assertEquals(anchor.getElement().getText(), "Home");
        Assert.assertEquals(anchor.getText(), "Home");
    }

    @Test
    public void createWithTarget() {
        Anchor anchor = new Anchor("#", "Home");
        Assert.assertEquals(anchor.getTargetValue(), AnchorTarget.DEFAULT);
        Assert.assertEquals(anchor.getTarget(), Optional.empty());

        anchor.setTarget(AnchorTarget.BLANK);

        Assert.assertEquals(anchor.getTargetValue(), AnchorTarget.BLANK);
        Assert.assertEquals(anchor.getTarget(),
                Optional.of(AnchorTarget.BLANK.getValue()));

        Assert.assertEquals(anchor.getTargetValue(),
                new Anchor("#", "Home", AnchorTarget.BLANK).getTargetValue());
        Assert.assertEquals(anchor.getTarget(),
                new Anchor("#", "Home", AnchorTarget.BLANK).getTarget());
    }

    @Test
    public void shouldNotRemoveRouterIgnoreAttributeWhenRemoveHref() {
        Anchor anchor = new Anchor();
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.removeHref();

        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                anchor.getElement().getAttribute("router-ignore"));
    }

    @Test
    public void shouldNotBreakBehaviorIfSetHrefWhenHavingRouterIgnoreAttributeBefore() {
        Anchor anchor = new Anchor();
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.setHref("/logout");

        Assert.assertEquals(
                "Anchor element should have router-ignore " + "attribute", "",
                anchor.getElement().getAttribute("router-ignore"));
    }

    @Test
    public void setTargetValue_useEnum_targetIsSet() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTarget.PARENT);

        Assert.assertEquals(Optional.of(AnchorTarget.PARENT.getValue()),
                anchor.getTarget());
        Assert.assertEquals(AnchorTarget.PARENT, anchor.getTargetValue());
    }

    @Test
    public void setTargetValue_useObject_targetIsSet() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTargetValue.forString("foo"));

        Assert.assertEquals(Optional.of("foo"), anchor.getTarget());
        Assert.assertEquals("foo", anchor.getTargetValue().getValue());
    }

    @Test
    public void getTargetValue_useEnumStringValue_targetIsReturned() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTarget.SELF.getValue());

        Assert.assertEquals(Optional.of(AnchorTarget.SELF.getValue()),
                anchor.getTarget());
        Assert.assertEquals(AnchorTarget.SELF, anchor.getTargetValue());
    }

    @Test
    public void getTargetValue_useSomeStringValue_targetIsReturned() {
        Anchor anchor = new Anchor();
        anchor.setTarget("foo");

        Assert.assertEquals(Optional.of("foo"), anchor.getTarget());
        Assert.assertEquals("foo", anchor.getTargetValue().getValue());
    }

    // Other test methods in super class

    @Override
    protected void addProperties() {
        addStringProperty("href", "", false);
        addOptionalStringProperty("target");
        addProperty("routerIgnore", boolean.class, false, true, false, true,
                "router-ignore");
    }

    @Test
    @Override
    public void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    public void setEnabled_anchorWithoutHref_doesNotThrow() {
        Anchor anchor = new Anchor();
        anchor.setEnabled(false);

        anchor.setEnabled(true);
        Assert.assertTrue(anchor.isEnabled());

        anchor.setHref("foo");
        anchor.setEnabled(false);
        anchor.removeHref();
        anchor.setEnabled(true);
    }

    @Test
    public void disabledAnchor_removeHref_hrefIsEmpty() {
        Anchor anchor = new Anchor();
        anchor.setHref("foo");
        anchor.setEnabled(false);
        Assert.assertEquals("foo", anchor.getHref());
        anchor.setHref("bar");
        Assert.assertEquals("bar", anchor.getHref());
        anchor.removeHref();
        Assert.assertEquals("", anchor.getHref());
        anchor.setEnabled(true);
        Assert.assertEquals("", anchor.getHref());
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
        anchor.setEnabled(true);

        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
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
        Assert.assertEquals(href, anchor.getHref());

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

        anchor.setEnabled(true);

        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
        Assert.assertNotEquals(href, anchor.getHref());
    }

    @Test
    public void disabledAnchor_setDownload_hrefIsRemoved_enableAnchor_hrefIsRestored() {
        mockUI();
        DownloadHandler downloadHandler = event -> event.getWriter()
                .write("foo");
        Anchor anchor = new Anchor(downloadHandler, "bar");
        String href = anchor.getHref();
        anchor.setEnabled(false);

        Assert.assertFalse(anchor.getElement().hasAttribute("href"));
        Assert.assertEquals(href, anchor.getHref());

        anchor.setEnabled(true);
        Assert.assertEquals(href, anchor.getHref());
    }

    @Test
    public void disabledAnchor_setDownloadWhenDisabled_hrefIsPreserved() {
        mockUI();
        DownloadHandler downloadHandler = event -> event.getWriter()
                .write("foo");
        Anchor anchor = new Anchor(downloadHandler, "bar");
        String href = anchor.getHref();
        anchor.setEnabled(false);

        anchor.setHref(new AbstractStreamResource() {

            @Override
            public String getName() {
                return "baz";
            }
        });

        anchor.setEnabled(true);

        Assert.assertTrue(anchor.getElement().hasAttribute("href"));
        Assert.assertNotEquals(href, anchor.getHref());
    }

    @Test
    public void anchorWithDownloadHandler_downloadAttributeIsSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(downloadHandler, "bar");

        Assert.assertTrue(
                "Pre-built download handlers should set download attribute",
                anchor.isDownload());
    }

    @Test
    public void anchorWithDownloadAttributeSet_newHandler_downloadAttributeCleared() {
        mockUI();
        ServletResourceDownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(downloadHandler, "bar");

        Assert.assertTrue(
                "Pre-built download handlers should set download attribute",
                anchor.isDownload());

        downloadHandler.inline();

        anchor.setHref(downloadHandler);

        Assert.assertFalse(
                "Setting inline download handler should clear download attribute",
                anchor.isDownload());
    }

    @Test
    public void anchorWithDownloadAttributeSet_newCustomHandler_downloadAttributeNotTouched() {
        mockUI();
        Anchor anchor = new Anchor("/home", "bar");
        anchor.getElement().setAttribute("download", true);

        Assert.assertTrue(
                "Pre-built download handlers should set download attribute",
                anchor.isDownload());

        anchor.setHref(event -> event.getWriter().write("foo"));

        Assert.assertTrue(
                "Setting custom download handler should not clear download attribute",
                anchor.isDownload());
    }

    @Test
    public void anchorWithDownloadHandler_inlineSet_downloadAttributeIsNotSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path").inline();
        Anchor anchor = new Anchor(downloadHandler, "bar");

        Assert.assertFalse(
                "Inline download handlers should not add download attribute",
                anchor.isDownload());
    }

    @Test
    public void anchorWithLinkModeDownload_downloadAttributeIsSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path").inline();
        Anchor anchor = new Anchor(downloadHandler, "bar", LinkMode.DOWNLOAD);

        Assert.assertTrue(
                "Inline download handlers should not add download attribute",
                anchor.isDownload());
    }

    @Test
    public void anchorWithLinkModeInline_downloadAttributeIsNotSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(downloadHandler, "bar", LinkMode.INLINE);

        Assert.assertFalse(
                "Inline download handlers should not add download attribute",
                anchor.isDownload());
    }

    private void mockUI() {
        ui = new UI();
        UI.setCurrent(ui);
    }
}
