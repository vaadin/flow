/*
 * Copyright 2000-2022 Vaadin Ltd.
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
 * have an aria-label property to set the accessible name of the component.
 * <p>
 * The default implementations set the aria-label of the component to the given
 * {@link #getElement()}. Override all methods in this interface if the
 * aria-label should be added to some other element.
 * <p>
 * The purpose of aria-label is to provide the user with a recognizable name of
 * the component. If the label text is visible on screen, aria-label should not
 * be used. There may be instances where the name of an element cannot be
 * determined programmatically from the content of the element, and there are
 * cases where providing a visible label is not the desired user experience. In
 * the cases where a visible label or visible tooltip is undesirable, aria-label
 * may be used to set the accessible name of the component.
 * <p>
 * See: https://www.w3.org/TR/wai-aria/#aria-label
 * <p>
 * Note: The aria-label property is not valid on every component, see
 * https://www.w3.org/TR/using-aria/#label-support for more details.
 *
 * @author Vaadin Ltd
 * @since
 */
public interface HasAriaLabel extends HasElement {
    /**
     * Set the aria-label of the component to the given text.
     *
     * @param ariaLabel
     *            the aria-label text to set or {@code null} to clear
     */
    default void setAriaLabel(String ariaLabel) {
        getElement().setProperty(ElementConstants.ARIA_LABEL_PROPERTY_NAME,
                ariaLabel);
    }

    /**
     * Gets the aria-label of the component.
     *
     * @return an optional aria-label of the component if no aria-label has been
     *         set
     */
    default Optional<String> getAriaLabel() {
        return Optional.ofNullable(getElement()
                .getProperty(ElementConstants.ARIA_LABEL_PROPERTY_NAME, null));
    }
}
