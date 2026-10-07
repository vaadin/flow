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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cases that {@link Video} and {@link Audio} share, run for both of them
 * through the concrete subclasses of this class.
 */
abstract class MediaTest extends ComponentTest {

    @BeforeEach
    void setCurrentUi() {
        // adding a source from a DownloadHandler registers a resource in the
        // session of the current UI, which the mock UI becomes
        new MockUI(
                new AlwaysLockedVaadinSession(new MockVaadinServletService()));
    }

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

    @Override
    protected void addProperties() {
        addProperty("controls", boolean.class, false, true, false, true);
        addProperty("autoplay", boolean.class, false, true, false, true);
        addProperty("loop", boolean.class, false, true, false, true);
        addProperty("muted", boolean.class, false, true, false, true);
        addProperty("preload", Media.Preload.class, null,
                Media.Preload.METADATA, false, true);
    }

    @Test
    @Override
    protected void testHasAriaLabelIsImplemented() {
        super.testHasAriaLabelIsImplemented();
    }

    /**
     * Creates the component under test with the given sources, through the
     * varargs constructor that both media components expose.
     */
    protected abstract Media createMedia(Source... sources);

    @Test
    void addSource_appendsSourcesInTheOrderTheyWereAdded() {
        Media media = (Media) getComponent();

        Source webm = media.addSource("/intro.webm", "video/webm");
        Source mp4 = media.addSource("/intro.mp4", "video/mp4");
        // fallback content is a child too, but not a source
        media.add(new Paragraph("Your browser cannot play this recording."));

        assertEquals(List.of(webm, mp4), media.getSources());
        assertEquals("/intro.webm", webm.getSrc());
        assertEquals("video/webm", webm.getType());
        assertSame(media, mp4.getParent().orElse(null));
    }

    @Test
    void addSource_downloadHandler_srcPointsAtTheHandler() {
        Media media = (Media) getComponent();

        Source source = media.addSource(
                event -> event.getWriter().write("media bytes"), "audio/mpeg");

        assertTrue(source.getSrc().startsWith("VAADIN/dynamic/resource/-1/"),
                "The source should be served from a dynamic resource, was "
                        + source.getSrc());
        assertEquals("audio/mpeg", source.getType());
    }

    @Test
    void sourcesConstructor_addsTheSources() {
        Source source = new Source("/intro.mp4", "video/mp4");

        Media media = createMedia(source);

        assertEquals(List.of(source), media.getSources());
    }

    @Test
    void getPreload_matchesTheKeywordRegardlessOfCase() {
        Media media = (Media) getComponent();

        // preload is an HTML enumerated attribute, so its keywords are matched
        // ASCII case-insensitively
        media.getElement().setAttribute("preload", "NONE");
        assertEquals(Media.Preload.NONE, media.getPreload());

        media.getElement().setAttribute("preload", "Metadata");
        assertEquals(Media.Preload.METADATA, media.getPreload());
    }
}
