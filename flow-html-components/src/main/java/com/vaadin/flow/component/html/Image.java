/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasAriaLabel;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * Component representing a <code>&lt;img&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.IMG)
public class Image extends HtmlContainer
        implements ClickNotifier<Image>, HasAriaLabel {

    private static final String ALT_ATTRIBUTE = "alt";
    private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors
            .attributeWithDefault("src", "");

    /**
     * Creates a new empty image.
     */
    public Image() {
        super();
    }

    /**
     * Creates an image with the given URL and an alternative text.
     *
     * The alternative text given to constructor is always set even if it is the
     * default empty string which is not retained with {@link #setAlt(String)}.
     *
     * @param src
     *            the image URL
     * @param alt
     *            the alternate text
     *
     * @see #setSrc(String)
     * @see #setAlt(String)
     */
    public Image(String src, String alt) {
        setSrc(src);
        setAlt(alt);
    }

    /**
     * Creates an image with the given stream resource and an alternative text.
     *
     * The alternative text given to constructor is always set even if it is the
     * default empty string which is not retained with {@link #setAlt(String)}.
     *
     * @param src
     *            the resource value, not null
     * @param alt
     *            the alternate text
     *
     * @see #setSrc(AbstractStreamResource)
     * @see #setAlt(String)
     * @deprecated use {@link #Image(DownloadHandler, String)} instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public Image(AbstractStreamResource src, String alt) {
        setSrc(src);
        setAlt(alt);
    }

    /**
     * Creates an image with the given download handler callback for providing
     * an image data and an alternative text.
     *
     * The alternative text given to constructor is always set even if it is the
     * default empty string which is not retained with {@link #setAlt(String)}.
     *
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @param downloadHandler
     *            the download handler callback that provides an image data, not
     *            null
     * @param alt
     *            the alternate text
     *
     * @see #setSrc(DownloadHandler)
     * @see #setAlt(String)
     */
    public Image(DownloadHandler downloadHandler, String alt) {
        setSrc(downloadHandler);
        setAlt(alt);
    }

    /**
     * Creates an image from byte array content with the given image name.
     *
     * This convenience constructor simplifies the creation of images from
     * in-memory byte data by automatically handling the creation of a
     * {@link DownloadHandler} with a {@link DownloadResponse}.
     *
     * The MIME type is automatically determined from the file extension in the
     * image name using {@link URLConnection#guessContentTypeFromName(String)}.
     * If the image name does not have a recognizable extension, the content
     * type will be null and the browser will attempt to determine it.
     *
     * The alternative text is set to the provided image name.
     *
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * to ensure the image is displayed in the browser rather than downloaded.
     *
     * @param imageContent
     *            the image data as a byte array, not null
     * @param imageName
     *            the image name (including file extension for MIME type
     *            detection), not null
     *
     * @see #setSrc(DownloadHandler)
     * @see #setAlt(String)
     */
    public Image(byte[] imageContent, String imageName) {
        this(DownloadHandler.fromInputStream(event -> {
            return new DownloadResponse(new ByteArrayInputStream(imageContent),
                    imageName,
                    URLConnection.guessContentTypeFromName(imageName),
                    imageContent.length);
        }).inline(), imageName);
    }

    /**
     * Gets the image URL.
     *
     * @return the image URL
     */
    public String getSrc() {
        return get(srcDescriptor);
    }

    /**
     * Sets the image URL.
     *
     * @param src
     *            the image URL
     */
    public void setSrc(String src) {
        set(srcDescriptor, src);
    }

    /**
     * Sets the image URL with the URL of the given {@link StreamResource}.
     *
     * @param src
     *            the resource value, not null
     * @deprecated use {@link #setSrc(DownloadHandler)} instead
     */
    @Deprecated(since = "24.8", forRemoval = true)
    public void setSrc(AbstractStreamResource src) {
        getElement().setAttribute("src", src);
    }

    /**
     * Sets the image URL with the URL of the given {@link DownloadHandler}
     * callback.
     *
     * Sets the <code>Content-Disposition</code> header to <code>inline</code>
     * for pre-defined download handlers, created by factory methods in
     * {@link DownloadHandler}, as well as for other
     * {@link AbstractDownloadHandler} implementations.
     *
     * @param downloadHandler
     *            the download handler resource, not null
     */
    public void setSrc(DownloadHandler downloadHandler) {
        if (downloadHandler instanceof AbstractDownloadHandler<?> handler) {
            // change disposition to inline in pre-defined handlers,
            // where it is 'attachment' by default
            handler.inline();
        }
        getElement().setAttribute("src", downloadHandler);
    }

    /**
     * Sets the alternate text for the image.
     *
     * @param alt
     *            the alternate text
     */
    public void setAlt(String alt) {
        if (alt == null) {
            getElement().removeAttribute(ALT_ATTRIBUTE);
        } else {
            // Also an empty string should be set as alt
            getElement().setAttribute(ALT_ATTRIBUTE, alt);
        }
    }

    /**
     * Gets the alternate text for the image.
     *
     * @return an optional alternate text, or an empty optional if no alternate
     *         text has been set
     */
    public Optional<String> getAlt() {
        return Optional.ofNullable(getElement().getAttribute(ALT_ATTRIBUTE));
    }
}
