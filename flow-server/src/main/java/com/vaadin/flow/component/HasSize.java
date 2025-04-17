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

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementConstants;
import com.vaadin.flow.server.Constants;

/**
 * Any component implementing this interface supports setting the size of the
 * component using {@link #setWidth(String)}, {@link #setHeight(String)},
 * {@link #setMaxWidth(String)}, {@link #setMaxHeight(String)},
 * {@link #setMinWidth(String)}, and {@link #setMinHeight(String)}. The sizes
 * are set on the element as inline styles, i.e. using
 * {@link Element#getStyle()}.
 *
 * @author Vaadin Ltd
 * @since 1.0
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
        getElement().getStyle().setWidth(width);
        getElement().removeAttribute(Constants.ATTRIBUTE_WIDTH_FULL);
    }

    /**
     * Sets the width of the component. Negative number implies unspecified size
     * (terminal is free to set the size).
     *
     * @param width
     *            the width of the object.
     * @param unit
     *            the unit used for the width.
     */
    default void setWidth(float width, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        setWidth(getCssSize(width, unit));
    }

    /**
     * Sets the min-width of the component.
     * <p>
     * The width should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code minWidth} value is {@literal null} then min-width
     * is removed.
     *
     * @param minWidth
     *            the min-width value (if <code>null</code>, the property will
     *            be removed)
     */
    default void setMinWidth(String minWidth) {
        getElement().getStyle().set(ElementConstants.STYLE_MIN_WIDTH, minWidth);
    }

    /**
     * Sets the min-width of the component. Negative number implies unspecified
     * size (terminal is free to set the size).
     *
     * @param minWidth
     *            the min-width of the object.
     * @param unit
     *            the unit used for the min-width.
     */
    default void setMinWidth(float minWidth, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        setMinWidth(getCssSize(minWidth, unit));
    }

    /**
     * Sets the max-width of the component.
     * <p>
     * The width should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code maxWidth} value is {@literal null} then max-width
     * is removed.
     *
     * @param maxWidth
     *            the max-width value (if <code>null</code>, the property will
     *            be removed)
     */
    default void setMaxWidth(String maxWidth) {
        getElement().getStyle().set(ElementConstants.STYLE_MAX_WIDTH, maxWidth);
    }

    /**
     * Sets the max-width of the component. Negative number implies unspecified
     * size (terminal is free to set the size).
     *
     * @param maxWidth
     *            the max-width of the object.
     * @param unit
     *            the unit used for the max-width.
     */
    default void setMaxWidth(float maxWidth, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        setMaxWidth(getCssSize(maxWidth, unit));
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
     *
     * Gets the min-width defined for the component.
     * <p>
     * Note that this does not return the actual size of the component but the
     * min-width which has been set using {@link #setMinWidth(String)}.
     *
     * @return the min-width which has been set for the component
     */
    default String getMinWidth() {
        return getElement().getStyle().get(ElementConstants.STYLE_MIN_WIDTH);
    }

    /**
     *
     * Gets the max-width defined for the component.
     * <p>
     * Note that this does not return the actual size of the component but the
     * max-width which has been set using {@link #setMaxWidth(String)}.
     *
     * @return the max-width which has been set for the component
     */
    default String getMaxWidth() {
        return getElement().getStyle().get(ElementConstants.STYLE_MAX_WIDTH);
    }

    /**
     * Gets the width unit of the component, if defined.
     *
     * @return an optional width unit for the component, or an empty optional if
     *         no width unit has been set
     */
    default Optional<Unit> getWidthUnit() {
        return Unit.getUnit(getWidth());
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
        getElement().getStyle().setHeight(height);
        getElement().removeAttribute(Constants.ATTRIBUTE_HEIGHT_FULL);
    }

    /**
     * Sets the height of the component. Negative number implies unspecified
     * size (terminal is free to set the size).
     *
     * @param height
     *            the height of the object.
     * @param unit
     *            the unit used for the height.
     */
    default void setHeight(float height, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        setHeight(getCssSize(height, unit));
    }

    /**
     * Sets the min-height of the component.
     * <p>
     * The height should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code minHeight} value is {@literal null} then
     * min-height is removed.
     *
     * @param minHeight
     *            the min-height value (if <code>null</code>, the property will
     *            be removed)
     */
    default void setMinHeight(String minHeight) {
        getElement().getStyle().set(ElementConstants.STYLE_MIN_HEIGHT,
                minHeight);
    }

    /**
     * Sets the min-height of the component. Negative number implies unspecified
     * size (terminal is free to set the size).
     *
     * @param minHeight
     *            the min-height of the object.
     * @param unit
     *            the unit used for the min-height.
     */
    default void setMinHeight(float minHeight, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        setMinHeight(getCssSize(minHeight, unit));
    }

    /**
     * Sets the max-height of the component.
     * <p>
     * The height should be in a format understood by the browser, e.g. "100px"
     * or "2.5em".
     * <p>
     * If the provided {@code maxHeight} value is {@literal null} then
     * max-height is removed.
     *
     * @param maxHeight
     *            the max-height value (if <code>null</code>, the property will
     *            be removed)
     */
    default void setMaxHeight(String maxHeight) {
        getElement().getStyle().set(ElementConstants.STYLE_MAX_HEIGHT,
                maxHeight);
    }

    /**
     * Sets the max-height of the component. Negative number implies unspecified
     * size (terminal is free to set the size).
     *
     * @param maxHeight
     *            the max-height of the object.
     * @param unit
     *            the unit used for the max-height.
     */
    default void setMaxHeight(float maxHeight, Unit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("Unit can not be null");
        }
        setMaxHeight(getCssSize(maxHeight, unit));
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
     * Gets the min-height defined for the component.
     * <p>
     * Note that this does not return the actual size of the component but the
     * min-height which has been set using {@link #setMinHeight(String)}.
     *
     * @return the min-height which has been set for the component
     */
    default String getMinHeight() {
        return getElement().getStyle().get(ElementConstants.STYLE_MIN_HEIGHT);
    }

    /**
     * Gets the max-height defined for the component.
     * <p>
     * Note that this does not return the actual size of the component but the
     * max-height which has been set using {@link #setMaxHeight(String)}.
     *
     * @return the max-height which has been set for the component
     */
    default String getMaxHeight() {
        return getElement().getStyle().get(ElementConstants.STYLE_MAX_HEIGHT);
    }

    /**
     * Gets the height unit of the component, if defined.
     *
     * @return an optional height unit for the component, or an empty optional
     *         if no height unit has been set
     */
    default Optional<Unit> getHeightUnit() {
        return Unit.getUnit(getHeight());
    }

    /**
     * Sets the width and the height of the component to "100%".
     * <p>
     * This is just a convenience method which delegates its call to the
     * {@link #setWidth(String)} and {@link #setHeight(String)} methods with
     * {@literal "100%"} as the argument value
     * <p>
     * When adding full-size components as a child of a Vaadin layout component
     * that is based on CSS Flexbox, such as HorizontalLayout or VerticalLayout,
     * this can result in unexpected behavior. For example, other components
     * with a fixed size may shrink to allow the full-size component to take up
     * as much space as possible, or the full-size component may cause the
     * layout to overflow. To improve this, you can enable the
     * {@code com.vaadin.experimental.layoutComponentImprovements} feature flag
     * to effectively make full-size components take up the <b>remaining</b>
     * space in the layout, rather than explicitly using 100% size of the
     * layout. This applies additional CSS styles that allow the component to
     * shrink below 100% if there are other components with fixed or relative
     * sizes in the layout.
     */
    default void setSizeFull() {
        setWidthFull();
        setHeightFull();
    }

    /**
     * Sets the width of the component to "100%".
     * <p>
     * This is just a convenience method which delegates its call to the
     * {@link #setWidth(String)} with {@literal "100%"} as the argument value.
     * <p>
     * When adding full-size components as a child of a Vaadin layout component
     * that is based on CSS Flexbox, such as HorizontalLayout or VerticalLayout,
     * this can result in unexpected behavior. For example, other components
     * with a fixed size may shrink to allow the full-size component to take up
     * as much space as possible, or the full-size component may cause the
     * layout to overflow. To improve this, you can enable the
     * {@code com.vaadin.experimental.layoutComponentImprovements} feature flag
     * to effectively make full-size components take up the <b>remaining</b>
     * space in the layout, rather than explicitly using 100% size of the
     * layout. This applies additional CSS styles that allow the component to
     * shrink below 100% if there are other components with fixed or relative
     * sizes in the layout.
     */
    default void setWidthFull() {
        setWidth("100%");
        getElement().setAttribute(Constants.ATTRIBUTE_WIDTH_FULL, true);
    }

    /**
     * Sets the height of the component to "100%".
     * <p>
     * This is just a convenience method which delegates its call to the
     * {@link #setHeight(String)} with {@literal "100%"} as the argument value
     * <p>
     * When adding full-size components as a child of a Vaadin layout component
     * that is based on CSS Flexbox, such as HorizontalLayout or VerticalLayout,
     * this can result in unexpected behavior. For example, other components
     * with a fixed size may shrink to allow the full-size component to take up
     * as much space as possible, or the full-size component may cause the
     * layout to overflow. To improve this, you can enable the
     * {@code com.vaadin.experimental.layoutComponentImprovements} feature flag
     * to effectively make full-size components take up the <b>remaining</b>
     * space in the layout, rather than explicitly using 100% size of the
     * layout. This applies additional CSS styles that allow the component to
     * shrink below 100% if there are other components with fixed or relative
     * sizes in the layout.
     */
    default void setHeightFull() {
        setHeight("100%");
        getElement().setAttribute(Constants.ATTRIBUTE_HEIGHT_FULL, true);
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

    /**
     * Utility method for internal use.
     *
     * @param size
     *            Size
     * @param unit
     *            Unit
     * @return Css format size string
     */
    static String getCssSize(float size, Unit unit) {
        if (size < 0) {
            return null;
        }
        return size + unit.toString();
    }
}
