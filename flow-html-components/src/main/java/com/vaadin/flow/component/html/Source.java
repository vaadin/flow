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

import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Component representing a <code>&lt;source&gt;</code> element — one of the
 * alternative files a {@link Video} or an {@link Audio} can play.
 * <p>
 * A media component lists the same recording in several formats, each as a
 * source of its own, and the browser plays the first one it can decode. The
 * {@link #setType(String) type} tells it which that is without downloading
 * anything, so give it whenever you know the format.
 * <p>
 * Sources are usually created through {@link Media#addSource(String, String)}
 * rather than directly.
 *
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/source">MDN:
 *      &lt;source&gt;</a>
 */
@NullMarked
@Tag(Tag.SOURCE)
public class Source extends HtmlComponent {

    private static final String SRC_ATTRIBUTE = "src";

    private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors
            .attributeWithDefault(SRC_ATTRIBUTE, "");

    private static final PropertyDescriptor<String, Optional<String>> typeDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("type", "");

    /**
     * Creates a new source without a URL.
     */
    public Source() {
        super();
    }

    /**
     * Creates a new source for the media file at the given URL.
     *
     * @param src
     *            the URL of the media file
     * @param type
     *            the MIME type of the media file, such as
     *            <code>video/mp4</code>, or an empty string when it is not
     *            known
     * @see #setSrc(String)
     * @see #setType(String)
     */
    public Source(String src, String type) {
        setSrc(src);
        setType(type);
    }

    /**
     * Creates a new source for the media file that the given download handler
     * serves.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @param downloadHandler
     *            the download handler that serves the media file, not
     *            <code>null</code>
     * @param type
     *            the MIME type of the media file, such as
     *            <code>video/mp4</code>, or an empty string when it is not
     *            known
     * @see #setSrc(DownloadHandler)
     * @see #setType(String)
     */
    public Source(DownloadHandler downloadHandler, String type) {
        setSrc(downloadHandler);
        setType(type);
    }

    /**
     * Sets the URL of the media file.
     *
     * @param src
     *            the URL of the media file
     */
    public void setSrc(String src) {
        set(srcDescriptor, src);
    }

    /**
     * Sets the URL of the media file to the URL of the given
     * {@link DownloadHandler} callback.
     * <p>
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     * <p>
     * The handler is wrapped with {@link DownloadHandler#allowDisabled()} so
     * that the media file is still served when the media component, or one of
     * its ancestors, is disabled. The browser fetches it as part of rendering
     * rather than as a user action, so blocking the request on the disabled
     * state would leave the player empty.
     *
     * @param downloadHandler
     *            the download handler that serves the media file, not
     *            <code>null</code>
     * @see #setSrc(String)
     */
    public void setSrc(DownloadHandler downloadHandler) {
        if (downloadHandler instanceof AbstractDownloadHandler<?> handler) {
            // change disposition to inline in pre-defined handlers,
            // where it is 'attachment' by default
            handler.inline();
        }
        getElement().setAttribute(SRC_ATTRIBUTE,
                downloadHandler.allowDisabled());
    }

    /**
     * Gets the URL of the media file.
     *
     * @return the URL of the media file, or an empty string if none has been
     *         set
     * @see #setSrc(String)
     */
    public String getSrc() {
        return get(srcDescriptor);
    }

    /**
     * Sets the MIME type of the media file, such as <code>video/mp4</code> or
     * <code>audio/mpeg</code>. A codec parameter may be included, as in
     * <code>video/webm; codecs="vp9, opus"</code>.
     * <p>
     * The browser skips a source whose type it cannot play without requesting
     * the file, so a type that is set saves a download on every browser that
     * plays one of the other sources.
     *
     * @param type
     *            the MIME type of the media file, or an empty string to remove
     *            the type
     */
    public void setType(String type) {
        set(typeDescriptor, type);
    }

    /**
     * Gets the MIME type of the media file.
     *
     * @return the MIME type, or an empty optional if none has been set
     * @see #setType(String)
     */
    public Optional<String> getType() {
        return get(typeDescriptor);
    }
}
