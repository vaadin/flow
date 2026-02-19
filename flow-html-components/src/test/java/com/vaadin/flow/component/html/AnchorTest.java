/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.ServletResourceDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnchorTest extends ComponentTest {

    private UI ui;

    @AfterEach
    void tearDown() {
        ui = null;
        UI.setCurrent(null);
    }

    @BeforeEach
    @Override
    void setup() throws IntrospectionException, InstantiationException,
            IllegalAccessException, ClassNotFoundException,
            InvocationTargetException, NoSuchMethodException {
        whitelistProperty("download");
        super.setup();
    }

    @Test
    void removeHref() {
        Anchor anchor = new Anchor();
        anchor.setHref("foo");
        assertTrue(anchor.getElement().hasAttribute("href"));

        anchor.removeHref();
        assertFalse(anchor.getElement().hasAttribute("href"));
    }

    @Test
    void createWithComponent() {
        Anchor anchor = new Anchor("#", new Text("Home"));
        assertEquals(anchor.getElement().getAttribute("href"), "#");
        assertEquals(anchor.getHref(), "#");
        assertEquals(anchor.getElement().getText(), "Home");
        assertEquals(anchor.getText(), "Home");
    }

    @Test
    void createWithTarget() {
        Anchor anchor = new Anchor("#", "Home");
        assertEquals(anchor.getTargetValue(), AnchorTarget.DEFAULT);
        assertEquals(anchor.getTarget(), Optional.empty());

        anchor.setTarget(AnchorTarget.BLANK);

        assertEquals(anchor.getTargetValue(), AnchorTarget.BLANK);
        assertEquals(anchor.getTarget(),
                Optional.of(AnchorTarget.BLANK.getValue()));

        assertEquals(anchor.getTargetValue(),
                new Anchor("#", "Home", AnchorTarget.BLANK).getTargetValue());
        assertEquals(anchor.getTarget(),
                new Anchor("#", "Home", AnchorTarget.BLANK).getTarget());
    }

    @Test
    void shouldNotRemoveRouterIgnoreAttributeWhenRemoveHref() {
        Anchor anchor = new Anchor();
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.removeHref();

        assertEquals("", anchor.getElement().getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
    }

    @Test
    void shouldNotBreakBehaviorIfSetHrefWhenHavingRouterIgnoreAttributeBefore() {
        Anchor anchor = new Anchor();
        anchor.getElement().setAttribute("router-ignore", true);
        anchor.setHref("/logout");

        assertEquals("", anchor.getElement().getAttribute("router-ignore"),
                "Anchor element should have router-ignore " + "attribute");
    }

    @Test
    void setTargetValue_useEnum_targetIsSet() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTarget.PARENT);

        assertEquals(Optional.of(AnchorTarget.PARENT.getValue()),
                anchor.getTarget());
        assertEquals(AnchorTarget.PARENT, anchor.getTargetValue());
    }

    @Test
    void setTargetValue_useObject_targetIsSet() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTargetValue.forString("foo"));

        assertEquals(Optional.of("foo"), anchor.getTarget());
        assertEquals("foo", anchor.getTargetValue().getValue());
    }

    @Test
    void getTargetValue_useEnumStringValue_targetIsReturned() {
        Anchor anchor = new Anchor();
        anchor.setTarget(AnchorTarget.SELF.getValue());

        assertEquals(Optional.of(AnchorTarget.SELF.getValue()),
                anchor.getTarget());
        assertEquals(AnchorTarget.SELF, anchor.getTargetValue());
    }

    @Test
    void getTargetValue_useSomeStringValue_targetIsReturned() {
        Anchor anchor = new Anchor();
        anchor.setTarget("foo");

        assertEquals(Optional.of("foo"), anchor.getTarget());
        assertEquals("foo", anchor.getTargetValue().getValue());
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
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    void setEnabled_anchorWithoutHref_doesNotThrow() {
        Anchor anchor = new Anchor();
        anchor.setEnabled(false);

        anchor.setEnabled(true);
        assertTrue(anchor.isEnabled());

        anchor.setHref("foo");
        anchor.setEnabled(false);
        anchor.removeHref();
        anchor.setEnabled(true);
    }

    @Test
    void disabledAnchor_removeHref_hrefIsEmpty() {
        Anchor anchor = new Anchor();
        anchor.setHref("foo");
        anchor.setEnabled(false);
        assertEquals("foo", anchor.getHref());
        anchor.setHref("bar");
        assertEquals("bar", anchor.getHref());
        anchor.removeHref();
        assertEquals("", anchor.getHref());
        anchor.setEnabled(true);
        assertEquals("", anchor.getHref());
    }

    @Test
    void disabledAnchor_hrefIsRemoved_enableAnchor_hrefIsRestored() {
        Anchor anchor = new Anchor("foo", "bar");
        anchor.setEnabled(false);

        assertFalse(anchor.getElement().hasAttribute("href"));

        anchor.setEnabled(true);
        assertTrue(anchor.getElement().hasAttribute("href"));
        assertEquals("foo", anchor.getHref());
    }

    @Test
    void disabledAnchor_setHrefWhenDisabled_enableAnchor_hrefIsPreserved() {
        Anchor anchor = new Anchor("foo", "bar");
        anchor.setEnabled(false);

        anchor.setHref("baz");

        anchor.setEnabled(true);

        assertTrue(anchor.getElement().hasAttribute("href"));
        assertEquals("baz", anchor.getHref());
    }

    @Test
    void disabledAnchor_setResourceWhenDisabled_enableAnchor_resourceIsPreserved() {
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

        assertTrue(anchor.getElement().hasAttribute("href"));
    }

    @Test
    void disabledAnchor_setResource_hrefIsRemoved_enableAnchor_hrefIsRestored() {
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

        assertFalse(anchor.getElement().hasAttribute("href"));
        assertEquals(href, anchor.getHref());

        anchor.setEnabled(true);
        assertEquals(href, anchor.getHref());
    }

    @Test
    void disabledAnchor_setResourceWhenDisabled_hrefIsPreserved() {
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

        assertTrue(anchor.getElement().hasAttribute("href"));
        assertNotEquals(href, anchor.getHref());
    }

    @Test
    void disabledAnchor_setDownload_hrefIsRemoved_enableAnchor_hrefIsRestored() {
        mockUI();
        DownloadHandler downloadHandler = event -> event.getWriter()
                .write("foo");
        Anchor anchor = new Anchor(downloadHandler, "bar");
        String href = anchor.getHref();
        anchor.setEnabled(false);

        assertFalse(anchor.getElement().hasAttribute("href"));
        assertEquals(href, anchor.getHref());

        anchor.setEnabled(true);
        assertEquals(href, anchor.getHref());
    }

    @Test
    void disabledAnchor_setDownloadWhenDisabled_hrefIsPreserved() {
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

        assertTrue(anchor.getElement().hasAttribute("href"));
        assertNotEquals(href, anchor.getHref());
    }

    @Test
    void anchorWithDownloadHandler_downloadAttributeIsSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(downloadHandler, "bar");

        assertTrue(anchor.isDownload(),
                "Pre-built download handlers should set download attribute");
    }

    @Test
    void anchorWithDownloadAttributeSet_newHandler_downloadAttributeCleared() {
        mockUI();
        ServletResourceDownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(downloadHandler, "bar");

        assertTrue(anchor.isDownload(),
                "Pre-built download handlers should set download attribute");

        downloadHandler.inline();

        anchor.setHref(downloadHandler);

        assertFalse(anchor.isDownload(),
                "Setting inline download handler should clear download attribute");
    }

    @Test
    void anchorWithDownloadAttributeSet_newCustomHandler_downloadAttributeNotTouched() {
        mockUI();
        Anchor anchor = new Anchor("/home", "bar");
        anchor.getElement().setAttribute("download", true);

        assertTrue(anchor.isDownload(),
                "Pre-built download handlers should set download attribute");

        anchor.setHref(event -> event.getWriter().write("foo"));

        assertTrue(anchor.isDownload(),
                "Setting custom download handler should not clear download attribute");
    }

    @Test
    void anchorWithDownloadHandler_inlineSet_downloadAttributeIsNotSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path").inline();
        Anchor anchor = new Anchor(downloadHandler, "bar");

        assertFalse(anchor.isDownload(),
                "Inline download handlers should not add download attribute");
    }

    @Test
    void anchorWithLinkModeDownload_downloadAttributeIsSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path").inline();
        Anchor anchor = new Anchor(downloadHandler, AttachmentType.DOWNLOAD,
                "bar");

        assertTrue(anchor.isDownload(),
                "Inline download handlers should not add download attribute");
    }

    @Test
    void anchorWithLinkModeInline_downloadAttributeIsNotSet() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(downloadHandler, AttachmentType.INLINE,
                "bar");

        assertFalse(anchor.isDownload(),
                "Inline download handlers should not add download attribute");
    }

    @Test
    void customDownloadHandler_constructorSetsDownloadMode() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(event -> {
        }, "bar");

        assertTrue(anchor.isDownload(),
                "Custom download handlers should by default add download attribute");
    }

    @Test
    void customDownloadHandler_nullType_constructorSetsDownloadMode() {
        mockUI();
        DownloadHandler downloadHandler = DownloadHandler
                .forServletResource("null/path");
        Anchor anchor = new Anchor(event -> {
        }, null, "bar");

        assertTrue(anchor.isDownload(),
                "Custom download handlers should by default add download attribute");
    }

    private void mockUI() {
        ui = new UI();
        UI.setCurrent(ui);
    }
}
