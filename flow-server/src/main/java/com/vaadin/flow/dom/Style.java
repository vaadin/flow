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
     * @param value
     *            the style property value (if <code>null</code>, the property
     *            will be removed)
     * @return this style instance
     */
    default Style setBackground(String value) {
        return set("background", value);
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
        return set("background-color", value);
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
        return set("border", value);
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
        return set("border-left", value);
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
        return set("border-right", value);
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
        return set("border-top", value);
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
        return set("border-bottom", value);
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
        return applyOrErase("box-sizing", value);
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
        return set("box-shadow", value);
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
        return applyOrErase("clear", value);
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
        return set("cursor", value);
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
        return set("color", value);
    }

    public enum Display {
        INLINE, BLOCK, CONTENTS, FLEX, GRID, INLINE_BLOCK, INLINE_FLEX, INLINE_GRID, INLINE_TABLE, LIST_ITEM, RUN_IN, TABLE, TABLE_CAPTION, TABLE_COLUMN_GROUP, TABLE_HEADER_GROUP, TABLE_FOOTER_GROUP, TABLE_ROW_GROUP, TABLE_CELL, TABLE_COLUMN, TABLE_ROW, NONE, INITIAL, INHERIT
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
        return applyOrErase("display", value);
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
        return applyOrErase("float", value);
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
        return set("font", value);
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
        return set("height", value);
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
        return set("min-height", value);
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
        return set("max-height", value);
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
        return set("margin", value);
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
        return set("margin-left", value);
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
        return set("margin-right", value);
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
        return set("margin-top", value);
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
        return set("margin-bottom", value);
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
        return set("margin-inline-start", value);
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
        return set("margin-inline-end", value);
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
        return set("outline", value);
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
        return set("opacity", value);
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
        return applyOrErase("overflow", value);
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
        return set("padding", value);
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
        return set("padding-left", value);
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
        return set("padding-right", value);
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
        return set("padding-top", value);
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
        return set("padding-bottom", value);
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
        return applyOrErase("position", value);
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
        return set("scale", value);
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
        return applyOrErase("text-align", value);
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
        return set("text-decoration", value);
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
        return set("transform", value);
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
        return set("transition", value);
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
        return applyOrErase("visibility", value);
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
        return set("width", value);
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
        return set("min-width", value);
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
        return set("max-width", value);
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
        return applyOrErase("white-space", value);
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
        return set("left", value);
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
        return set("right", value);
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
        return set("top", value);
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
        return set("bottom", value);
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
        return applyOrErase("z-index", value);
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
        return applyOrErase("font-weight", value);
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
        return applyOrErase("font-weight", value);
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
        return set("font-size", value);
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
        return set("line-height", value);
    }

    /**
     * Css values for the <code>align-items</code> property.
     */
    public enum AlignItems {
        NORMAL, STRETCH, CENTER, UNSAFE, SAFE, START, END, FLEX_START, FLEX_END, SELF_START, SELF_END, BASELINE, INITIAL;
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
        return applyOrErase("align-items", value);
    }

    /**
     * Css values for the <code>align-self</code> property.
     */
    public enum AlignSelf {
        AUTO, NORMAL, STRETCH, UNSAFE, SAFE, CENTER, START, END, FLEX_START, FLEX_END, SELF_START, SELF_END, BASELINE, INITIAL;
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
        return applyOrErase("align-self", value);
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
        return applyOrErase("flex-wrap", value);
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
        return set("flex-grow", value);
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
        return set("flex-shrink", value);
    }

    /**
     * Css values for the <code>justify-content</code> property.
     */
    public enum JustifyContent {
        CENTER, START, END, FLEX_START, FLEX_END, LEFT, RIGHT, NORMAL, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY, STRETCH, SAFE, UNSAFE, INITIAL
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
        return applyOrErase("justify-content", value);
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
        return applyOrErase("flex-direction", value);
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
        return applyOrErase("flex-basis", value);
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
        return set("flex-basis", value);
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
