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

import com.vaadin.flow.dom.ElementConstants;

/**
 * A generic interface for components and other user interface objects that may
 * have an aria-description and an aria-describedby DOM attributes to set the
 * accessible description of the component.
 * <p>
 * The default implementation sets the aria-description and aria-describedby of
 * the component to the given {@link #getElement()}. Override all methods in
 * this interface if the aria-description and aria-describedby should be added
 * to some other element.
 * <p>
 * The purpose of these attributes is to provide the user with additional
 * descriptive text that complements the accessible name of the component. If
 * the description text is visible on screen, aria-describedby <b>should</b> be
 * used and aria-description <b>should not</b> be used.
 * <p>
 * If both attributes are present on the same element, aria-describedby takes
 * precedence over aria-description.
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
     * @return the aria-description of the component or {@code null} if no
     *         aria-description has been set
     */
    default String getAriaDescription() {
        return getElement()
                .getAttribute(ElementConstants.ARIA_DESCRIPTION_ATTRIBUTE_NAME);
    }

    /**
     * Set the aria-describedby of the component. The value must be a valid id
     * attribute of another element that describes the component. The
     * description element <b>must</b> be in the same DOM scope of the
     * component, otherwise screen readers may fail to announce the description
     * content properly.
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
     * Set the aria-describedby of the component to reference the given
     * description component. If {@code descriptionComponent} does not have an
     * id, one is generated automatically.
     * <p>
     * The id is resolved lazily before the next client response after this
     * component is attached, so the description component's id can be set after
     * calling this method. If no id is set by then, one will be generated.
     * <p>
     * The description component <b>must</b> be in the same DOM scope as this
     * component, otherwise screen readers may fail to announce the description
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
     * @return the aria-describedby of the component or {@code null} if no
     *         aria-describedby has been set
     */
    default String getAriaDescribedBy() {
        return getElement()
                .getAttribute(ElementConstants.ARIA_DESCRIBEDBY_ATTRIBUTE_NAME);
    }
}
