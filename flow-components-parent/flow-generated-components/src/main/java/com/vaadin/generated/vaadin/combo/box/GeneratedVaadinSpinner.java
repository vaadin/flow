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
import com.vaadin.generated.vaadin.combo.box.GeneratedVaadinSpinner;

@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.14-SNAPSHOT",
		"WebComponent: vaadin-spinner#2.0.0-beta2", "Flow#0.1.14-SNAPSHOT"})
@Tag("vaadin-spinner")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-spinner.html")
public class GeneratedVaadinSpinner extends Component implements HasStyle {

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isActive() {
		return getElement().getProperty("active", false);
	}

	/**
	 * @param active
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public <R extends GeneratedVaadinSpinner> R setActive(boolean active) {
		getElement().setProperty("active", active);
		return getSelf();
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends GeneratedVaadinSpinner> R getSelf() {
		return (R) this;
	}
}