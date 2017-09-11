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
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;

@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: vaadin-combo-box-item#2.0.0", "Flow#0.1-SNAPSHOT"})
@Tag("vaadin-combo-box-item")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-item.html")
public class GeneratedVaadinComboBoxItem<R extends GeneratedVaadinComboBoxItem<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The index of the item
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public double getIndex() {
		return getElement().getProperty("index", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The index of the item
	 * </p>
	 * 
	 * @param index
	 *            the double value to set
	 */
	public void setIndex(double index) {
		getElement().setProperty("index", index);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The item to render
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public String getItemString() {
		return getElement().getProperty("item");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The item to render
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	protected JsonObject protectedGetItemObject() {
		return (JsonObject) getElement().getPropertyRaw("item");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The item to render
	 * </p>
	 * 
	 * @param item
	 *            the String value to set
	 */
	public void setItem(java.lang.String item) {
		getElement().setProperty("item", item == null ? "" : item);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The item to render
	 * </p>
	 * 
	 * @param item
	 *            the JsonObject value to set
	 */
	protected void setItem(elemental.json.JsonObject item) {
		getElement().setPropertyJson("item", item);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The text label corresponding to the item
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The text label corresponding to the item
	 * </p>
	 * 
	 * @param label
	 *            the String value to set
	 */
	public void setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True when item is selected
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isSelected() {
		return getElement().getProperty("selected", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True when item is selected
	 * </p>
	 * 
	 * @param selected
	 *            the boolean value to set
	 */
	public void setSelected(boolean selected) {
		getElement().setProperty("selected", selected);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True when item is focused
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True when item is focused
	 * </p>
	 * 
	 * @param focused
	 *            the boolean value to set
	 */
	public void setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
	}
}