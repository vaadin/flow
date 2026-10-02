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

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.Tag;

/**
 * Component representing an <code>&lt;audio&gt;</code> element — the browser's
 * built-in audio player.
 * <p>
 * List the recording in as many formats as you want to support, one
 * {@link Source} each, and the browser plays the first one it can decode:
 *
 * <pre>
 * Audio audio = new Audio();
 * audio.addSource("/chime.ogg", "audio/ogg");
 * audio.addSource("/chime.mp3", "audio/mpeg");
 * audio.setControls(true);
 * </pre>
 *
 * Nothing of the element is rendered until {@link #setControls(boolean)} is
 * called, so a player that the visitor is meant to start needs that call. A
 * sound that is only played by the application, and that the visitor should not
 * see a player for, is the one case where leaving the controls off is right.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/audio">MDN:
 *      &lt;audio&gt;</a>
 * @see Video
 */
@NullMarked
@Tag(Tag.AUDIO)
public class Audio extends Media {

    /**
     * Creates an audio player without any source.
     */
    public Audio() {
        super();
    }

    /**
     * Creates an audio player that plays the first of the given sources the
     * browser can decode.
     *
     * @param sources
     *            the same recording in one or more file formats
     */
    public Audio(Source... sources) {
        super(sources);
    }
}
