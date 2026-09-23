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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SourceTest extends ComponentTest {

    // Property cases in the super class

    @Override
    protected void addProperties() {
        addStringProperty("src", "");
        addOptionalStringProperty("type");
    }

    @Test
    void constructor_setsSrcAndType() {
        Source source = new Source("/intro.mp4", "video/mp4");

        assertEquals("/intro.mp4", source.getSrc());
        assertEquals("video/mp4", source.getType().orElse(null));
    }

    @Test
    void setSrc_downloadHandler_isInlineAndServedWhileDisabled() {
        Element element = mock(Element.class);
        class TestSource extends Source {
            @Override
            public Element getElement() {
                return element;
            }
        }
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        assertFalse(handler.isInline());

        new TestSource().setSrc(handler);

        assertTrue(handler.isInline(),
                "The media file is played in the page, so it must be served inline rather than as a download");
        ArgumentCaptor<DownloadHandler> captor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        verify(element).setAttribute(eq("src"), captor.capture());
        assertEquals(DisabledUpdateMode.ALWAYS,
                captor.getValue().getDisabledUpdateMode(),
                "The browser fetches the media file while rendering, so it must be served even when the player is disabled");
    }
}
