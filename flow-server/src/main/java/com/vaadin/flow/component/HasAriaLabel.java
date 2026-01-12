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

import com.vaadin.flow.dom.ElementConstants;

/**
 * A generic interface for components and other user interface objects that may
 * have an aria-label and an aria-labelledby DOM attributes to set the
 * accessible name of the component.
 * <p>
 * The default implementation set the aria-label and aria-labelledby of the
 * component to the given {@link #getElement()}. Override all methods in this
 * interface if the aria-label and aria-labelledby should be added to some other
 * element.
 * <p>
 * The purpose of aria-label is to provide the user with a recognizable name of
 * the component. If the label text is visible on screen, aria-labelledby
 * <b>should</b> be used and aria-label <b>should not</b> be used. There may be
 * instances where the name of an element cannot be determined programmatically
 * from the content of the element, and there are cases where providing a
 * visible label is not the desired user experience. In the cases where a
 * visible label or visible tooltip is undesirable, aria-label may be used to
 * set the accessible name of the component.
 * <p>
 * <b>Don't include both</b>. If both are present on the same element,
 * aria-labelledby will take precedence over aria-label.
 * <p>
 * See: https://www.w3.org/TR/wai-aria/#aria-label
 * <p>
 * See: https://www.w3.org/TR/wai-aria/#aria-labelledby
 * <p>
 * Note: The aria-label and aria-labelledby attributes are not valid on every
 * component, see https://www.w3.org/TR/using-aria/#label-support for more
 * details.
 *
 * @author Vaadin Ltd
 */
public interface HasAriaLabel extends HasElement {
    /**
     * Set the aria-label of the component to the given text.
     * <p>
     * This method should not be used if {@link #setAriaLabelledBy(String)} is
     * also used. If both attributes are present, aria-labelledby will take
     * precedence over aria-label.
     *
     * @param ariaLabel
     *            the aria-label text to set or {@code null} to clear
     */
    default void setAriaLabel(String ariaLabel) {
        if (ariaLabel != null) {
            getElement().setAttribute(
                    ElementConstants.ARIA_LABEL_ATTRIBUTE_NAME, ariaLabel);
        } else {
            getElement().removeAttribute(
                    ElementConstants.ARIA_LABEL_ATTRIBUTE_NAME);
        }
    }

    /**
     * Gets the aria-label of the component.
     *
     * @return an optional aria-label of the component if no aria-label has been
     *         set
     */
    default Optional<String> getAriaLabel() {
        return Optional.ofNullable(getElement()
                .getAttribute(ElementConstants.ARIA_LABEL_ATTRIBUTE_NAME));
    }

    /**
     * Set the aria-labelledby of the component. The value must be a valid id
     * attribute of another element that labels the component. The label element
     * <b>must</b> be in the same DOM scope of the component, otherwise screen
     * readers may fail to announce the label content properly.
     * <p>
     * This method should not be used if {@link #setAriaLabel(String)} is also
     * used. If both attributes are present, aria-labelledby will take
     * precedence over aria-label.
     *
     * @param ariaLabelledBy
     *            the string with the id of the element that will be used as
     *            label or {@code null} to clear
     */
    default void setAriaLabelledBy(String ariaLabelledBy) {
        if (ariaLabelledBy != null) {
            getElement().setAttribute(
                    ElementConstants.ARIA_LABELLEDBY_ATTRIBUTE_NAME,
                    ariaLabelledBy);
        } else {
            getElement().removeAttribute(
                    ElementConstants.ARIA_LABELLEDBY_ATTRIBUTE_NAME);
        }
    }

    /**
     * Gets the aria-labelledby of the component
     *
     * @return an optional aria-labelledby of the component if no
     *         aria-labelledby has been set
     */
    default Optional<String> getAriaLabelledBy() {
        return Optional.ofNullable(getElement()
                .getAttribute(ElementConstants.ARIA_LABELLEDBY_ATTRIBUTE_NAME));
    }
}
