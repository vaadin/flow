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
package com.vaadin.flow.uitest.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.AudioElement;
import com.vaadin.flow.component.html.testbench.VideoElement;

import static com.vaadin.flow.uitest.ui.MediaComponentsView.POSTER_PAYLOAD;
import static com.vaadin.flow.uitest.ui.MediaComponentsView.SAMPLE_COUNT;
import static com.vaadin.flow.uitest.ui.MediaComponentsView.SAMPLE_RATE;

/**
 * Verifies that the media components produce a player the browser can really
 * use: an audio file served by a download handler is decoded, and a video whose
 * formats are all unavailable ends up reporting that it found nothing to play.
 */
public class MediaComponentsIT extends AbstractStreamResourceIT {

    @Test
    public void audioFromDownloadHandler_isDecodedByTheBrowser() {
        open();

        AudioElement audio = $(AudioElement.class).id("audio-player");
        Assert.assertTrue("The audio player should show the browser controls",
                audio.hasAttribute("controls"));

        // HAVE_METADATA or more means the browser fetched the file and read it
        waitUntil(driver -> ((Number) executeScript(
                "return arguments[0].readyState", audio)).intValue() >= 1);

        double duration = ((Number) executeScript(
                "return arguments[0].duration", audio)).doubleValue();
        Assert.assertEquals(
                "The browser should have decoded the audio the download handler serves",
                SAMPLE_COUNT / (double) SAMPLE_RATE, duration, 0.05);
    }

    @Test
    public void videoWithoutAPlayableSource_reportsThatItFoundNothingToPlay() {
        open();

        VideoElement video = $(VideoElement.class).id("video-player");
        List<WebElement> sources = video.findElements(By.tagName("source"));
        Assert.assertEquals("Both sources should be rendered", 2,
                sources.size());
        Assert.assertEquals("video/webm", sources.get(0).getAttribute("type"));
        Assert.assertEquals("video/mp4", sources.get(1).getAttribute("type"));

        // NETWORK_NO_SOURCE, the state a media element ends in when none of its
        // sources can be played
        waitUntil(driver -> ((Number) executeScript(
                "return arguments[0].networkState", video)).intValue() == 3);
    }

    @Test
    public void videoPoster_isServedByTheDownloadHandler() throws IOException {
        open();

        VideoElement video = $(VideoElement.class).id("video-player");
        String poster = video.getAttribute("poster");
        Assert.assertNotNull("The video should have a poster attribute",
                poster);

        try (InputStream stream = download(poster)) {
            Assert.assertEquals(POSTER_PAYLOAD,
                    IOUtils.toString(stream, StandardCharsets.UTF_8));
        }
    }
}
