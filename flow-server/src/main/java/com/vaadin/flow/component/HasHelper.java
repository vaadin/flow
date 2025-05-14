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
package com.vaadin.flow.component;

import java.util.Optional;

import com.vaadin.flow.dom.Element;

/**
 * Mixin interface for field components that have helper text as property and
 * slots for inserting components.
 * <p>
 * Root element should be a web component that uses slot for example in the
 * following way:
 *
 * <pre>{@code
 *     <field-with-helper>
 *         <shadow-root>
 *             ...
 *             <slot name="helper"></slot>
 *             ...
 *         </shadow-root>
 *         <span slot="helper">${helperText}</span>
 *     </field-with-helper>
 * }</pre>
 *
 * @author Vaadin Ltd
 * @since 2.4
 */
public interface HasHelper extends HasElement {
    /**
     * String used for the helper text.
     *
     * @return the {@code helperText} property from the web component
     */
    default String getHelperText() {
        return getElement().getProperty("helperText");
    }

    /**
     * <p>
     * String used for the helper text. It shows a text adjacent to the field
     * that can be used, e.g., to inform to the users which values it expects.
     * Example: a text "The password must contain numbers" for the
     * PasswordField.
     * </p>
     *
     * <p>
     * In case both {@link #setHelperText(String)} and
     * {@link #setHelperComponent(Component)} are used, only the element defined
     * by {@link #setHelperComponent(Component)} will be visible, regardless of
     * the order on which they are defined.
     * </p>
     *
     * @param helperText
     *            the String value to set
     */
    default void setHelperText(String helperText) {
        getElement().setProperty("helperText", helperText);
    }

    /**
     * Adds the given component into helper slot of component, replacing any
     * existing helper component. It adds the component adjacent to the field
     * that can be used, e.g., to inform to the users which values it expects.
     * Example: a component that shows the password strength for the
     * PasswordField.
     *
     * @param component
     *            the component to set, can be {@code null} to remove existing
     *            helper component
     *
     * @see #setHelperText(String)
     */
    default void setHelperComponent(Component component) {
        getElement().getChildren()
                .filter(child -> "helper".equals(child.getAttribute("slot")))
                .findAny().ifPresent(getElement()::removeChild);

        if (component != null) {
            component.getElement().setAttribute("slot", "helper");
            getElement().appendChild(component.getElement());
        }
    }

    /**
     * Gets the component in the helper slot of this field.
     *
     * @return the helper component of this field, or {@code null} if no helper
     *         component has been set
     * @see #setHelperComponent(Component)
     */
    default Component getHelperComponent() {
        Optional<Component> component = getElement().getChildren()
                .filter(child -> "helper".equals(child.getAttribute("slot")))
                .map(Element::getComponent).findFirst()
                .orElse(Optional.empty());

        return component.orElse(null);
    }
}
