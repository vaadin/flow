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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.signals.Signal;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cases that {@link Video} and {@link Audio} share, run for both of them
 * through the concrete subclasses of this class.
 */
abstract class MediaTest extends ComponentTest {

    private MockUI ui;

    @BeforeEach
    void setCurrentUi() {
        // adding a source from a DownloadHandler registers a resource in the
        // session of the current UI, which the mock UI becomes
        ui = new MockUI(
                new AlwaysLockedVaadinSession(new MockVaadinServletService()));
    }

    @AfterEach
    void tearDown() {
        CurrentInstance.clearAll();
    }

    @Override
    protected void addProperties() {
        // shown by default, and the generic cases expect the attribute only
        // for the non-default value; covered by its own test below
        whitelistProperty("controlsVisible");
        addProperty("autoplay", boolean.class, false, true, false, true);
        addProperty("loop", boolean.class, false, true, false, true);
        // a property rather than an attribute, and kept when set back to false
        // so that it unmutes a player that is already muted
        addProperty("muted", boolean.class, false, true, false, false);
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
    void controlsVisible_shownByDefault_canBeHiddenAndShownAgain() {
        // checked through both constructors, since a player without controls
        // gives the visitor no way to start it
        for (Media media : List.of((Media) getComponent(),
                createMedia(new Source("/intro.mp4", "video/mp4")))) {
            assertTrue(media.isControlsVisible());
            assertTrue(media.getElement().hasAttribute("controls"));

            media.setControlsVisible(false);
            assertFalse(media.isControlsVisible());
            assertFalse(media.getElement().hasAttribute("controls"));

            media.setControlsVisible(true);
            assertTrue(media.isControlsVisible());
            assertTrue(media.getElement().hasAttribute("controls"));
        }
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

    @Test
    void pauseAndSeek_sendTheCommandsToThePlayer() {
        Media media = createMedia();
        ui.add(media);

        media.pause();
        assertEquals(new JsCall(Media.PlaybackJs.class, "pause", List.of()),
                ui.onlyScheduledJsCall());

        media.seek(Duration.ofSeconds(90, 500_000_000));
        assertEquals(new JsCall(Media.PlaybackJs.class, "seek", List.of(90.5)),
                ui.onlyScheduledJsCall());

        Duration negative = Duration.ofMillis(-1);
        assertThrows(IllegalArgumentException.class,
                () -> media.seek(negative));
    }

    @Test
    void play_reportsWhatTheBrowserRefused_notAnInterruptionOrSuccess() {
        Media media = createMedia();
        ui.add(media);
        List<Media.PlaybackError> errors = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            media.play(errors::add);
        }
        List<PendingJavaScriptInvocation> plays = ui.dumpPendingJsInvocations();
        // the browser answers null for a playback that started or that pause()
        // interrupted
        plays.get(0).complete(JacksonUtils.nullNode());
        plays.get(1).complete(JacksonUtils.readTree("""
                {"name": "NotAllowedError", "message": "needs a gesture"}"""));
        plays.get(2).complete(JacksonUtils.readTree("""
                {"name": "NotSupportedError", "message": "no source"}"""));
        plays.get(3).complete(JacksonUtils.readTree("""
                {"name": "SecurityError"}"""));
        plays.get(4).completeExceptionally(
                JacksonUtils.createNode("bridge failed"));

        assertEquals(4, errors.size(), "Errors were: " + errors);
        assertEquals(List.of(
                new Media.PlaybackError(Media.PlaybackErrorCode.NOT_ALLOWED,
                        "needs a gesture"),
                new Media.PlaybackError(Media.PlaybackErrorCode.NOT_SUPPORTED,
                        "no source"),
                new Media.PlaybackError(Media.PlaybackErrorCode.UNKNOWN, "")),
                errors.subList(0, 3));
        assertEquals(Media.PlaybackErrorCode.UNKNOWN, errors.get(3).code());
        assertTrue(errors.get(3).message().contains("bridge failed"),
                errors.get(3).message());
    }

    @Test
    void playbackStateSignal_followsThePlayerWhileAttached() {
        Media media = createMedia();
        Signal<Media.PlaybackState> state = media.playbackStateSignal();
        assertSame(state, media.playbackStateSignal());
        assertEquals(Media.PlaybackState.UNKNOWN, state.peek());

        // a player that appears is asked for its state, since it may already
        // be playing
        ui.add(media);
        ui.dumpPendingJsInvocations().get(0)
                .complete(JacksonUtils.createNode("PLAYING"));
        assertEquals(Media.PlaybackState.PLAYING, state.peek());

        String reported = "ENDED";
        for (String eventType : List.of("play", "pause", "ended", "emptied",
                "seeked")) {
            reported = reported.equals("ENDED") ? "PAUSED" : "ENDED";
            fireWithPlaybackState(media, eventType, reported);
            assertEquals(Media.PlaybackState.valueOf(reported), state.peek(),
                    "after " + eventType);
        }

        ui.remove(media);
        assertEquals(Media.PlaybackState.UNKNOWN, state.peek());
    }

    @Test
    void playbackStateSignal_askedForWhileAttached_readsTheState_ignoresAnAnswerAfterDetach() {
        Media media = createMedia();
        ui.add(media);
        ui.dumpPendingJsInvocations();

        Signal<Media.PlaybackState> state = media.playbackStateSignal();
        ui.dumpPendingJsInvocations().get(0)
                .complete(JacksonUtils.createNode("PAUSED"));
        assertEquals(Media.PlaybackState.PAUSED, state.peek());

        ui.remove(media);
        ui.add(media);
        PendingJavaScriptInvocation read = ui.dumpPendingJsInvocations().get(0);
        ui.remove(media);
        read.complete(JacksonUtils.createNode("PLAYING"));
        assertEquals(Media.PlaybackState.UNKNOWN, state.peek());
    }

    private static void fireWithPlaybackState(Media media, String eventType,
            String reported) {
        String key = ElementListenerMap.getEventDataKey(
                JsSnippets.of(Media.PlaybackStateJs.class).readPlaybackState());
        media.getElement().getNode().getFeature(ElementListenerMap.class)
                .fireEvent(new DomEvent(media.getElement(), eventType,
                        JacksonUtils.createObjectNode().put(key, reported)));
    }
}
