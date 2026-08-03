/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.dom.ElementConstants;

/**
 * A generic interface for components that support setting the accessible
 * description. The description is announced by screen readers when the
 * component receives focus.
 * <p>
 * The description can be provided either as plain text with
 * {@link #setAriaDescription(String)}, or with
 * {@link #setAriaDescribedBy(String)} by referencing another element whose
 * content serves as the description. If both are set, the element reference
 * takes precedence.
 *
 * @author Vaadin Ltd
 * @since 25.3
 */
public interface HasAriaDescription extends HasElement {

    /**
     * Set the aria-description of the component to the given text.
     * <p>
     * If both aria-description and aria-describedby are present,
     * aria-describedby takes precedence.
     * <p>
     * See: https://www.w3.org/TR/wai-aria/#aria-description
     *
     * @param ariaDescription
     *            the aria-description text to set or {@code null} to clear
     */
    default void setAriaDescription(String ariaDescription) {
        if (ariaDescription != null) {
            getElement().setAttribute(
                    ElementConstants.ARIA_DESCRIPTION_ATTRIBUTE_NAME,
                    ariaDescription);
        } else {
            getElement().removeAttribute(
                    ElementConstants.ARIA_DESCRIPTION_ATTRIBUTE_NAME);
        }
    }

    /**
     * Gets the aria-description of the component.
     *
     * @return an optional aria-description, or an empty optional if none has
     *         been set
     */
    default Optional<String> getAriaDescription() {
        return Optional.ofNullable(getElement().getAttribute(
                ElementConstants.ARIA_DESCRIPTION_ATTRIBUTE_NAME));
    }

    /**
     * Sets the aria-describedby of the component.
     * <p>
     * The value must be a valid id attribute of another element that describes
     * the component. The description element must be in the same DOM scope of
     * the component, otherwise screen readers will fail to announce the
     * description content properly.
     * <p>
     * See: https://www.w3.org/TR/wai-aria/#aria-describedby
     *
     * @param ariaDescribedBy
     *            the string with the id of the element that will be used as
     *            description or {@code null} to clear
     */
    default void setAriaDescribedBy(String ariaDescribedBy) {
        if (ariaDescribedBy != null) {
            getElement().setAttribute(
                    ElementConstants.ARIA_DESCRIBEDBY_ATTRIBUTE_NAME,
                    ariaDescribedBy);
        } else {
            getElement().removeAttribute(
                    ElementConstants.ARIA_DESCRIBEDBY_ATTRIBUTE_NAME);
        }
    }

    /**
     * Sets the aria-describedby of the component to reference the given
     * description component.
     * <p>
     * The description component does not need an id: if it has none by the time
     * the value is sent to the client, one is generated automatically.
     * <p>
     * The description component must be in the same DOM scope as this
     * component, otherwise screen readers will fail to announce the description
     * content properly.
     * <p>
     * See: https://www.w3.org/TR/wai-aria/#aria-describedby
     *
     * @param descriptionComponent
     *            the component to use as the description, not {@code null}
     */
    default void setAriaDescribedBy(Component descriptionComponent) {
        if (descriptionComponent == null) {
            throw new IllegalArgumentException(
                    "The provided component cannot be null");
        }
        ComponentUtil.resolveOrGenerateIdLater(getElement(),
                descriptionComponent, "ariadescribedby-",
                this::setAriaDescribedBy);
    }

    /**
     * Gets the aria-describedby of the component.
     *
     * @return an optional aria-describedby, or an empty optional if none has
     *         been set
     */
    default Optional<String> getAriaDescribedBy() {
        return Optional.ofNullable(getElement().getAttribute(
                ElementConstants.ARIA_DESCRIBEDBY_ATTRIBUTE_NAME));
    }
}
