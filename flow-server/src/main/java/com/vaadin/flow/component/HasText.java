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
package com.vaadin.flow.component;

/**
 * A component that supports text content.
 * <p>
 * The default implementations set the text as text content of
 * {@link #getElement()}. Override all methods in this interface if the text
 * should be added to some other element.
 *
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface HasText extends HasElement {

    /**
     * Sets the given string as the content of this component. This removes any
     * existing child components and child elements. To mix text and child
     * components in a component that also supports child components, use
     * {@link HasComponents#add(Component...)} with the {@link Text} component
     * for the textual parts.
     *
     * @param text
     *            the text content to set
     */
    default void setText(String text) {
        getElement().setText(text);
    }

    /**
     * Gets the text content of this component. This method only considers the
     * text of the actual component. The text contents of any child components
     * or elements are not considered.
     *
     * @return the text content of this component, not <code>null</code>
     */
    default String getText() {
        return getElement().getText();
    }
}
