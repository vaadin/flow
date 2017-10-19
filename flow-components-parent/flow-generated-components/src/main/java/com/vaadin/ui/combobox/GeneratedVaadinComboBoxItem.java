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
package com.vaadin.ui.combobox;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * The default element used for items in the vaadin-combobox.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following state attributes are exposed for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Attribute</th>
 * <th>Description</th>
 * <th>Part name</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code selected}</td>
 * <td>Set when the item is selected</td>
 * <td>:host</td>
 * </tr>
 * <tr>
 * <td>{@code focused}</td>
 * <td>Set when the item is focused</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.ComboBoxItemElement#3.0.0", "Flow#1.0-SNAPSHOT"})
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
	 * 
	 * @return the {@code index} property from the webcomponent
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
	 * 
	 * @return the {@code item} property from the webcomponent
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
	 * 
	 * @return the {@code item} property from the webcomponent
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
	 * 
	 * @return the {@code label} property from the webcomponent
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
	 * 
	 * @return the {@code selected} property from the webcomponent
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
	 * 
	 * @return the {@code focused} property from the webcomponent
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