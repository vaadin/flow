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

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.shared.Registration;
import org.slf4j.LoggerFactory;

/**
 * Component for a <code>&lt;label&gt;</code> element, which represents a
 * caption for an item in a user interface.
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
 * @deprecated Use {@link NativeLabel} instead, if you need the HTML
 *             <code>&lt;label&gt;</code> element, which is normally not needed
 *             within a Vaadin Flow application's high-level components. To use
 *             a Label that works in the older style of Vaadin 8 or Java Swing,
 *             and can be used both to label a component and to display loose
 *             text, an alternative is the Label available in
 *             <a href="https://vaadin.com/classic-components">Classic
 *             Components</a>. This {@link Label} component /
 *             <code>&lt;label&gt;</code> element is not meant for loose text in
 *             the page - it should only be coupled with another component by
 *             using the {@link #setFor(Component)} or by adding them to it with
 *             the {@link #add(Component...)} method, for example if you use
 *             {@link Input}.
 *
 */
@Tag(Tag.LABEL)
@Deprecated(since = "24.1", forRemoval = true)
public class Label extends HtmlContainer {
    private static final PropertyDescriptor<String, Optional<String>> forDescriptor = PropertyDescriptors
            .optionalAttributeWithDefault("for", "");

    private static Boolean productionMode = null;

    private Registration checkForAttributeOnAttach;

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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (skipForAttributeCheck() || !attachEvent.isInitialAttach()) {
            return; // skip check in production so that customer / clients /
            // ops-teams are not complaining about this warning to the
            // devs. This should be dealt with by devs in development
            // mode.
        }
        if (checkForAttributeOnAttach == null) {
            checkForAttributeOnAttach = attachEvent.getUI()
                    .beforeClientResponse(this, ctx -> {
                        // Label was not associated with a for-attribute
                        // AND
                        // Label was not associated by adding a nested component
                        // AND
                        // Label has no attribute slot=label
                        // (used e.g. in flow-components/FormLayout)
                        if (getFor().isEmpty()
                                && getChildren().findAny().isEmpty()
                                && !Objects.equals(
                                        getElement().getAttribute("slot"),
                                        "label")) {
                            LoggerFactory.getLogger(Label.class.getName()).warn(
                                    "The Label '{}' was not associated with a component. "
                                            + "Labels should not be used for loose text on the page. "
                                            + "Consider alternatives like Text, Paragraph, Span or Div. "
                                            + "See the JavaDocs and Deprecation Warning for more Information.",
                                    getText());
                        }
                        checkForAttributeOnAttach.remove();
                    });
        }
    }

    /**
     * Checks if the application is running in production mode.
     * <p>
     * When unsure, reports that production mode is true so spam-like logging
     * does not take place in production.
     *
     * @return true if in production mode or the mode is unclear, false if in
     *         development mode
     **/
    private static boolean skipForAttributeCheck() {
        if (productionMode != null) {
            return productionMode;
        }

        var service = VaadinService.getCurrent();
        if (service == null) {
            return true;
        }

        productionMode = service.getDeploymentConfiguration()
                .isProductionMode();
        return productionMode;
    }
}
