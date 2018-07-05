/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Element;

/**
 * Implementation of icons used in PWA resources.
 *
 * Creates the href automatically based on
 * - baseName (the file name with path, as "icons/icon.png"
 * - width (width of icon)
 * - height (height of icon)
 * - (possibly) fileHash (the hashcode of image file)
 *
 * The href will be set as:
 * [basename]-[width]x[height].png{?[filehash]}
 *
 * The trailing ?[filehash] will be added if icon cache is not controlled
 * by service worker: cached = false
 *
 * So caching of a icon is left left for browser if it's not cached with
 * service worker.
 *
 */
public class PWAIcon implements Serializable {
    /**
     * Where icon belongs to.
     *
     * In header or manifest.json.
     *
     */
    public enum Domain {
        HEADER,
        MANIFEST;
    }

    private boolean cached = false;
    private int width = 0;
    private int height = 0;
    private long fileHash = 0;
    private String baseName = "icons/icon.png";
    private Domain domain = Domain.HEADER;
    private boolean hrefOverride = false;
    private byte[] data;

    private Map<String, String> attributes = new HashMap<>();
    private String tag = "link";

    protected PWAIcon(int width, int height, String baseName) {
        this(width, height, baseName, Domain.HEADER);
    }

    protected PWAIcon(int width, int height, String baseName, Domain domain) {
        this(width, height, baseName, domain, false);
    }

    protected PWAIcon(int width, int height, String baseName, Domain domain,
            boolean cached) {
        this(width, height, baseName, domain, cached, "icon", "");
    }

    protected PWAIcon(int width, int height, String baseName, Domain domain,
            boolean cached, String rel, String media) {
        attributes.put("type", "image/png");
        attributes.put("rel", "icon");
        this.width = width;
        this.height = height;
        this.baseName = baseName;
        this.domain = domain;
        this.cached = cached;
        attributes.put("rel", rel);
        if (media != null && !media.isEmpty()) {
            attributes.put("media", media);
        }
        attributes.put("sizes", width + "x" + height);
        setRelativeName();
    }


    /**
     * Gets an {@link Element} presentation of the icon.
     *
     * @return an {@link Element} presentation of the icon
     */
    public Element asElement() {
        Element element = new Element(tag);
        attributes.entrySet().forEach(entry ->
            element.attr(entry.getKey(), entry.getValue()));
        return element;
    }

    /**
     * Width of icon.
     *
     * @return Width of icon
     */
    public int getWidth() {
        return width;
    }

    /**
     * Height of icon.
     *
     * @return Height of icon
     */
    public int getHeight() {
        return height;
    }

    /**
     * Should the icon be cached viá Service Worker.
     *
     * @return Should the icon be cached viá Service Worker.
     */
    public boolean cached() {
        return cached;
    }

    /**
     * Sets the href based on icon values.
     *
     */
    private void setRelativeName() {
        if (!hrefOverride) {
            int split = baseName.lastIndexOf('.');
            String link = baseName.substring(0,split) + "-" + getSizes() +
                    baseName.substring(split);
            if (!cached) {
                link = link + "?" + fileHash;
            }
            attributes.put("href", link);
        }
    }

    /**
     * Gets string for sizes -attribute.
     *
     *
     * @return a String as [size]x[size]
     */
    public String getSizes() {
        return attributes.get("sizes");
    }

    /**
     * href attribute.
     *
     * @return href attribute
     */
    public String getHref() {
        return attributes.get("href");
    }

    /**
     * Return href with '/' -prefix and removed possible ?[fileHash].
     *
     * Used in matching, when serving images.
     *
     * @return Return href with '/' -prefix and removed possible ?[fileHash]
     */
    public String getRelHref() {
        String[] splitted = getHref().split("\\?");
        return "/" + splitted[0];
    }

    /**'
     * Gets the cache-string used in Google Workbox caching.
     *
     * @return "{ url: '[href]', revision: '[fileHash' }"
     */
    public String getCacheFormat() {
        return String.format("{ url: '%s', revision: '%s' }", getHref(),
                fileHash);
    }

    /**
     * Type attribute.
     *
     * @return type -attribute
     */
    public String getType() {
        return attributes.get("type");
    }

    /**
     * Domain of icon.
     *
     * @return Domain of icon
     */
    public Domain getDomain() {
        return this.domain;
    }

    /**
     * Sets the image presenting the icon.
     *
     * @param image image in png -format
     * @throws IOException
     */
    public void setImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write( image, "png", baos );
        baos.flush();
        data = baos.toByteArray();
        fileHash = Arrays.hashCode(data);
        setRelativeName();
        baos.close();
    }

    /**
     * Writes the image to output stream.
     *
     * @param outputStream output stream
     * @throws IOException possible exception in stream writing
     */
    public synchronized void write(OutputStream outputStream)
            throws IOException {
        outputStream.write(data);
    }

}
