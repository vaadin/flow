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

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.streams.DownloadEvent;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SourceTest extends ComponentTest {

    @Override
    protected void addProperties() {
        // null means not set, which the generic String property cases reject
        whitelistProperty("src");
        whitelistProperty("type");
    }

    @Test
    void constructor_setsSrcAndType() {
        Source source = new Source("/intro.mp4", "video/mp4");

        assertEquals("/intro.mp4", source.getSrc());
        assertEquals("video/mp4", source.getType());
    }

    @Test
    void srcAndType_nullUntilSet_nullRemovesTheAttributes() {
        Source source = new Source();
        assertNull(source.getSrc());
        assertNull(source.getType());

        source.setSrc("/intro.mp4");
        source.setType("video/mp4");
        source.setSrc((String) null);
        source.setType(null);

        assertNull(source.getSrc());
        assertNull(source.getType());
        assertFalse(source.getElement().hasAttribute("src"));
        assertFalse(source.getElement().hasAttribute("type"));
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

    @Test
    void setSrc_rangeRequestedFromHandlerWithoutRanges_warnsOnceInDevelopmentMode()
            throws Throwable {
        DownloadHandler served = serveFrom(new NamedHandler(false));

        List<String> warnings = captureWarnings(() -> {
            served.handleDownloadRequest(createEvent("bytes=0-", false));
            served.handleDownloadRequest(createEvent("bytes=4096-", false));
        });

        assertEquals(List.of("The media file 'lake.webm'"), warnings);
    }

    @Test
    void setSrc_handlerWithoutName_warningNamesTheClass() throws Throwable {
        DownloadHandler lambda = serveFrom(event -> {
        });
        DownloadHandler unnamed = serveFrom(new UnnamedHandler());

        List<String> warnings = captureWarnings(() -> {
            lambda.handleDownloadRequest(createEvent("bytes=0-", false));
            unnamed.handleDownloadRequest(createEvent("bytes=0-", false));
        });

        assertEquals(List.of(
                "A media file served by a lambda in "
                        + SourceTest.class.getName(),
                "A media file served by " + UnnamedHandler.class.getName()),
                warnings);
    }

    @Test
    void setSrc_rangeAnsweredNotRequestedOrProductionMode_doesNotWarn()
            throws Throwable {
        NamedHandler enabledAfterSetSrc = new NamedHandler(false);
        DownloadHandler rangesEnabledLater = serveFrom(enabledAfterSetSrc);
        enabledAfterSetSrc.rangeRequestsEnabled = true;
        DownloadHandler withoutRanges = serveFrom(new NamedHandler(false));

        List<String> warnings = captureWarnings(() -> {
            rangesEnabledLater
                    .handleDownloadRequest(createEvent("bytes=0-", false));
            withoutRanges.handleDownloadRequest(createEvent(null, false));
            withoutRanges.handleDownloadRequest(createEvent("bytes=0-", true));
        });

        assertEquals(List.of(), warnings);
    }

    /**
     * Sets the handler as the source of a media file and returns the handler
     * that the browser is served by.
     */
    private static DownloadHandler serveFrom(DownloadHandler handler) {
        Element element = mock(Element.class);
        new Source() {
            @Override
            public Element getElement() {
                return element;
            }
        }.setSrc(handler);
        ArgumentCaptor<DownloadHandler> captor = ArgumentCaptor
                .forClass(DownloadHandler.class);
        verify(element).setAttribute(eq("src"), captor.capture());
        return captor.getValue();
    }

    private static DownloadEvent createEvent(@Nullable String range,
            boolean productionMode) {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(productionMode);
        VaadinService service = mock(VaadinService.class);
        when(service.getDeploymentConfiguration()).thenReturn(configuration);
        VaadinRequest request = mock(VaadinRequest.class);
        when(request.getHeader("Range")).thenReturn(range);
        when(request.getService()).thenReturn(service);
        DownloadEvent event = mock(DownloadEvent.class);
        when(event.getRequest()).thenReturn(request);
        return event;
    }

    /**
     * Runs the action and returns how each warning that it logged names the
     * media file.
     */
    private static List<String> captureWarnings(Executable action)
            throws Throwable {
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(Source.class))
                    .thenReturn(logger);
            action.execute();
        }
        ArgumentCaptor<Object> mediaFiles = ArgumentCaptor
                .forClass(Object.class);
        verify(logger, atLeast(0)).warn(anyString(), mediaFiles.capture());
        verifyNoMoreInteractions(logger);
        return mediaFiles.getAllValues().stream().map(String::valueOf).toList();
    }

    private static class NamedHandler implements DownloadHandler {
        private boolean rangeRequestsEnabled;

        private NamedHandler(boolean rangeRequestsEnabled) {
            this.rangeRequestsEnabled = rangeRequestsEnabled;
        }

        @Override
        public void handleDownloadRequest(DownloadEvent event) {
            // the content does not matter, only how it is requested
        }

        @Override
        public String getUrlPostfix() {
            return "lake.webm";
        }

        @Override
        public boolean isRangeRequestsEnabled() {
            return rangeRequestsEnabled;
        }
    }

    private static class UnnamedHandler implements DownloadHandler {
        @Override
        public void handleDownloadRequest(DownloadEvent event) {
            // the content does not matter, only how it is requested
        }
    }
}
