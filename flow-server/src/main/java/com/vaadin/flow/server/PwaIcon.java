/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import java.io.Serializable;
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

    private final int width;
    private final int height;
    private String baseName;
    private Domain domain;

    private final Map<String, String> attributes = new HashMap<>();
    private String tag = "link";

    public PwaIcon(int width, int height, String baseName) {
        this(width, height, baseName, Domain.HEADER);
    }

    public PwaIcon(int width, int height, String baseName, Domain domain) {
        this(width, height, baseName, domain, "icon", "");
    }

    public PwaIcon(int width, int height, String baseName, Domain domain,
            String rel, String media) {
        this.width = width;
        this.height = height;
        this.baseName = baseName;
        this.domain = domain;

        attributes.put("type", "image/png");
        attributes.put("rel", rel);
        attributes.put("sizes", width + "x" + height);
        if (media != null && !media.isEmpty()) {
            attributes.put("media", media);
        }
        attributes.put("href", getPath());
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

    public String getPath() {
        int split = baseName.lastIndexOf('.');
        return baseName.substring(0, split) + "-" + getSizes()
                + baseName.substring(split);
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
     * Gets the value of the {@literal type} attribute.
     *
     * @return value of the {@literal type} attribute
     */
    public String getType() {
        return attributes.get("type");
    }

    /**
     * Gets the icon {@link Domain}.
     *
     * @return the domain of the icon
     */
    public Domain getDomain() {
        return domain;
    }

}
