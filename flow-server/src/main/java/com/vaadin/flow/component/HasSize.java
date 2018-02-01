/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementConstants;

/**
 * Any component implementing this interface supports setting the size of the
 * component using {@link #setWidth(String)} and {@link #setHeight(String)}. The
 * sizes are set on the element as inline styles, i.e. using
 * {@link Element#getStyle()}.
 *
 * @author Vaadin Ltd
 */
public interface HasSize extends HasElement {

    /**
     * Sets the width of the component.
     * <p>
     * The width should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code width} value is {@literal null} then width is
     * removed.
     *
     * @param width
     *            the width to set, may be {@code null}
     */
    default void setWidth(String width) {
        getElement().getStyle().set(ElementConstants.STYLE_WIDTH, width);
    }

    /**
     *
     * Gets the width defined for the component.
     * <p>
     * Note that this does not return the actual size of the component but the
     * width which has been set using {@link #setWidth(String)}.
     *
     * @return the width which has been set for the component
     */
    default String getWidth() {
        return getElement().getStyle().get(ElementConstants.STYLE_WIDTH);
    }

    /**
     * Sets the height of the component.
     * <p>
     * The height should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code height} value is {@literal null} then height is
     * removed.
     *
     * @param height
     *            the height to set, may be {@code null}
     */
    default void setHeight(String height) {
        getElement().getStyle().set(ElementConstants.STYLE_HEIGHT, height);
    }

    /**
     * Gets the height defined for the component.
     * <p>
     * Note that this does not return the actual size of the component but the
     * height which has been set using {@link #setHeight(String)}.
     *
     * @return the height which has been set for the component
     */
    default String getHeight() {
        return getElement().getStyle().get(ElementConstants.STYLE_HEIGHT);
    }

    /**
     * Sets the width and the height of the component to "100%".
     * <p>
     * This is just a convenience method which delegates its call to the
     * {@link #setWidth(String)} and {@link #setHeight(String)} methods with
     * {@literal "100%"} as the argument value
     */
    default void setSizeFull() {
        setWidth("100%");
        setHeight("100%");
    }

    /**
     * Removes the width and the height of the component.
     * <p>
     * This is just a convenience method which delegates its call to the
     * {@link #setWidth(String)} and {@link #setHeight(String)} methods with
     * {@literal null} as the argument value
     */
    default void setSizeUndefined() {
        setWidth(null);
        setHeight(null);
    }
}
