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
package com.vaadin.flow.server;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Element;

/**
 * Implementation of icons used in PWA resources.
 *
 * Creates the href automatically based on - baseName (the file name with path,
 * as {@literal icons/icon.png"}) - width (width of icon) - height (height of
 * icon) - (possibly) fileHash (the hashcode of image file)
 *
 * The href will be set as: {@code [basename]-[width]x[height].png{?[filehash]}}
 *
 * The trailing {@literal ?[filehash]} will be added if icon cache is not
 * controlled by service worker: cached = false
 *
 * Icon caching is left to the browser if it's not cached with service worker.
 *
 * @since 1.2
 */
public class PwaIcon implements Serializable {
    /**
     * Place where icon belongs to (header or manifest.webmanifest).
     */
    public enum Domain {
        HEADER, MANIFEST
    }

    private final boolean shouldBeCached;
    private final int width;
    private final int height;
    private long fileHash;
    private String baseName;
    private Domain domain;
    private byte[] data;

    private final Map<String, String> attributes = new HashMap<>();
    private String tag = "link";

    private PwaRegistry registry;

    PwaIcon(int width, int height, String baseName) {
        this(width, height, baseName, Domain.HEADER);
    }

    PwaIcon(int width, int height, String baseName, Domain domain) {
        this(width, height, baseName, domain, false);
    }

    PwaIcon(int width, int height, String baseName, Domain domain,
            boolean shouldBeCached) {
        this(width, height, baseName, domain, shouldBeCached, "icon", "");
    }

    PwaIcon(int width, int height, String baseName, Domain domain,
            boolean shouldBeCached, String rel, String media) {
        this.width = width;
        this.height = height;
        this.baseName = baseName;
        this.domain = domain;
        this.shouldBeCached = shouldBeCached;

        attributes.put("type", "image/png");
        attributes.put("rel", rel);
        attributes.put("sizes", width + "x" + height);
        if (media != null && !media.isEmpty()) {
            attributes.put("media", media);
        }

        setRelativeName();
    }

    protected PwaIcon(PwaIcon icon) {
        this.width = icon.width;
        this.height = icon.height;
        this.baseName = icon.baseName;
        this.domain = icon.domain;
        this.shouldBeCached = icon.shouldBeCached;
        this.attributes.putAll(icon.attributes);
    }

    /**
     * Gets an {@link Element} presentation of the icon.
     *
     * @return an {@link Element} presentation of the icon
     */
    public Element asElement() {
        Element element = new Element(tag);
        attributes.forEach(element::attr);
        return element;
    }

    /**
     * Gets width of an icon.
     *
     * @return width of an icon
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets height of an icon.
     *
     * @return height of an icon
     */
    public int getHeight() {
        return height;
    }

    /**
     * Should the icon be cached by the Service Worker.
     *
     * @return should the icon be cached by the Service Worker.
     */
    public boolean shouldBeCached() {
        return shouldBeCached;
    }

    private void setRelativeName() {
        int split = baseName.lastIndexOf('.');
        String link = baseName.substring(0, split) + "-" + getSizes()
                + baseName.substring(split);
        if (!shouldBeCached) {
            link = link + "?" + fileHash;
        }
        attributes.put("href", link);
    }

    /**
     * Gets the value of the {@literal sizes} attribute.
     *
     * @return value of the {@literal sizes} attribute
     */
    public String getSizes() {
        return attributes.get("sizes");
    }

    /**
     * Gets the value of the {@literal href} attribute.
     *
     * @return value of the {@literal href} attribute
     */
    public String getHref() {
        return attributes.get("href");
    }

    /**
     * Return href with '/' -prefix and removed possible ?[fileHash].
     *
     * Used in matching, when serving images.
     *
     * @return href with '/' -prefix and removed possible ?[fileHash]
     */
    public String getRelHref() {
        String[] split = getHref().split("\\?");
        return "/" + split[0];
    }

    /**
     * Gets the cache-string used in Google Workbox caching.
     *
     * @return "{ url: '[href]', revision: '[fileHash' }"
     */
    public String getCacheFormat() {
        return String.format("{ url: '%s', revision: '%s' }", getHref(),
                fileHash);
    }

    /**
     * Gets the value of the {@literal type} attribute.
     *
     * @return value of the {@literal type} attribute
     */
    public String getType() {
        return attributes.get("type");
    }

    /**
     * Gets the value of the {@literal rel} attribute.
     *
     * @return value of the {@literal rel} attribute
     */
    String getRel() {
        return attributes.get("rel");
    }

    /**
     * Gets the icon {@link Domain}.
     *
     * @return the domain of the icon
     */
    public Domain getDomain() {
        return domain;
    }

    public void setRegistry(PwaRegistry registry) {
        this.registry = registry;
    }

    /**
     * Sets the image presenting the icon.
     *
     * @param image
     *            the image in png format
     */
    public void setImage(BufferedImage image) {
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", stream);
            stream.flush();
            data = stream.toByteArray();
            fileHash = Arrays.hashCode(data);
            setRelativeName();
        } catch (IOException ioe) {
            throw new UncheckedIOException("Failed to write an image ", ioe);
        }
    }

    void setImage(InputStream image) throws IOException {
        if (image != null) {
            data = image.readAllBytes();
            fileHash = Arrays.hashCode(data);
            setRelativeName();
        }
    }

    /**
     * Gets if the icon can be written on a stream or not.
     *
     * @return {@literal true} if the icon can be written, otherwise
     *         {@literal false}.
     * @see #write(OutputStream)
     */
    boolean isAvailable() {
        return data != null || registry.getBaseImage() != null;
    }

    /**
     * Writes the icon image to output stream.
     *
     * @param outputStream
     *            output stream to write the icon image to
     */
    public void write(OutputStream outputStream) {
        if (data == null) {
            // New image with wanted size
            // Store byte array and hashcode of image (GeneratedImage)
            setImage(drawIconImage(getBaseImage()));
        }
        try {
            outputStream.write(data);
        } catch (IOException ioe) {
            throw new UncheckedIOException(
                    "Failed to store the icon image into the stream provided",
                    ioe);
        }
    }

    // visible for test
    protected BufferedImage getBaseImage() {
        return registry.getBaseImage();
    }

    private BufferedImage drawIconImage(BufferedImage baseImage) {
        // Pick top-left pixel as fill color if needed for image
        // resizing
        int bgColor = baseImage.getRGB(0, 0);

        BufferedImage bimage = new BufferedImage(this.getWidth(),
                this.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // Draw the image on to the buffered image
        Graphics2D graphics = bimage.createGraphics();

        // fill bg with fill-color
        graphics.setBackground(new Color(bgColor, true));
        graphics.clearRect(0, 0, this.getWidth(), this.getHeight());

        // calculate ratio (bigger ratio) for resize
        float ratio = (float) baseImage.getWidth()
                / (float) this.getWidth() > (float) baseImage.getHeight()
                        / (float) this.getHeight()
                                ? (float) baseImage.getWidth()
                                        / (float) this.getWidth()
                                : (float) baseImage.getHeight()
                                        / (float) this.getHeight();

        // Forbid upscaling of image
        ratio = ratio > 1.0f ? ratio : 1.0f;

        // calculate sizes with ratio
        int newWidth = Math.round(baseImage.getHeight() / ratio);
        int newHeight = Math.round(baseImage.getWidth() / ratio);

        // draw rescaled img in the center of created image
        graphics.drawImage(
                baseImage.getScaledInstance(newWidth, newHeight,
                        Image.SCALE_SMOOTH),
                (this.getWidth() - newWidth) / 2,
                (this.getHeight() - newHeight) / 2, null);
        graphics.dispose();
        return bimage;
    }

}
