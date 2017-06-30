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
package com.vaadin.components.vaadin.combo.box;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;

@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: vaadin-combo-box-item#null", "Flow#0.1.12-SNAPSHOT"})
@Tag("vaadin-combo-box-item")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-item.html")
public class VaadinComboBoxItem<R extends VaadinComboBoxItem<R>>
		extends
			Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The index of the item
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
	 * @return This instance, for method chaining.
	 */
	public R setIndex(double index) {
		getElement().setProperty("index", index);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
	 */
	public String getItemString() {
		return getElement().getProperty("item");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
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
	 * @return This instance, for method chaining.
	 */
	public R setItem(java.lang.String item) {
		getElement().setProperty("item", item == null ? "" : item);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The item to render
	 * 
	 * @param item
	 * @return This instance, for method chaining.
	 */
	public R setItem(elemental.json.JsonObject item) {
		getElement().setPropertyJson("item", item);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The text label corresponding to the item
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
	 * @return This instance, for method chaining.
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when item is selected
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
	 * @return This instance, for method chaining.
	 */
	public R setSelected(boolean selected) {
		getElement().setProperty("selected", selected);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True when item is focused
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
	 * @return This instance, for method chaining.
	 */
	public R setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
		return getSelf();
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected R getSelf() {
		return (R) this;
	}
}