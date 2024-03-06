/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

import java.util.Optional;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HasAriaLabel;
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
     * <p>
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
     * <p>
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
