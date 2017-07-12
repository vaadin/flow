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
package com.vaadin.components.paper.swatch.picker;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.components.paper.swatch.picker.PaperSwatchPicker;
import elemental.json.JsonArray;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * This is a simple color picker element that will allow you to choose one of
 * the Material Design colors from a list of available swatches.
 * 
 * Example:
 * 
 * <paper-swatch-picker></paper-swatch-picker>
 * 
 * <paper-swatch-picker color="{{selectedColor}}"></paper-swatch-picker>
 * 
 * You can configure the color palette being used using the {@code colorList}
 * array and the {@code columnCount} property, which specifies how many
 * "generic" colours (i.e. columns in the picker) you want to display.
 * 
 * <paper-swatch-picker column-count=5 color-list='["#65a5f2", "#83be54",
 * "#f0d551", "#e5943c", "#a96ddb"]'></paper-swatch-picker>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-swatch-picker-color-size} | The size of each of the color
 * boxes | {@code 20px} {@code --paper-swatch-picker-icon-size} | The size of
 * the color picker icon | {@code 24px} {@code --paper-swatch-picker-icon} |
 * Mixin applied to the color picker icon | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-swatch-picker#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-swatch-picker")
@HtmlImport("frontend://bower_components/paper-swatch-picker/paper-swatch-picker.html")
public class PaperSwatchPicker extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The selected color, as hex (i.e. #ffffff). value.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * 'color-changed' event happens.
	 */
	@Synchronize(property = "color", value = "color-changed")
	public String getColor() {
		return getElement().getProperty("color");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The selected color, as hex (i.e. #ffffff). value.
	 * 
	 * @param color
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperSwatchPicker> R setColor(java.lang.String color) {
		getElement().setProperty("color", color == null ? "" : color);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The colors to be displayed. By default, these are the Material Design
	 * colors. This array is arranged by "generic color", so for example, all
	 * the reds (from light to dark), then the pinks, then the blues, etc.
	 * Depending on how many of these generic colors you have, you should update
	 * the {@code columnCount} property.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonArray getColorList() {
		return (JsonArray) getElement().getPropertyRaw("colorList");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The colors to be displayed. By default, these are the Material Design
	 * colors. This array is arranged by "generic color", so for example, all
	 * the reds (from light to dark), then the pinks, then the blues, etc.
	 * Depending on how many of these generic colors you have, you should update
	 * the {@code columnCount} property.
	 * 
	 * @param colorList
	 *            the JsonArray value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperSwatchPicker> R setColorList(
			elemental.json.JsonArray colorList) {
		getElement().setPropertyJson("colorList", colorList);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number of columns to display in the picker. This corresponds to the
	 * number of generic colors (i.e. not counting the light/dark) variants of a
	 * specific color) you are using in your {@code colorList}. For example, the
	 * Material Design palette has 18 colors
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getColumnCount() {
		return getElement().getProperty("columnCount", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The number of columns to display in the picker. This corresponds to the
	 * number of generic colors (i.e. not counting the light/dark) variants of a
	 * specific color) you are using in your {@code colorList}. For example, the
	 * Material Design palette has 18 colors
	 * 
	 * @param columnCount
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperSwatchPicker> R setColumnCount(double columnCount) {
		getElement().setProperty("columnCount", columnCount);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown horizontally
	 * relative to the dropdown trigger.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getHorizontalAlign() {
		return getElement().getProperty("horizontalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown horizontally
	 * relative to the dropdown trigger.
	 * 
	 * @param horizontalAlign
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperSwatchPicker> R setHorizontalAlign(
			java.lang.String horizontalAlign) {
		getElement().setProperty("horizontalAlign",
				horizontalAlign == null ? "" : horizontalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown vertically
	 * relative to the dropdown trigger.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getVerticalAlign() {
		return getElement().getProperty("verticalAlign");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The orientation against which to align the menu dropdown vertically
	 * relative to the dropdown trigger.
	 * 
	 * @param verticalAlign
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperSwatchPicker> R setVerticalAlign(
			java.lang.String verticalAlign) {
		getElement().setProperty("verticalAlign",
				verticalAlign == null ? "" : verticalAlign);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the color picker button will not produce a ripple effect when
	 * interacted with via the pointer.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoink() {
		return getElement().getProperty("noink", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the color picker button will not produce a ripple effect when
	 * interacted with via the pointer.
	 * 
	 * @param noink
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends PaperSwatchPicker> R setNoink(boolean noink) {
		getElement().setProperty("noink", noink);
		return getSelf();
	}

	@DomEvent("color-picker-selected")
	public static class ColorPickerSelectedEvent
			extends
				ComponentEvent<PaperSwatchPicker> {
		public ColorPickerSelectedEvent(PaperSwatchPicker source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addColorPickerSelectedListener(
			ComponentEventListener<ColorPickerSelectedEvent> listener) {
		return addListener(ColorPickerSelectedEvent.class, listener);
	}

	@DomEvent("color-changed")
	public static class ColorChangedEvent
			extends
				ComponentEvent<PaperSwatchPicker> {
		public ColorChangedEvent(PaperSwatchPicker source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addColorChangedListener(
			ComponentEventListener<ColorChangedEvent> listener) {
		return addListener(ColorChangedEvent.class, listener);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends PaperSwatchPicker> R getSelf() {
		return (R) this;
	}
}