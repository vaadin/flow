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

class VideoTest extends MediaTest {

    // The media cases are in the super class

    @Override
    protected void addProperties() {
        super.addProperties();
        addOptionalStringProperty("poster");
    }

    @Override
    protected Media createMedia(Source... sources) {
        return new Video(sources);
    }

    @Test
    void setPoster_downloadHandler_isInlineAndServedWhileDisabled() {
        Element element = mock(Element.class);
        class TestVideo extends Video {
            @Override
            public Element getElement() {
                return element;
            }
        }
        InputStreamDownloadHandler handler = DownloadHandler
                .fromInputStream(event -> DownloadResponse.error(500));
        assertFalse(handler.isInline());

        new TestVideo().setPoster(handler);

        assertTrue(handler.isInline(),
                "The poster is rendered in the page, so it must be served inline rather than as a download");
        ArgumentCaptor<DownloadHandler> captor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        verify(element).setAttribute(eq("poster"), captor.capture());
        assertEquals(DisabledUpdateMode.ALWAYS,
                captor.getValue().getDisabledUpdateMode(),
                "The browser fetches the poster while rendering, so it must be served even when the video is disabled");
    }
}
