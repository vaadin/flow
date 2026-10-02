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

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import com.vaadin.flow.component.html.Audio;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Media.Preload;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.MediaComponentsView", layout = ViewTestLayout.class)
public class MediaComponentsView extends Div {

    static final int SAMPLE_RATE = 8000;
    static final int SAMPLE_COUNT = SAMPLE_RATE / 2;

    static final String POSTER_PAYLOAD = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 16 16">\
            <rect width="16" height="16" fill="#1676f3"/></svg>""";

    public MediaComponentsView() {
        Audio audio = new Audio();
        audio.setId("audio-player");
        audio.setControls(true);
        audio.setPreload(Preload.AUTO);
        audio.addSource(DownloadHandler.fromInputStream(event -> {
            byte[] wav = createSilentWav();
            return new DownloadResponse(new ByteArrayInputStream(wav),
                    "silence.wav", "audio/wav", wav.length);
        }), "audio/wav");

        // None of the sources can be played, so the browser has to end up
        // reporting that it found nothing to play
        Video video = new Video();
        video.setId("video-player");
        video.setControls(true);
        video.setMuted(true);
        video.setPreload(Preload.METADATA);
        video.setPoster(DownloadHandler.fromInputStream(event -> {
            byte[] bytes = POSTER_PAYLOAD.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    "poster.svg", "image/svg+xml", bytes.length);
        }));
        video.addSource("there-is-no-such-file.webm", "video/webm");
        video.addSource("there-is-no-such-file.mp4", "video/mp4");

        add(audio, video);
    }

    /**
     * Builds half a second of silence as an 8-bit mono PCM WAVE file, so that
     * the view has a media file the browser really can decode without one being
     * committed to the repository.
     */
    private static byte[] createSilentWav() {
        int dataSize = SAMPLE_COUNT;
        ByteBuffer wav = ByteBuffer.allocate(44 + dataSize)
                .order(ByteOrder.LITTLE_ENDIAN);
        wav.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(36 + dataSize);
        wav.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        wav.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(16); // size of the format chunk
        wav.putShort((short) 1); // uncompressed PCM
        wav.putShort((short) 1); // one channel
        wav.putInt(SAMPLE_RATE);
        wav.putInt(SAMPLE_RATE); // bytes per second
        wav.putShort((short) 1); // bytes per sample frame
        wav.putShort((short) 8); // bits per sample
        wav.put("data".getBytes(StandardCharsets.US_ASCII));
        wav.putInt(dataSize);
        for (int i = 0; i < dataSize; i++) {
            // the midpoint of the unsigned 8-bit range is silence
            wav.put((byte) 128);
        }
        return wav.array();
    }
}
