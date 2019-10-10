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

import com.vaadin.flow.component.Component;
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
 * consider using a {@link Span} or a {@link Div} instead.
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
