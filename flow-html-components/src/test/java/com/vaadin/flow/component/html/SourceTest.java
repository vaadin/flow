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

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.DisabledUpdateMode;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.MockLogger;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class SourceTest extends ComponentTest {

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

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
                .fromInputStream(event -> DownloadResponse.error(500))
                .enableRangeRequests();
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
    void setSrc_handlerWithoutRanges_warnsOncePerMediaFileInDevelopmentMode() {
        UI ui = createUi(false);
        MockLogger logger = new MockLogger();

        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(Source.class))
                    .thenReturn(logger);
            for (int i = 0; i < 2; i++) {
                ui.add(new Source(DownloadHandler.fromInputStream(
                        event -> DownloadResponse.error(500), "lake.webm"),
                        "video/webm"));
            }
            ui.add(new Source(
                    DownloadHandler.fromInputStream(
                            event -> DownloadResponse.error(500),
                            "harbour.webm").enableRangeRequests(),
                    "video/webm"));
            ui.add(new Source(DownloadHandler.forFile(new File("forest.mp4")),
                    "video/mp4"));
            ui.add(new Source(event -> event.getResponse().setStatus(500),
                    "video/mp4"));
        }

        String logs = logger.getLogs();
        assertEquals(2,
                logs.lines().filter(line -> line.startsWith(MockLogger.WARN))
                        .count(),
                "Only the handlers without ranges should be warned about, each only once, logs were "
                        + logs);
        assertTrue(logs.contains("'lake.webm'"),
                "The warning should name the media file, logs were " + logs);
        assertTrue(
                logs.contains("served by a lambda in "
                        + SourceTest.class.getName() + " does not"),
                "The warning should name the class that declares the lambda, logs were "
                        + logs);
    }

    @Test
    void setSrc_handlerWithoutRanges_productionMode_doesNotWarn() {
        UI ui = createUi(true);
        MockLogger logger = new MockLogger();

        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class)) {
            loggerFactory.when(() -> LoggerFactory.getLogger(Source.class))
                    .thenReturn(logger);
            ui.add(new Source(
                    DownloadHandler.fromInputStream(
                            event -> DownloadResponse.error(500), "meadow.ogg"),
                    "audio/ogg"));
        }

        assertEquals("", logger.getLogs());
    }

    private static UI createUi(boolean productionMode) {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setProductionMode(productionMode);
        return new MockUI(new AlwaysLockedVaadinSession(
                new MockVaadinServletService(configuration)));
    }
}
