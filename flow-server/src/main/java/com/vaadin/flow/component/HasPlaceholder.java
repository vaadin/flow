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

/**
 * A component which supports a placeholder.
 * <p>
 * A placeholder is a text that should be displayed in the input element, when
 * the user has not entered a value.
 * <p>
 * The default implementations sets the <code>placeholder</code> property for
 * this element. Override all methods in this interface if the placeholder
 * should be set in some other way.
 */
public interface HasPlaceholder extends HasElement {
    /**
     * Sets the placeholder text that should be displayed in the input element,
     * when the user has not entered a value
     *
     * @param placeholder
     *            the placeholder text, may be null.
     */
    default void setPlaceholder(String placeholder) {
        getElement().setProperty("placeholder",
                placeholder == null ? "" : placeholder);
    }

    /**
     * The placeholder text that should be displayed in the input element, when
     * the user has not entered a value
     *
     * @return the {@code placeholder} property from the web component. May be
     *         null if not yet set.
     */
    default String getPlaceholder() {
        return getElement().getProperty("placeholder");
    }
}
