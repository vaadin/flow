/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

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

    enum WhiteSpace {
        NORMAL, NOWRAP, PRE, PRE_WRAP, PRE_LINE, BREAK_SPACES, INHERIT, INITIAL;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH).replace('_', '-');
        }

        public static WhiteSpace forString(String value) {
            return Stream.of(values())
                    .filter(whiteSpace -> whiteSpace.toString().equals(value))
                    .findFirst().orElse(null);
        }
    }

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

    /**
     * Sets the given {@code value} as {@code "white-space"} style value.
     * 
     * @param value
     *            the {@code "white-space"} style value, not {@code null}
     */
    default void setWhiteSpace(WhiteSpace value) {
        getElement().getStyle().set("white-space",
                Objects.requireNonNull(value).toString());
    }

    /**
     * Gets the {@code "white-space"} style value.
     * <p>
     * The default value is {@literal WhiteSpace#NORMAL}. If the
     * {@code "white-space"} style value is non standard then {@code null} is
     * returned.
     * 
     * @return the {@code "white-space"} style value, may be {@code null}
     */
    default WhiteSpace getWhiteSpace() {
        String value = getElement().getStyle().get("white-space");
        if (value == null) {
            return WhiteSpace.NORMAL;
        }
        return WhiteSpace.forString(value);
    }
}
