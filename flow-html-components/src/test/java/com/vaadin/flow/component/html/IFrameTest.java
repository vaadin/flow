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

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IFrameTest extends ComponentTest {

    // Actual test methods mostly in super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
        addOptionalStringProperty("srcdoc");
        addOptionalStringProperty("name");
        addOptionalStringProperty("allow");

        addProperty("importance", IFrame.ImportanceType.class,
                IFrame.ImportanceType.AUTO, IFrame.ImportanceType.HIGH, true,
                true);

        addProperty("sandbox", IFrame.SandboxType[].class, null,
                new IFrame.SandboxType[] { IFrame.SandboxType.ALLOW_POPUPS,
                        IFrame.SandboxType.ALLOW_MODALS },
                true, true);
    }

    @Test
    void reload() throws Exception {
        Element element = Mockito.mock(Element.class);
        IFrame iframe = new IFrame();
        Field f = Component.class.getDeclaredField("element");

        f.setAccessible(true);
        f.set(iframe, element);

        iframe.reload();

        Mockito.verify(element).executeJs("this.src = this.src");
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    @Test
    void setSrc_downloadHandler_disabledUpdateModeIsAlways() {
        Element element = Mockito.mock(Element.class);
        class TestIFrame extends IFrame {
            @Override
            public Element getElement() {
                return element;
            }
        }
        // Plain lambda DownloadHandler, not an AbstractDownloadHandler subclass
        DownloadHandler lambda = event -> {
        };

        new TestIFrame().setSrc(lambda);

        ArgumentCaptor<DownloadHandler> captor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        Mockito.verify(element).setAttribute(Mockito.eq("src"),
                captor.capture());
        assertEquals(DisabledUpdateMode.ALWAYS,
                captor.getValue().getDisabledUpdateMode());
    }

    @Test
    void downloadHandler_isSetToInline() {
        Element element = Mockito.mock(Element.class);
        class TestIFrame extends IFrame {
            public TestIFrame(DownloadHandler downloadHandler) {
                super(downloadHandler);
            }

            @Override
            public Element getElement() {
                return element;
            }
        }
        // dummy handler
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        assertFalse(handler.isInline());
        new TestIFrame(handler);
        assertTrue(handler.isInline());
    }

    @Test
    void setSrc_unsafeScheme_throws() {
        IFrame iframe = new IFrame();
        assertThrows(IllegalArgumentException.class,
                () -> iframe.setSrc("javascript:alert(1)"));
    }

    @Test
    void setUnsafeSrc_unsafeScheme_setsSrcWithoutValidation() {
        IFrame iframe = new IFrame();
        iframe.setUnsafeSrc("javascript:alert(1)");
        assertEquals("javascript:alert(1)", iframe.getSrc());
    }
}
