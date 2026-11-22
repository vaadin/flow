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
package com.vaadin.flow.component.html.testbench;

import com.vaadin.testbench.TestBenchElement;

/**
 * A TestBench element representing a media player, the common base of
 * {@link VideoElement} and {@link AudioElement}. It drives the player the way a
 * script on the page would, so it also works for a player whose controls are
 * hidden.
 */
public abstract class MediaElement extends TestBenchElement {

    /**
     * Starts the playback and waits until the browser has started it.
     * <p>
     * The browser's autoplay policy applies as to any script: a player with
     * sound can only be started this way after the test has interacted with the
     * page, such as by clicking an element, so mute the player when the test
     * has not.
     *
     * @throws org.openqa.selenium.JavascriptException
     *             if the browser refuses to start the playback
     */
    public void play() {
        // play() returns a promise, which the driver waits for
        callFunction("play");
    }

    /**
     * Pauses the playback.
     */
    public void pause() {
        callFunction("pause");
    }

    /**
     * Checks whether the player is playing: the playback has been started and
     * has neither been paused nor reached the end of the recording.
     *
     * @return <code>true</code> if the player is playing
     */
    public boolean isPlaying() {
        return !getPropertyBoolean("paused") && !getPropertyBoolean("ended");
    }
}
