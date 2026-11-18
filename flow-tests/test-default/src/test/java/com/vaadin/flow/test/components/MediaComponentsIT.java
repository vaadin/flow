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
package com.vaadin.flow.test.components;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.AudioElement;
import com.vaadin.flow.component.html.testbench.VideoElement;
import com.vaadin.flow.test.TestFor;
import com.vaadin.testbench.BrowserTest;

import static com.vaadin.flow.test.components.MediaComponentsView.POSTER_PAYLOAD;
import static com.vaadin.flow.test.components.MediaComponentsView.SAMPLE_COUNT;
import static com.vaadin.flow.test.components.MediaComponentsView.SAMPLE_RATE;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the media components produce a player the browser can really
 * use: an audio file served by a download handler is decoded, and a video whose
 * formats are all unavailable ends up reporting that it found nothing to play.
 */
@TestFor(MediaComponentsView.class)
public class MediaComponentsIT extends AbstractStreamResourceIT {

    @BrowserTest
    public void audioFromDownloadHandler_isDecodedByTheBrowser() {
        open();

        AudioElement audio = $(AudioElement.class).id("audio-player");
        assertTrue(audio.hasAttribute("controls"),
                "The audio player should show the browser controls");

        // HAVE_METADATA or more means the browser fetched the file and read it
        waitUntil(driver -> ((Number) executeScript(
                "return arguments[0].readyState", audio)).intValue() >= 1);

        double duration = ((Number) executeScript(
                "return arguments[0].duration", audio)).doubleValue();
        Assertions.assertEquals(SAMPLE_COUNT / (double) SAMPLE_RATE, duration,
                0.05,
                "The browser should have decoded the audio the download handler serves");
    }

    @BrowserTest
    public void videoWithoutAPlayableSource_reportsThatItFoundNothingToPlay() {
        open();

        VideoElement video = $(VideoElement.class).id("video-player");
        List<WebElement> sources = video.findElements(By.tagName("source"));
        Assertions.assertEquals(2, sources.size(),
                "Both sources should be rendered");
        Assertions.assertEquals("video/webm",
                sources.get(0).getAttribute("type"));
        Assertions.assertEquals("video/mp4",
                sources.get(1).getAttribute("type"));

        // NETWORK_NO_SOURCE, the state a media element ends in when none of its
        // sources can be played
        waitUntil(driver -> ((Number) executeScript(
                "return arguments[0].networkState", video)).intValue() == 3);
    }

    @BrowserTest
    public void videoPoster_isServedByTheDownloadHandler() throws IOException {
        open();

        VideoElement video = $(VideoElement.class).id("video-player");
        String poster = video.getAttribute("poster");
        assertNotNull(poster, "The video should have a poster attribute");

        try (InputStream stream = download(poster)) {
            Assertions.assertEquals(POSTER_PAYLOAD,
                    IOUtils.toString(stream, StandardCharsets.UTF_8));
        }
    }
}
