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
 * A generic interface for components that have an accessible description.
 * <p>
 * The accessible description provides supplementary information about the
 * component to assistive technologies, such as screen readers.
 * <p>
 * It can be set as plain text with the {@code aria-description} attribute, or
 * by referencing existing elements on the page with the
 * {@code aria-describedby} attribute. Prefer the latter when the description is
 * already visible to all users.
 *
 * @author Vaadin Ltd
 * @since 25.3
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-description">
 *      MDN: aria-description</a>
 * @see <a href=
 *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-describedby">
 *      MDN: aria-describedby</a>
 */
public interface HasAriaDescription extends HasElement {

    /**
     * Sets the {@code aria-description} attribute of the component to the given
     * text.
     * <p>
     * If both {@code aria-description} and {@code aria-describedby} are
     * present, {@code aria-describedby} takes precedence.
     *
     * @param ariaDescription
     *            the description text, or {@code null} to remove the attribute
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-description">
     *      MDN: aria-description</a>
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
     * Gets the {@code aria-description} attribute of the component.
     *
     * @return an optional aria-description, or an empty optional if none has
     *         been set
     */
    default Optional<String> getAriaDescription() {
        return Optional.ofNullable(getElement().getAttribute(
                ElementConstants.ARIA_DESCRIPTION_ATTRIBUTE_NAME));
    }

    /**
     * Sets the {@code aria-describedby} attribute of the component to one or
     * more element IDs, separated by spaces.
     * <p>
     * Each ID must match the {@code id} attribute of another element that
     * describes the component. The description elements must be in the same DOM
     * scope as the component, otherwise screen readers will fail to announce
     * the description content properly.
     *
     * @param ariaDescribedBy
     *            a space-separated list of element IDs, or {@code null} to
     *            remove the attribute
     * @see <a href=
     *      "https://developer.mozilla.org/en-US/docs/Web/Accessibility/ARIA/Reference/Attributes/aria-describedby">
     *      MDN: aria-describedby</a>
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
     * Sets the {@code aria-describedby} attribute of the component to reference
     * the given description component.
     * <p>
     * The description component does not need an ID: if it has none by the time
     * the value is sent to the client, one is generated automatically. The
     * attribute value is available right away, but a generated ID is only
     * assigned to the description component before the next client response
     * after this component is attached, so the value read with
     * {@link #getAriaDescribedBy()} should not be cached within the same
     * request.
     * <p>
     * Calling {@link #setAriaDescribedBy(String)}, also with {@code null} to
     * remove the attribute, or referencing another component cancels a pending
     * resolution, leaving the previously referenced component's ID untouched.
     * <p>
     * The description component must be in the same DOM scope as this
     * component, otherwise screen readers will fail to announce the description
     * content properly.
     *
     * @param descriptionComponent
     *            the component to use as the description, not {@code null}
     * @throws IllegalArgumentException
     *             if {@code descriptionComponent} is {@code null}; use
     *             {@link #setAriaDescribedBy(String)} with {@code null} to
     *             remove the attribute instead
     * @see #setAriaDescribedBy(String)
     */
    default void setAriaDescribedBy(Component descriptionComponent) {
        if (descriptionComponent == null) {
            throw new IllegalArgumentException(
                    "The provided component cannot be null");
        }
        ComponentUtil.resolveOrGenerateIdLater(getElement(),
                descriptionComponent, "ariadescribedby-",
                this::getAriaDescribedBy, this::setAriaDescribedBy);
    }

    /**
     * Gets the {@code aria-describedby} attribute of the component.
     *
     * @return an optional aria-describedby, or an empty optional if none has
     *         been set
     */
    default Optional<String> getAriaDescribedBy() {
        return Optional.ofNullable(getElement().getAttribute(
                ElementConstants.ARIA_DESCRIBEDBY_ATTRIBUTE_NAME));
    }
}
