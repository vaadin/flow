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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.stream.Stream;

import com.vaadin.signals.BindingActiveException;
import com.vaadin.signals.Signal;

import com.vaadin.flow.component.page.ColorScheme;

import static com.vaadin.flow.dom.ElementConstants.STYLE_ALIGN_ITEMS;
import static com.vaadin.flow.dom.ElementConstants.STYLE_ALIGN_SELF;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BACKGROUND;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BACKGROUND_COLOR;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BACKGROUND_POSITION;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BACKGROUND_SIZE;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BORDER;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BORDER_BOTTOM;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BORDER_LEFT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BORDER_RADIUS;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BORDER_RIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BORDER_TOP;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BOTTOM;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BOX_SHADOW;
import static com.vaadin.flow.dom.ElementConstants.STYLE_BOX_SIZING;
import static com.vaadin.flow.dom.ElementConstants.STYLE_CLEAR;
import static com.vaadin.flow.dom.ElementConstants.STYLE_COLOR;
import static com.vaadin.flow.dom.ElementConstants.STYLE_COLOR_SCHEME;
import static com.vaadin.flow.dom.ElementConstants.STYLE_CURSOR;
import static com.vaadin.flow.dom.ElementConstants.STYLE_DISPLAY;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FILTER;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FLEX_BASIS;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FLEX_DIRECTION;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FLEX_GROW;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FLEX_SHRINK;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FLEX_WRAP;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FLOAT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FONT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FONT_SIZE;
import static com.vaadin.flow.dom.ElementConstants.STYLE_FONT_WEIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_GAP;
import static com.vaadin.flow.dom.ElementConstants.STYLE_HEIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_JUSTIFY_CONTENT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_LEFT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_LINE_HEIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN_BOTTOM;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN_INLINE_END;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN_INLINE_START;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN_LEFT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN_RIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MARGIN_TOP;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MAX_HEIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MAX_WIDTH;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MIN_HEIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_MIN_WIDTH;
import static com.vaadin.flow.dom.ElementConstants.STYLE_OPACITY;
import static com.vaadin.flow.dom.ElementConstants.STYLE_OUTLINE;
import static com.vaadin.flow.dom.ElementConstants.STYLE_OVERFLOW;
import static com.vaadin.flow.dom.ElementConstants.STYLE_PADDING;
import static com.vaadin.flow.dom.ElementConstants.STYLE_PADDING_BOTTOM;
import static com.vaadin.flow.dom.ElementConstants.STYLE_PADDING_LEFT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_PADDING_RIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_PADDING_TOP;
import static com.vaadin.flow.dom.ElementConstants.STYLE_POSITION;
import static com.vaadin.flow.dom.ElementConstants.STYLE_RIGHT;
import static com.vaadin.flow.dom.ElementConstants.STYLE_ROTATE;
import static com.vaadin.flow.dom.ElementConstants.STYLE_SCALE;
import static com.vaadin.flow.dom.ElementConstants.STYLE_TEXT_ALIGN;
import static com.vaadin.flow.dom.ElementConstants.STYLE_TEXT_DECORATION;
import static com.vaadin.flow.dom.ElementConstants.STYLE_TOP;
import static com.vaadin.flow.dom.ElementConstants.STYLE_TRANSFORM;
import static com.vaadin.flow.dom.ElementConstants.STYLE_TRANSFORM_ORIGIN;
import static com.vaadin.flow.dom.ElementConstants.STYLE_TRANSITION;
import static com.vaadin.flow.dom.ElementConstants.STYLE_VISIBILITY;
import static com.vaadin.flow.dom.ElementConstants.STYLE_WHITE_SPACE;
import static com.vaadin.flow.dom.ElementConstants.STYLE_WIDTH;
import static com.vaadin.flow.dom.ElementConstants.STYLE_Z_INDEX;

/**
 * Provides inline styles for {@link Element}s.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface Style extends Serializable {

    /**
     * Gets the value of the given style property.
     * <p>
     * Note that the name should be in camelCase and not dash-separated, i.e.,
     * use "fontFamily" and not "font-family".
     * <p>
     * Returns the value of the latest signal applied for a bound style while
     * the element was in the attached state.
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     * @return the style property value, or <code>null</code> if the style
     *         property has not been set
     * @see #bind(String, Signal)
     */
    String get(String name);

    /**
     * Sets the given style property to the given value.
     * <p>
     * Both camelCased (e.g. <code>fontFamily</code>) and dash-separated (e.g.
     * <code>font-family</code> versions are supported.
     * <p>
     * While a binding for a specific style name is active, any attempt to
     * manually set that same style throws a {@link BindingActiveException}.
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     * @see #bind(String, Signal)
     */
    Style set(String name, String value);

    /**
     * Removes the given style property if it has been set.
     * <p>
     * Both camelCased (e.g. <code>fontFamily</code>) and dash-separated (e.g.
     * <code>font-family</code> versions are supported.
     * <p>
     * While a binding for a specific style name is active, any attempt to
     * manually remove that same style throws a {@link BindingActiveException}.
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     * @return this style instance
     * @see #bind(String, Signal)
     */
    Style remove(String name);

    /**
     * Removes all set style properties.
     * <p>
     * This method silently clears all style signal bindings (unsubscribe and
     * forget recorded values) in addition to clearing style values.
     *
     * @return this style instance
     * @see #bind(String, Signal)
     */
    Style clear();

    /**
     * Checks if the given style property has been set.
     * <p>
     * Both camelCased (e.g. <code>fontFamily</code>) and dash-separated (e.g.
     * <code>font-family</code> versions are supported.
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     *
     * @return <code>true</code> if the style property has been set,
     *         <code>false</code> otherwise
     */
    boolean has(String name);

    /**
     * Gets the defined style property names.
     * <p>
     * Note that this always returns the name as camelCased, e.g.
     * <code>fontFamily</code> even if it has been set as dash-separated
     * (<code>font-family</code>).
     * <p>
     * Includes names of the style properties bound with the signals while the
     * element was in the attached state.
     *
     * @return a stream of defined style property names
     * @see #bind(String, Signal)
     */
    Stream<String> getNames();

    /**
     * Binds the given style property to the provided string signal and keeps
     * the style property value synchronized with the signal.
     * <p>
     * Passing {@code null} as the {@code signal} removes any existing binding
     * for the given style property. When unbinding, the current presence of the
     * style property is left unchanged.
     * <p>
     * When a binding is in place, the style signal mirrors
     * {@code signal.value()}. If the signal value is {@code null}, the style
     * property is removed; otherwise it is set to the string value.
     * <p>
     * The binding effect is active only while the owner element is in the
     * attached state. While the owner is in the detached state, updates from
     * the signal have no effect.
     * <p>
     * While a binding for a specific style name is active, any attempt to bind
     * another signal for the same name throws a {@link BindingActiveException}.
     * <p>
     * Name handling follows the same rules as {@link #set(String, String)}:
     * both camelCase and dash-separated names are supported and normalized in
     * the same way.
     *
     * @param name
     *            the style property name, not {@code null}
     * @param signal
     *            the signal that provides the style signal; {@code null}
     *            removes an existing binding for the given name
     * @return this style instance
     * @throws BindingActiveException
     *             thrown when there is already an existing binding
     * @see #set(String, String)
     * @see #remove(String)
     * @see #clear()
     * @see #get(String)
     * @see #getNames()
     *
     * @since 25.0
     */
    Style bind(String name, Signal<String> signal);

    /**
     * Sets the <code>background</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBackground(String value) {
        return set(STYLE_BACKGROUND, value);
    }

    /**
     * Sets the <code>background-color</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBackgroundColor(String value) {
        return set(STYLE_BACKGROUND_COLOR, value);
    }

    /**
     * Sets the <code>background-position</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBackgroundPosition(String value) {
        return set(STYLE_BACKGROUND_POSITION, value);
    }

    /**
     * Sets the <code>background-size</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBackgroundSize(String value) {
        return set(STYLE_BACKGROUND_SIZE, value);
    }

    /**
     * Sets the <code>border</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorder(String value) {
        return set(STYLE_BORDER, value);
    }

    /**
     * Sets the <code>border-left</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorderLeft(String value) {
        return set(STYLE_BORDER_LEFT, value);
    }

    /**
     * Sets the <code>border-right</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorderRight(String value) {
        return set(STYLE_BORDER_RIGHT, value);
    }

    /**
     * Sets the <code>border-top</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorderTop(String value) {
        return set(STYLE_BORDER_TOP, value);
    }

    /**
     * Sets the <code>border-bottom</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorderBottom(String value) {
        return set(STYLE_BORDER_BOTTOM, value);
    }

    /**
     * Sets the <code>border-radius</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorderRadius(String value) {
        return set(STYLE_BORDER_RADIUS, value);
    }

    public enum BoxSizing {
        CONTENT_BOX, BORDER_BOX, INITIAL, INHERIT
    }

    /**
     * Sets the <code>box-sizing</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBoxSizing(BoxSizing value) {
        return applyOrErase(STYLE_BOX_SIZING, value);
    }

    /**
     * Sets the <code>box-shadow</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBoxShadow(String value) {
        return set(STYLE_BOX_SHADOW, value);
    }

    /**
     * Css values for the clear property.
     */
    public enum Clear {
        NONE, LEFT, RIGHT, BOTH, INITIAL, INHERIT
    }

    /**
     * Sets the <code>clear</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setClear(Clear value) {
        return applyOrErase(STYLE_CLEAR, value);
    }

    /**
     * Sets the <code>cursor</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setCursor(String value) {
        return set(STYLE_CURSOR, value);
    }

    /**
     * Sets the <code>color</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setColor(String value) {
        return set(STYLE_COLOR, value);
    }

    /**
     * Sets the <code>color-scheme</code> property.
     * <p>
     * The color scheme affects how the browser renders UI elements and allows
     * the component to adapt to system color scheme preferences.
     *
     * @param value
     *            the color scheme value (if <code>null</code> or NORMAL, the
     *            property will be removed)
     * @return this style instance
     */
    default Style setColorScheme(ColorScheme.Value value) {
        if (value == null || value == ColorScheme.Value.NORMAL) {
            return remove(STYLE_COLOR_SCHEME);
        } else {
            return set(STYLE_COLOR_SCHEME, value.getValue());
        }
    }

    /**
     * Gets the <code>color-scheme</code> property.
     *
     * @return the color scheme value, or NORMAL if not set
     */
    default ColorScheme.Value getColorScheme() {
        String value = get(STYLE_COLOR_SCHEME);
        return ColorScheme.Value.fromString(value);
    }

    /**
     * Sets the <code>filter</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFilter(String value) {
        return applyOrErase(STYLE_FILTER, value);
    }

    public enum Display {
        INLINE,
        BLOCK,
        CONTENTS,
        FLEX,
        GRID,
        INLINE_BLOCK,
        INLINE_FLEX,
        INLINE_GRID,
        INLINE_TABLE,
        LIST_ITEM,
        RUN_IN,
        TABLE,
        TABLE_CAPTION,
        TABLE_COLUMN_GROUP,
        TABLE_HEADER_GROUP,
        TABLE_FOOTER_GROUP,
        TABLE_ROW_GROUP,
        TABLE_CELL,
        TABLE_COLUMN,
        TABLE_ROW,
        NONE,
        INITIAL,
        INHERIT
    }

    /**
     * Sets the <code>display</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setDisplay(Display value) {
        return applyOrErase(STYLE_DISPLAY, value);
    }

    // PostFixed with "Css" to avoid a collision with java.lang.Float
    /**
     * Css values for the float property.
     */
    public enum FloatCss {
        NONE, LEFT, RIGHT, INITIAL, INHERIT
    }

    /**
     * Sets the <code>float</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFloat(FloatCss value) {
        return applyOrErase(STYLE_FLOAT, value);
    }

    /**
     * Sets the <code>font</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFont(String value) {
        return set(STYLE_FONT, value);
    }

    /**
     * Sets the <code>gap</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setGap(String value) {
        return applyOrErase(STYLE_GAP, value);
    }

    /**
     * Sets the <code>height</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setHeight(String value) {
        return set(STYLE_HEIGHT, value);
    }

    /**
     * Sets the <code>min-height</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMinHeight(String value) {
        return set(STYLE_MIN_HEIGHT, value);
    }

    /**
     * Sets the <code>max-height</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMaxHeight(String value) {
        return set(STYLE_MAX_HEIGHT, value);
    }

    /**
     * Sets the <code>margin</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMargin(String value) {
        return set(STYLE_MARGIN, value);
    }

    /**
     * Sets the <code>margin-left</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMarginLeft(String value) {
        return set(STYLE_MARGIN_LEFT, value);
    }

    /**
     * Sets the <code>margin-right</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMarginRight(String value) {
        return set(STYLE_MARGIN_RIGHT, value);
    }

    /**
     * Sets the <code>margin-top</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMarginTop(String value) {
        return set(STYLE_MARGIN_TOP, value);
    }

    /**
     * Sets the <code>margin-bottom</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMarginBottom(String value) {
        return set(STYLE_MARGIN_BOTTOM, value);
    }

    /**
     * Sets the <code>margin-inline-start</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMarginInlineStart(String value) {
        return set(STYLE_MARGIN_INLINE_START, value);
    }

    /**
     * Sets the <code>margin-inline-end</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMarginInlineEnd(String value) {
        return set(STYLE_MARGIN_INLINE_END, value);
    }

    /**
     * Sets the <code>outline</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setOutline(String value) {
        return set(STYLE_OUTLINE, value);
    }

    /**
     * Sets the <code>opacity</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setOpacity(String value) {
        return set(STYLE_OPACITY, value);
    }

    public enum Overflow {
        VISIBLE, HIDDEN, CLIP, SCROLL, AUTO, INITIAL, INHERIT
    }

    /**
     * Sets the <code>overflow</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setOverflow(Overflow value) {
        return applyOrErase(STYLE_OVERFLOW, value);
    }

    /**
     * Sets the <code>padding</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setPadding(String value) {
        return set(STYLE_PADDING, value);
    }

    /**
     * Sets the <code>padding-left</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setPaddingLeft(String value) {
        return set(STYLE_PADDING_LEFT, value);
    }

    /**
     * Sets the <code>padding-right</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setPaddingRight(String value) {
        return set(STYLE_PADDING_RIGHT, value);
    }

    /**
     * Sets the <code>padding-top</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setPaddingTop(String value) {
        return set(STYLE_PADDING_TOP, value);
    }

    /**
     * Sets the <code>padding-bottom</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setPaddingBottom(String value) {
        return set(STYLE_PADDING_BOTTOM, value);
    }

    /**
     * Css values for the position property.
     */
    public enum Position {
        STATIC, RELATIVE, ABSOLUTE, FIXED, STICKY;
    }

    /**
     * Sets the <code>position</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setPosition(Position value) {
        return applyOrErase(STYLE_POSITION, value);
    }

    /**
     * Sets the <code>rotate</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setRotate(String value) {
        return applyOrErase(STYLE_ROTATE, value);
    }

    /**
     * Sets the <code>scale</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setScale(String value) {
        return set(STYLE_SCALE, value);
    }

    /**
     * Css values for the text-align property.
     */
    public enum TextAlign {
        LEFT, RIGHT, CENTER, JUSTIFY, INITIAL, INHERIT
    }

    /**
     * Sets the <code>text-align</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTextAlign(TextAlign value) {
        return applyOrErase(STYLE_TEXT_ALIGN, value);
    }

    /**
     * Sets the <code>text-decoration</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTextDecoration(String value) {
        return set(STYLE_TEXT_DECORATION, value);
    }

    /**
     * Sets the <code>transform</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTransform(String value) {
        return set(STYLE_TRANSFORM, value);
    }

    /**
     * Sets the <code>transform-origin</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTransformOrigin(String value) {
        return set(STYLE_TRANSFORM_ORIGIN, value);
    }

    /**
     * Sets the <code>transition</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTransition(String value) {
        return set(STYLE_TRANSITION, value);
    }

    /**
     * Css values for the visibility property.
     */
    public enum Visibility {
        VISIBLE, HIDDEN, COLLAPSE, INITIAL, INHERIT
    }

    /**
     * Sets the <code>visibility</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setVisibility(Visibility value) {
        return applyOrErase(STYLE_VISIBILITY, value);
    }

    /**
     * Sets the <code>width</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setWidth(String value) {
        return set(STYLE_WIDTH, value);
    }

    /**
     * Sets the <code>min-width</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMinWidth(String value) {
        return set(STYLE_MIN_WIDTH, value);
    }

    /**
     * Sets the <code>max-width</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setMaxWidth(String value) {
        return set(STYLE_MAX_WIDTH, value);
    }

    /**
     * Css values for the white-space property.
     */
    public enum WhiteSpace {
        NORMAL, NOWRAP, PRE, PRE_LINE, PRE_WRAP, BREAK_SPACES, INITIAL, INHERIT
    }

    /**
     * Sets the <code>white-space</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setWhiteSpace(WhiteSpace value) {
        return applyOrErase(STYLE_WHITE_SPACE, value);
    }

    /**
     * Sets the <code>left</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setLeft(String value) {
        return set(STYLE_LEFT, value);
    }

    /**
     * Sets the <code>right</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setRight(String value) {
        return set(STYLE_RIGHT, value);
    }

    /**
     * Sets the <code>top</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTop(String value) {
        return set(STYLE_TOP, value);
    }

    /**
     * Sets the <code>bottom</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBottom(String value) {
        return set(STYLE_BOTTOM, value);
    }

    /**
     * Sets the <code>z-index</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setZIndex(Integer value) {
        return applyOrErase(STYLE_Z_INDEX, value);
    }

    /**
     * Css values for the <code>font-weight</code> property.
     */
    public enum FontWeight {
        NORMAL, LIGHTER, BOLD, BOLDER, INITIAL, INHERIT
    }

    /**
     * Sets the <code>font-weight</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFontWeight(FontWeight value) {
        return applyOrErase(STYLE_FONT_WEIGHT, value);
    }

    /**
     * Sets the <code>font-weight</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFontWeight(Integer value) {
        return applyOrErase(STYLE_FONT_WEIGHT, value);
    }

    /**
     * Sets the <code>font-weight</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFontWeight(String value) {
        return set(STYLE_FONT_WEIGHT, value);
    }

    /**
     * Sets the <code>font-size</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFontSize(String value) {
        return set(STYLE_FONT_SIZE, value);
    }

    /**
     * Sets the <code>line-height</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setLineHeight(String value) {
        return set(STYLE_LINE_HEIGHT, value);
    }

    /**
     * Css values for the <code>align-items</code> property.
     */
    public enum AlignItems {
        NORMAL,
        STRETCH,
        CENTER,
        UNSAFE,
        SAFE,
        START,
        END,
        FLEX_START,
        FLEX_END,
        SELF_START,
        SELF_END,
        BASELINE,
        INITIAL;
    }

    /**
     * Sets the <code>align-items</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setAlignItems(AlignItems value) {
        return applyOrErase(STYLE_ALIGN_ITEMS, value);
    }

    /**
     * Css values for the <code>align-self</code> property.
     */
    public enum AlignSelf {
        AUTO,
        NORMAL,
        STRETCH,
        UNSAFE,
        SAFE,
        CENTER,
        START,
        END,
        FLEX_START,
        FLEX_END,
        SELF_START,
        SELF_END,
        BASELINE,
        INITIAL;
    }

    /**
     * Sets the <code>align-self</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setAlignSelf(AlignSelf value) {
        return applyOrErase(STYLE_ALIGN_SELF, value);
    }

    /**
     * Css values for the <code>flex-wrap</code> property.
     */
    public enum FlexWrap {
        NOWRAP, WRAP, WRAP_REVERSE, INITIAL
    }

    /**
     * Sets the <code>flex-wrap</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFlexWrap(FlexWrap value) {
        return applyOrErase(STYLE_FLEX_WRAP, value);
    }

    /**
     * Sets the <code>flex-grow</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFlexGrow(String value) {
        return set(STYLE_FLEX_GROW, value);
    }

    /**
     * Sets the <code>flex-shrink</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFlexShrink(String value) {
        return set(STYLE_FLEX_SHRINK, value);
    }

    /**
     * Css values for the <code>justify-content</code> property.
     */
    public enum JustifyContent {
        CENTER,
        START,
        END,
        FLEX_START,
        FLEX_END,
        LEFT,
        RIGHT,
        NORMAL,
        SPACE_BETWEEN,
        SPACE_AROUND,
        SPACE_EVENLY,
        STRETCH,
        SAFE,
        UNSAFE,
        INITIAL
    }

    /**
     * Sets the <code>justify-content</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setJustifyContent(JustifyContent value) {
        return applyOrErase(STYLE_JUSTIFY_CONTENT, value);
    }

    /**
     * Css values for the <code>justify-content</code> property.
     */
    public enum FlexDirection {
        ROW, ROW_REVERSE, COLUMN, COLUMN_REVERSE, INITIAL
    }

    /**
     * Sets the <code>flex-direction</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFlexDirection(FlexDirection value) {
        return applyOrErase(STYLE_FLEX_DIRECTION, value);
    }

    /**
     * Css values for the <code>flex-basis</code> property.
     */
    public enum FlexBasis {
        AUTO, MAX_CONTENT, MIN_CONTENT, FIT_CONTENT, CONTENT, INITIAL
    }

    /**
     * Sets the <code>flex-basis</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFlexBasis(FlexBasis value) {
        return applyOrErase(STYLE_FLEX_BASIS, value);
    }

    /**
     * Sets the <code>flex-basis</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setFlexBasis(String value) {
        return set(STYLE_FLEX_BASIS, value);
    }

    private Style applyOrErase(String propertyName, Enum value) {
        if (value == null) {
            return remove(propertyName);
        } else {
            return set(propertyName,
                    value.name().replace("_", "-").toLowerCase());
        }
    }

    private Style applyOrErase(String propertyName, Object value) {
        if (value == null) {
            return remove(propertyName);
        } else {
            return set(propertyName, value.toString());
        }
    }

}
