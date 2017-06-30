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
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: PasswordFieldElement#null", "Flow#0.1.12-SNAPSHOT"})
@Tag("vaadin-password-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-password-field.html")
public class PasswordFieldElement<R extends PasswordFieldElement<R>>
		extends
			Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to hide the eye icon which toggles the password visibility.
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
	 * @return This instance, for method chaining.
	 */
	public R setHideToggleButton(boolean hideToggleButton) {
		getElement().setProperty("hideToggleButton", hideToggleButton);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the password is visible ([type=text]).
	 */
	public boolean isPasswordVisible() {
		return getElement().getProperty("passwordVisible", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the password is visible ([type=text]).
	 * 
	 * @param passwordVisible
	 * @return This instance, for method chaining.
	 */
	public R setPasswordVisible(boolean passwordVisible) {
		getElement().setProperty("passwordVisible", passwordVisible);
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