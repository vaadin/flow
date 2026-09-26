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
import java.util.Optional;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HasComponentsOfType;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Base class for the two media players the browser has built in: {@link Video}
 * (<code>&lt;video&gt;</code>) and {@link Audio} (<code>&lt;audio&gt;</code>).
 * Everything that does not depend on there being a picture — the playback
 * attributes and the list of {@link Source sources} — lives here.
 * <p>
 * A media component plays one recording, offered in as many file formats as you
 * care to provide. Each format is a source of its own, and the browser plays
 * the first one it can decode, which is why two or three sources are the normal
 * case: no single format plays everywhere.
 *
 * <pre>
 * Video video = new Video();
 * video.addSource("/intro.webm", "video/webm");
 * video.addSource("/intro.mp4", "video/mp4");
 * video.setControls(true);
 * </pre>
 *
 * Only {@code <source>} components can be added as children, because that is
 * all the API needs to build a player. An application that wants the fallback
 * content or the subtitle tracks that the elements also accept has to add those
 * through {@link #getElement()}.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Guides/Audio_and_video_delivery">MDN:
 *      Audio and video delivery</a>
 */
@NullMarked
public abstract class Media extends HtmlComponent
        implements HasComponentsOfType<Source>, HasAriaLabel {

    private static final String AUTOPLAY_ATTRIBUTE = "autoplay";
    private static final String CONTROLS_ATTRIBUTE = "controls";
    private static final String LOOP_ATTRIBUTE = "loop";
    private static final String MUTED_ATTRIBUTE = "muted";

    private static final PropertyDescriptor<String, Optional<String>> preloadDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("preload", "");

    /**
     * How much of the media file the browser should fetch before playback is
     * asked for.
     * <p>
     * This is a hint and nothing more: a browser is free to ignore it, and what
     * it does when no preload is set differs between browsers. Use it to keep a
     * page with several players from downloading all of them at once, not to
     * guarantee that a file is or is not fetched.
     *
     * @see Media#setPreload(Preload)
     */
    public enum Preload {
        /**
         * The browser may download the whole file even if the visitor never
         * plays it.
         */
        AUTO("auto"),

        /**
         * Only the metadata — duration, dimensions, the first frame — is
         * fetched. Playback then starts with a short delay, but the player can
         * already show how long the recording is.
         */
        METADATA("metadata"),

        /**
         * Nothing is fetched until playback starts. The cheapest option for a
         * page that embeds a player the visitor is not expected to use.
         */
        NONE("none");

        private final String value;

        Preload(String value) {
            this.value = value;
        }

        /**
         * Gets the value used for the {@code preload} attribute.
         *
         * @return the attribute value
         */
        public String getValue() {
            return value;
        }

        /**
         * Resolves an attribute value to its constant. The comparison ignores
         * case, because {@code preload} is an HTML enumerated attribute and its
         * keywords are matched ASCII case-insensitively, so an element that
         * arrived with {@code preload="NONE"} has to read back as
         * {@link #NONE}.
         */
        private static Optional<Preload> fromAttributeValue(String value) {
            return Stream.of(values())
                    .filter(preload -> preload.value.equalsIgnoreCase(value))
                    .findFirst();
        }
    }

    /**
     * Creates a media component without any source.
     */
    protected Media() {
        super();
    }

    /**
     * Creates a media component that plays the first of the given sources the
     * browser can decode.
     *
     * @param sources
     *            the same recording in one or more file formats
     */
    protected Media(Source... sources) {
        super();
        add(sources);
    }

    /**
     * Adds a source for the media file at the given URL.
     *
     * @param src
     *            the URL of the media file
     * @param type
     *            the MIME type of the media file, such as
     *            <code>video/mp4</code>, or an empty string when it is not
     *            known
     * @return the source that was added, so that it can be configured further
     * @see #addSource(DownloadHandler, String)
     */
    public Source addSource(String src, String type) {
        Source source = new Source(src, type);
        add(source);
        return source;
    }

    /**
     * Adds a source for the media file that the given download handler serves.
     * This is the version to use for a file the application produces itself,
     * rather than one that is served from a URL of its own.
     *
     * @param downloadHandler
     *            the download handler that serves the media file, not
     *            <code>null</code>
     * @param type
     *            the MIME type of the media file, such as
     *            <code>video/mp4</code>, or an empty string when it is not
     *            known
     * @return the source that was added, so that it can be configured further
     * @see #addSource(String, String)
     */
    public Source addSource(DownloadHandler downloadHandler, String type) {
        Source source = new Source(downloadHandler, type);
        add(source);
        return source;
    }

    /**
     * Returns the sources of this media component, in the order the browser
     * considers them. This is the typed counterpart of {@link #getChildren()}.
     *
     * @return the sources of this media component
     */
    public List<Source> getSources() {
        return ComponentUtil.getChildrenOfType(this, Source.class).toList();
    }

    /**
     * Sets whether the browser shows its own playback controls — a play button,
     * a timeline, a volume slider. Without them, nothing on the page starts the
     * playback, so turn them on unless the application drives the player
     * itself.
     * <p>
     * The controls are the browser's, so they do not look the same everywhere
     * and cannot be styled through the application theme.
     *
     * @param controls
     *            <code>true</code> to show the playback controls,
     *            <code>false</code> to hide them
     */
    public void setControls(boolean controls) {
        getElement().setAttribute(CONTROLS_ATTRIBUTE, controls);
    }

    /**
     * Gets whether the browser shows its own playback controls.
     *
     * @return <code>true</code> if the playback controls are shown
     * @see #setControls(boolean)
     */
    public boolean isControls() {
        return getElement().hasAttribute(CONTROLS_ATTRIBUTE);
    }

    /**
     * Sets whether playback starts as soon as enough of the media file has
     * loaded, without waiting for the visitor.
     * <p>
     * Browsers block autoplay of a recording that has sound until the visitor
     * has interacted with the site, so a player that is expected to start on
     * its own also has to be {@link #setMuted(boolean) muted}.
     *
     * @param autoplay
     *            <code>true</code> to start playback automatically
     */
    public void setAutoplay(boolean autoplay) {
        getElement().setAttribute(AUTOPLAY_ATTRIBUTE, autoplay);
    }

    /**
     * Gets whether playback starts automatically.
     *
     * @return <code>true</code> if playback starts automatically
     * @see #setAutoplay(boolean)
     */
    public boolean isAutoplay() {
        return getElement().hasAttribute(AUTOPLAY_ATTRIBUTE);
    }

    /**
     * Sets whether playback starts over from the beginning when the end of the
     * recording is reached.
     *
     * @param loop
     *            <code>true</code> to play the recording in a loop
     */
    public void setLoop(boolean loop) {
        getElement().setAttribute(LOOP_ATTRIBUTE, loop);
    }

    /**
     * Gets whether the recording is played in a loop.
     *
     * @return <code>true</code> if the recording is played in a loop
     * @see #setLoop(boolean)
     */
    public boolean isLoop() {
        return getElement().hasAttribute(LOOP_ATTRIBUTE);
    }

    /**
     * Sets whether the recording is silent when the player is first shown.
     * <p>
     * This is the initial state only. Once the visitor has unmuted the player
     * through the browser's controls, the browser stops reading this attribute,
     * and setting it again does not mute the player a second time.
     *
     * @param muted
     *            <code>true</code> to start the player muted
     */
    public void setMuted(boolean muted) {
        getElement().setAttribute(MUTED_ATTRIBUTE, muted);
    }

    /**
     * Gets whether the player starts muted.
     *
     * @return <code>true</code> if the player starts muted
     * @see #setMuted(boolean)
     */
    public boolean isMuted() {
        return getElement().hasAttribute(MUTED_ATTRIBUTE);
    }

    /**
     * Sets how much of the media file the browser should fetch before playback
     * is asked for. Passing <code>null</code> removes the hint and leaves the
     * decision to the browser.
     *
     * @param preload
     *            how much to fetch in advance, or <code>null</code> to let the
     *            browser decide
     */
    public void setPreload(@Nullable Preload preload) {
        set(preloadDescriptor, preload == null ? "" : preload.getValue());
    }

    /**
     * Gets how much of the media file the browser is asked to fetch in advance.
     *
     * @return the preload hint, or an empty optional if none has been set
     * @see #setPreload(Preload)
     */
    public Optional<Preload> getPreload() {
        return get(preloadDescriptor).flatMap(Preload::fromAttributeValue);
    }
}
