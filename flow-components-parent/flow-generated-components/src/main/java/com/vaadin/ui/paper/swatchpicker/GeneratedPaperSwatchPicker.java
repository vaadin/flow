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
package com.vaadin.ui.paper.swatchpicker;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import elemental.json.JsonArray;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * This is a simple color picker element that will allow you to choose one of
 * the Material Design colors from a list of available swatches.
 * </p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-swatch-picker&gt;&lt;/paper-swatch-picker&gt;
 * 
 * &lt;paper-swatch-picker color=&quot;{{selectedColor}}&quot;&gt;&lt;/paper-swatch-picker&gt;
 * </code>
 * </pre>
 * <p>
 * You can configure the color palette being used using the {@code colorList}
 * array and the {@code columnCount} property, which specifies how many
 * &quot;generic&quot; colours (i.e. columns in the picker) you want to display.
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-swatch-picker column-count=5 color-list='[&quot;#65a5f2&quot;, &quot;#83be54&quot;, &quot;#f0d551&quot;, &quot;#e5943c&quot;, &quot;#a96ddb&quot;]'&gt;&lt;/paper-swatch-picker&gt;
 * </code>
 * </pre>
 * 
 * <h3>Styling</h3>
 * <p>
 * The following custom properties and mixins are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --paper-swatch-picker-color-size}</td>
 * <td>The size of each of the color boxes</td>
 * <td>{@code 20px}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-swatch-picker-icon-size}</td>
 * <td>The size of the color picker icon</td>
 * <td>{@code 24px}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-swatch-picker-icon}</td>
 * <td>Mixin applied to the color picker icon</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: paper-swatch-picker#2.0.1", "Flow#1.0-SNAPSHOT" })
@Tag("paper-swatch-picker")
@HtmlImport("frontend://bower_components/paper-swatch-picker/paper-swatch-picker.html")
public class GeneratedPaperSwatchPicker<R extends GeneratedPaperSwatchPicker<R>>
        extends Component implements ComponentSupplier<R>, HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The selected color, as hex (i.e. #ffffff). value.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'color-changed' event happens.
     * </p>
     * 
     * @return the {@code color} property from the webcomponent
     */
    @Synchronize(property = "color", value = "color-changed")
    public String getColor() {
        return getElement().getProperty("color");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The selected color, as hex (i.e. #ffffff). value.
     * </p>
     * 
     * @param color
     *            the String value to set
     */
    public void setColor(java.lang.String color) {
        getElement().setProperty("color", color == null ? "" : color);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The colors to be displayed. By default, these are the Material Design
     * colors. This array is arranged by &quot;generic color&quot;, so for
     * example, all the reds (from light to dark), then the pinks, then the
     * blues, etc. Depending on how many of these generic colors you have, you
     * should update the {@code columnCount} property.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code colorList} property from the webcomponent
     */
    protected JsonArray protectedGetColorList() {
        return (JsonArray) getElement().getPropertyRaw("colorList");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The colors to be displayed. By default, these are the Material Design
     * colors. This array is arranged by &quot;generic color&quot;, so for
     * example, all the reds (from light to dark), then the pinks, then the
     * blues, etc. Depending on how many of these generic colors you have, you
     * should update the {@code columnCount} property.
     * </p>
     * 
     * @param colorList
     *            the JsonArray value to set
     */
    protected void setColorList(elemental.json.JsonArray colorList) {
        getElement().setPropertyJson("colorList", colorList);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number of columns to display in the picker. This corresponds to the
     * number of generic colors (i.e. not counting the light/dark) variants of a
     * specific color) you are using in your {@code colorList}. For example, the
     * Material Design palette has 18 colors
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code columnCount} property from the webcomponent
     */
    public double getColumnCount() {
        return getElement().getProperty("columnCount", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The number of columns to display in the picker. This corresponds to the
     * number of generic colors (i.e. not counting the light/dark) variants of a
     * specific color) you are using in your {@code colorList}. For example, the
     * Material Design palette has 18 colors
     * </p>
     * 
     * @param columnCount
     *            the double value to set
     */
    public void setColumnCount(double columnCount) {
        getElement().setProperty("columnCount", columnCount);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown horizontally
     * relative to the dropdown trigger.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code horizontalAlign} property from the webcomponent
     */
    public String getHorizontalAlign() {
        return getElement().getProperty("horizontalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown horizontally
     * relative to the dropdown trigger.
     * </p>
     * 
     * @param horizontalAlign
     *            the String value to set
     */
    public void setHorizontalAlign(java.lang.String horizontalAlign) {
        getElement().setProperty("horizontalAlign",
                horizontalAlign == null ? "" : horizontalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown vertically
     * relative to the dropdown trigger.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code verticalAlign} property from the webcomponent
     */
    public String getVerticalAlign() {
        return getElement().getProperty("verticalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The orientation against which to align the menu dropdown vertically
     * relative to the dropdown trigger.
     * </p>
     * 
     * @param verticalAlign
     *            the String value to set
     */
    public void setVerticalAlign(java.lang.String verticalAlign) {
        getElement().setProperty("verticalAlign",
                verticalAlign == null ? "" : verticalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the color picker button will not produce a ripple effect when
     * interacted with via the pointer.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noink} property from the webcomponent
     */
    public boolean isNoink() {
        return getElement().getProperty("noink", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * If true, the color picker button will not produce a ripple effect when
     * interacted with via the pointer.
     * </p>
     * 
     * @param noink
     *            the boolean value to set
     */
    public void setNoink(boolean noink) {
        getElement().setProperty("noink", noink);
    }

    @DomEvent("color-picker-selected")
    public static class ColorPickerSelectedEvent<R extends GeneratedPaperSwatchPicker<R>>
            extends ComponentEvent<R> {
        public ColorPickerSelectedEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addColorPickerSelectedListener(
            ComponentEventListener<ColorPickerSelectedEvent<R>> listener) {
        return addListener(ColorPickerSelectedEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("color-changed")
    public static class ColorChangeEvent<R extends GeneratedPaperSwatchPicker<R>>
            extends ComponentEvent<R> {
        public ColorChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public Registration addColorChangeListener(
            ComponentEventListener<ColorChangeEvent<R>> listener) {
        return addListener(ColorChangeEvent.class,
                (ComponentEventListener) listener);
    }
}