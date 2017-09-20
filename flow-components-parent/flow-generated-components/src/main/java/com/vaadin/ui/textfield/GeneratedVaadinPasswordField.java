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
package com.vaadin.ui.textfield;

import javax.annotation.Generated;
import com.vaadin.ui.event.Tag;
import com.vaadin.ui.common.HtmlImport;

@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: PasswordFieldElement#1.1.0-alpha4", "Flow#0.1-SNAPSHOT"})
@Tag("vaadin-password-field")
@HtmlImport("frontend://bower_components/vaadin-text-field/vaadin-password-field.html")
public class GeneratedVaadinPasswordField<R extends GeneratedVaadinPasswordField<R>>
		extends
			GeneratedVaadinTextField<R> {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to hide the eye icon which toggles the password visibility.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isRevealButtonHidden() {
		return getElement().getProperty("revealButtonHidden", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set to true to hide the eye icon which toggles the password visibility.
	 * </p>
	 * 
	 * @param revealButtonHidden
	 *            the boolean value to set
	 */
	public void setRevealButtonHidden(boolean revealButtonHidden) {
		getElement().setProperty("revealButtonHidden", revealButtonHidden);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the password is visible ([type=text]).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isPasswordVisible() {
		return getElement().getProperty("passwordVisible", false);
	}
}