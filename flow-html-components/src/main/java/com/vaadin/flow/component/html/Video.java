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

import java.util.Optional;

import org.jspecify.annotations.NullMarked;

import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Component representing a <code>&lt;video&gt;</code> element — the browser's
 * built-in video player.
 * <p>
 * List the recording in as many formats as you want to support, one
 * {@link Source} each, and the browser plays the first one it can decode:
 *
 * <pre>
 * Video video = new Video();
 * video.addSource("/intro.webm", "video/webm");
 * video.addSource("/intro.mp4", "video/mp4");
 * video.setControls(true);
 * video.setPoster("/intro-poster.jpg");
 * </pre>
 *
 * The element has no controls of its own until {@link #setControls(boolean)} is
 * called, so a player that the visitor is meant to start needs that call. Give
 * the player a size through {@link #setWidth(String)} and
 * {@link #setHeight(String)}; without one, the page reflows once the video
 * dimensions are known, unless a {@link #setPoster(String) poster} fixes the
 * size earlier.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/video">MDN:
 *      &lt;video&gt;</a>
 * @see Audio
 */
@NullMarked
@Tag(Tag.VIDEO)
public class Video extends Media {

    private static final String POSTER_ATTRIBUTE = "poster";

    private static final PropertyDescriptor<String, Optional<String>> posterDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault(POSTER_ATTRIBUTE, "");

    /**
     * Creates a video player without any source.
     */
    public Video() {
        super();
    }

    /**
     * Creates a video player that plays the first of the given sources the
     * browser can decode.
     *
     * @param sources
     *            the same recording in one or more file formats
     */
    public Video(Source... sources) {
        super(sources);
    }

    /**
     * Sets the URL of the image to show while the video is downloading, and
     * before it is played for the first time. Without a poster, the browser
     * shows the first frame of the video, which means it has to download part
     * of the file before there is anything to look at.
     *
     * @param poster
     *            the URL of the poster image, or an empty string to remove the
     *            poster
     */
    public void setPoster(String poster) {
        set(posterDescriptor, poster);
    }

    /**
     * Sets the poster image to the one that the given {@link DownloadHandler}
     * callback serves. This is the version to use for an image the application
     * produces itself, rather than one that is served from a URL of its own.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     * <p>
     * The handler is wrapped with {@link DownloadHandler#allowDisabled()} so
     * that the poster is still served when the video, or one of its ancestors,
     * is disabled. The browser fetches it as part of rendering rather than as a
     * user action, so blocking the request on the disabled state would leave
     * the player blank.
     *
     * @param downloadHandler
     *            the download handler that serves the poster image, not
     *            <code>null</code>
     * @see #setPoster(String)
     */
    public void setPoster(DownloadHandler downloadHandler) {
        if (downloadHandler instanceof AbstractDownloadHandler<?> handler) {
            // change disposition to inline in pre-defined handlers,
            // where it is 'attachment' by default
            handler.inline();
        }
        getElement().setAttribute(POSTER_ATTRIBUTE,
                downloadHandler.allowDisabled());
    }

    /**
     * Gets the URL of the poster image.
     *
     * @return the URL of the poster image, or an empty optional if none has
     *         been set
     * @see #setPoster(String)
     */
    public Optional<String> getPoster() {
        return get(posterDescriptor);
    }
}
