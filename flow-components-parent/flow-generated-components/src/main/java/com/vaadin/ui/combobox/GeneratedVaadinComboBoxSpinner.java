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

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * The loading spinner element of combo box
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
 * <td>{@code active}</td>
 * <td>Set when the spinner is active</td>
 * <td>:host</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.ComboBoxSpinnerElement#3.0.0-alpha7",
		"Flow#1.0-SNAPSHOT"})
@Tag("vaadin-combo-box-spinner")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-spinner.html")
public class GeneratedVaadinComboBoxSpinner<R extends GeneratedVaadinComboBoxSpinner<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * 
	 * @return the {@code active} property from the webcomponent
	 */
	public boolean isActive() {
		return getElement().getProperty("active", false);
	}

	/**
	 * @param active
	 *            the boolean value to set
	 */
	public void setActive(boolean active) {
		getElement().setProperty("active", active);
	}
}