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
package com.vaadin.components.vaadin.text.field;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.vaadin.text.field.VaadinPasswordField;

@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: PasswordFieldElement#null", "Flow#0.1.13-SNAPSHOT"})
@Tag("vaadin-password-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-password-field.html")
public class VaadinPasswordField extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to hide the eye icon which toggles the password visibility.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isHideToggleButton() {
		return getElement().getProperty("hideToggleButton", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to hide the eye icon which toggles the password visibility.
	 * 
	 * @param hideToggleButton
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends VaadinPasswordField> R setHideToggleButton(
			boolean hideToggleButton) {
		getElement().setProperty("hideToggleButton", hideToggleButton);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the password is visible ([type=text]).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPasswordVisible() {
		return getElement().getProperty("passwordVisible", false);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends VaadinPasswordField> R getSelf() {
		return (R) this;
	}
}