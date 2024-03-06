/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html;

/**
 * Enum representing <code>target</code> attribute values for an
 * <code>&lt;a&gt;</code> element.
 *
 * @author Vaadin Ltd
 * @since
 */
public enum AnchorTarget implements AnchorTargetValue {
    /**
     * Remove the target value. This has the same effect as <code>SELF</code>.
     */
    DEFAULT(""),
    /**
     * Open a link in the current context.
     */
    SELF("_self"),
    /**
     * Open a link in a new unnamed context.
     */
    BLANK("_blank"),
    /**
     * Open a link in the parent context, or the current context if there is no
     * parent context.
     */
    PARENT("_parent"),
    /**
     * Open a link in the top most grandparent context, or the current context
     * if there is no parent context.
     */
    TOP("_top");

    private final String value;

    /**
     * @param value
     *            the text value to use by an {@code <a>} (anchor) tag.
     */
    AnchorTarget(String value) {
        this.value = value;
    }

    /**
     * @return value the text value to use by an {@code <a>} (anchor) tag.
     */
    @Override
    public String getValue() {
        return value;
    }

}
