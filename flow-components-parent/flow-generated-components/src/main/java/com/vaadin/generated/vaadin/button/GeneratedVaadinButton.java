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
package com.vaadin.generated.vaadin.button;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.HasClickListeners;
import com.vaadin.ui.HasText;
import com.vaadin.ui.Focusable;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-button>} is a Polymer 2 element providing an accessible and
 * customizable button.
 * 
 * {@code }`html <vaadin-button> </vaadin-button> {@code }`
 * 
 * ### Styling
 * 
 * The following shadow DOM parts are exposed for styling:
 * 
 * Part name | Description ----------------|---------------- {@code button} |
 * The internal {@code <button>} element
 * 
 * The following attributes are exposed for styling:
 * 
 * Attribute | Description --------- | ----------- {@code active} | Set when the
 * button is pressed down, either with mouse, touch or the keyboard.
 * {@code disabled} | Set when the button is disabled. {@code focus-ring} | Set
 * when the button is focused using the keyboard. {@code focused} | Set when the
 * button is focused.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.16-SNAPSHOT",
		"WebComponent: Vaadin.ButtonElement#null", "Flow#0.1.16-SNAPSHOT"})
@Tag("vaadin-button")
@HtmlImport("frontend://bower_components/vaadin-button/vaadin-button.html")
public class GeneratedVaadinButton<R extends GeneratedVaadinButton<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasClickListeners<R>,
			HasText,
			Focusable<R>,
			HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutofocus() {
		return getElement().getProperty("autofocus", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Specify that this control should have input focus when the page loads.
	 * 
	 * @param autofocus
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setAutofocus(boolean autofocus) {
		getElement().setProperty("autofocus", autofocus);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the element currently has focus.
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
	 * If true, the user cannot interact with this element.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isDisabled() {
		return getElement().getProperty("disabled", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If true, the user cannot interact with this element.
	 * 
	 * @param disabled
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setDisabled(boolean disabled) {
		getElement().setProperty("disabled", disabled);
		return get();
	}

	public void connectedCallback() {
		getElement().callFunction("connectedCallback");
	}

	public void disconnectedCallback() {
		getElement().callFunction("disconnectedCallback");
	}

	/**
	 * Sets the given string as the content of this component.
	 * 
	 * @param the
	 *            text content to set
	 * @see HasText#setText(String)
	 */
	public GeneratedVaadinButton(java.lang.String text) {
		setText(text);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedVaadinButton() {
	}
}