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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;

/**
 * Component for a <code>&lt;label&gt;</code> element, which represents a
 * caption for an item in a user interface.
 * <p>
 * Note that Label components are not meant for loose text in the page - they
 * should be coupled with another component by using the
 * {@link #setFor(Component)} or by adding them to it with the
 * {@link #add(Component...)} method.
 * <p>
 * Clicking on a label automatically transfers the focus to the associated
 * component. This is especially helpful when building forms with
 * {@link Input}s.
 * <p>
 * For adding texts to the page without linking them to other components,
 * consider using a {@link Span} or a {@link Div} instead. If the text should be
 * interpreted as HTML, use a {@link Html} (but remember to guard against
 * cross-site scripting attacks).
 *
 * @author Vaadin Ltd
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/HTML/Element/label">https://developer.mozilla.org/en-US/docs/Web/HTML/Element/label</a>
 * @since 1.0
 */
@Tag(Tag.LABEL)
public class Label extends HtmlContainer {
    private static final PropertyDescriptor<String, Optional<String>> forDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("for", "");

    /**
     * Creates a new empty label.
     */
    public Label() {
        super();
    }

    /**
     * Creates a new label with the given text content.
     *
     * @param text
     *            the text content
     */
    public Label(String text) {
        this();
        setText(text);
    }

    /**
     * Sets the component that this label describes. The component (or its id)
     * should be defined in case the described component is not an ancestor of
     * the label.
     * <p>
     * The provided component must have an id set. This component will still use
     * the old id if the id of the provided component is changed after this
     * method has been called.
     *
     * @param forComponent
     *            the component that this label describes, not <code>null</code>
     *            , must have an id
     * @throws IllegalArgumentException
     *             if the provided component has no id
     */
    public void setFor(Component forComponent) {
        if (forComponent == null) {
            throw new IllegalArgumentException(
                    "The provided component cannot be null");
        }
        setFor(forComponent.getId()
                .orElseThrow(() -> new IllegalArgumentException(
                        "The provided component must have an id")));
    }

    /**
     * Sets the id of the component that this label describes. The id should be
     * defined in case the described component is not an ancestor of the label.
     *
     * @param forId
     *            the id of the described component, or <code>null</code> if
     *            there is no value
     */
    public void setFor(String forId) {
        set(forDescriptor, forId);
    }

    /**
     * Gets the id of the component that this label describes.
     *
     * @see #setFor(String)
     *
     * @return an optional id of the described component, or an empty optional
     *         if the attribute has not been set
     */
    public Optional<String> getFor() {
        return get(forDescriptor);
    }
}
