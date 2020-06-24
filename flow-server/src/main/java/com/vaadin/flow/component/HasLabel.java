/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 * A component that supports label definition.
 * <p>
 * The default implementations set the label of the component to the given text for
 * {@link #getElement()}. Override all methods in this interface if the text
 * should be added to some other element.
 *
 *
 * @author Vaadin Ltd
 * @since
 */
public interface HasLabel extends HasElement{
    /**
     * Set the label of the component to the given text.
     *
     * @param label
     *            the label text to set or {@code null} to clear
     */
    default void setLabel(String label) {
        getElement().setProperty(ElementConstants.LABEL_PROPERTY_NAME, label);
    }

    /**
     * Gets the label of the component.
     *
     * @return the label of the component or {@code null} if no label has
     *         been set
     */
    default String getLabel() {
        return getElement().getProperty(ElementConstants.LABEL_PROPERTY_NAME, null);
    }
}
