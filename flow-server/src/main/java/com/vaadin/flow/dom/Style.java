/*
 * Copyright 2000-2023 Vaadin Ltd.
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
     * Note that the name should be in camelCase and not dash-separated, i.e.
     * use "fontFamily" and not "font-family"
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     * @return the style property value, or <code>null</code> if the style
     *         property has not been set
     */
    String get(String name);

    /**
     * Sets the given style property to the given value.
     * <p>
     * Both camelCased (e.g. <code>fontFamily</code>) and dash-separated (e.g.
     * <code>font-family</code> versions are supported.
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    Style set(String name, String value);

    /**
     * Removes the given style property if it has been set.
     * <p>
     * Both camelCased (e.g. <code>fontFamily</code>) and dash-separated (e.g.
     * <code>font-family</code> versions are supported.
     *
     * @param name
     *            the style property name as camelCase, not <code>null</code>
     * @return this style instance
     */
    Style remove(String name);

    /**
     * Removes all set style properties.
     *
     * @return this style instance
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
     *
     * @return a stream of defined style property names
     */
    Stream<String> getNames();

    /**
     * Sets the <code>background</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-background
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBackground(String value) {
        set("background", value);
        return this;
    }

    /**
     * Sets the <code>border</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-border
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBorder(String value) {
        set("border", value);
        return this;
    }

    /**
     * Sets the <code>box-sizing</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-box-sizing
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBoxSizing(String value) {
        set("box-sizing", value);
        return this;
    }

    /**
     * Sets the <code>box-shadow</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-box-shadow
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBoxShadow(String value) {
        set("box-shadow", value);
        return this;
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
        applyOrErase("clear", value);
        return this;
    }

    /**
     * Sets the <code>cursor</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-cursor
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setCursor(String value) {
        set("cursor", value);
        return this;
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
        set("color", value);
        return this;
    }

    public enum Display {
        INLINE, BLOCK, CONTENTS, FLEX, GRID, INLINE_BLOCK, INLINE_FLEX, INLINE_GRID, INLINE_TABLE, LIST_ITEM, RUN_IN, TABLE, TABLE_CAPTION, TABLE_COLUMN_GROUP, TABLE_HEADER_GROUP, TABLE_FOOTER_GROUP, TABLE_ROW_GROUP, TABLE_CELL, TABLE_COLUMN, TABLE_ROW, NONE, INITIAL, INHERIT
    }

    /**
     * Sets the <code>display</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-display
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setDisplay(Display value) {
        applyOrErase("display", value);
        return this;
    }

    // PostFixed with "Css" to avoid collician with java.lang.Float
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
        applyOrErase("float", value);
        return this;
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
        set("font", value);
        return this;
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
        set("height", value);
        return this;
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
        set("margin", value);
        return this;
    }

    /**
     * Sets the <code>outline</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-outline
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setOutline(String value) {
        set("outline", value);
        return this;
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
        set("opacity", value);
        return this;
    }

    /**
     * Sets the <code>overflow</code> property.
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setOverflow(String value) {
        set("overflow", value);
        return this;
    }

    /**
     * Sets the <code>padding</code> property. * @param value the style property
     * value (if <code>null</code>, the property will be removed)
     *
     * @return this style instance
     */
    default Style setPadding(String value) {
        set("padding", value);
        return this;
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
        applyOrErase("position", value);
        return this;
    }

    /**
     * Sets the <code>scale</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-scale
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setScale(String value) {
        set("scale", value);
        return this;
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
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-text-align
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTextAlign(TextAlign value) {
        applyOrErase("text-align", value);
        return this;
    }

    /**
     * Sets the <code>text-decoration</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-text-decoration
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTextDecoration(String value) {
        set("text-decoration", value);
        return this;
    }

    /**
     * Sets the <code>transform</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-transform
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTransform(String value) {
        set("transform", value);
        return this;
    }

    /**
     * Sets the <code>transition</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-transition
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setTransition(String value) {
        set("transition", value);
        return this;
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
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-visibility
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setVisibility(Visibility value) {
        applyOrErase("visibility", value);
        return this;
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
        set("width", value);
        return this;
    }

    /**
     * Css values for the white-space property.
     */
    public enum WhiteSpace {
        NORMAL, NOWRAP, PRE, PRE_LINE, PRE_WRAP, INITIAL, INHERIT
    }

    /**
     * Sets the <code>white-space</code> property.
     *
     * @see https://drafts.csswg.org/css-backgrounds-3/#propdef-white-space
     *
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setWhiteSpace(WhiteSpace value) {
        applyOrErase("white-space", value);
        return this;
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
        set("left", value);
        return this;
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
        set("right", value);
        return this;
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
        set("top", value);
        return this;
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
        set("bottom", value);
        return this;
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
        applyOrErase("z-index", value);
        return this;
    }

    private void applyOrErase(String propertyName, Enum value) {
        if (value == null) {
            remove(propertyName);
        } else {
            set(propertyName, value.name().replace("_", "-").toLowerCase());
        }
    }

    private void applyOrErase(String propertyName, Object value) {
        if (value == null) {
            remove(propertyName);
        } else {
            set(propertyName, value.toString());
        }
    }

}
