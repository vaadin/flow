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
package com.vaadin.flow.component.html;

import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.AbstractStreamResource;
import com.vaadin.flow.server.StreamResource;

/**
 * Component representing a <code>&lt;img&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Tag(Tag.IMG)
public class Image extends HtmlContainer implements ClickNotifier<Image> {

    private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors
            .attributeWithDefault("src", "");

    private static final PropertyDescriptor<String, Optional<String>> altDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("alt", "");

    /**
     * Creates a new empty image.
     */
    public Image() {
        super();
    }

    /**
     * Creates an image with the given URL and an alternative text.
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
     * @param src
     *            the resource value, not null
     * @param alt
     *            the alternate text
     *
     * @see #setSrc(AbstractStreamResource)
     * @see #setAlt(String)
     */
    public Image(AbstractStreamResource src, String alt) {
        setSrc(src);
        setAlt(alt);
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
     */
    public void setSrc(AbstractStreamResource src) {
        getElement().setAttribute("src", src);
    }

    /**
     * Sets the alternate text for the image.
     *
     * @param alt
     *            the alternate text
     */
    public void setAlt(String alt) {
        set(altDescriptor, alt);
    }

    /**
     * Gets the alternate text for the image.
     *
     * @return an optional alternate text, or an empty optional if no alternate
     *         text has been set
     */
    public Optional<String> getAlt() {
        return get(altDescriptor);
    }
}
