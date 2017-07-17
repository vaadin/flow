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
package com.vaadin.generated.vaadin.combo.box;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.generated.vaadin.combo.box.GeneratedVaadinComboBoxItem;
import elemental.json.JsonObject;

@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: vaadin-combo-box-item#2.0.0-beta2",
		"Flow#0.1.14-SNAPSHOT"})
@Tag("vaadin-combo-box-item")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-item.html")
public class GeneratedVaadinComboBoxItem extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The index of the item
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getIndex() {
		return getElement().getProperty("index", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The index of the item
	 * 
	 * @param index
	 *            the double value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinComboBoxItem> R setIndex(double index) {
		getElement().setProperty("index", index);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getItemString() {
		return getElement().getProperty("item");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getItemObject() {
		return (JsonObject) getElement().getPropertyRaw("item");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
	 * 
	 * @param item
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinComboBoxItem> R setItem(
			java.lang.String item) {
		getElement().setProperty("item", item == null ? "" : item);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
	 * 
	 * @param item
	 *            the JsonObject value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinComboBoxItem> R setItem(
			elemental.json.JsonObject item) {
		getElement().setPropertyJson("item", item);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The text label corresponding to the item
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The text label corresponding to the item
	 * 
	 * @param label
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinComboBoxItem> R setLabel(
			java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when item is selected
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isSelected() {
		return getElement().getProperty("selected", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when item is selected
	 * 
	 * @param selected
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinComboBoxItem> R setSelected(
			boolean selected) {
		getElement().setProperty("selected", selected);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when item is focused
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when item is focused
	 * 
	 * @param focused
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinComboBoxItem> R setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
		return getSelf();
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends GeneratedVaadinComboBoxItem> R getSelf() {
		return (R) this;
	}
}