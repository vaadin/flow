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
package com.vaadin.generated.paper.spinner;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design: [Progress &
 * activity](https://www.google.com/design/spec/components
 * /progress-activity.html)
 * 
 * Element providing a single color material design circular spinner.
 * 
 * <paper-spinner-lite active></paper-spinner-lite>
 * 
 * The default spinner is blue. It can be customized to be a different color.
 * 
 * ### Accessibility
 * 
 * Alt attribute should be set to provide adequate context for accessibility. If
 * not provided, it defaults to 'loading'. Empty alt can be provided to mark the
 * element as decorative if alternative content is provided in another form
 * (e.g. a text block following the spinner).
 * 
 * <paper-spinner-lite alt="Loading contacts list" active></paper-spinner-lite>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-spinner-color} |
 * Color of the spinner | {@code --google-blue-500}
 * {@code --paper-spinner-stroke-width} | The width of the spinner stroke | 3px
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.17-SNAPSHOT",
		"WebComponent: paper-spinner-lite#2.0.0", "Flow#0.1.17-SNAPSHOT"})
@Tag("paper-spinner-lite")
@HtmlImport("frontend://bower_components/paper-spinner/paper-spinner-lite.html")
public class GeneratedPaperSpinnerLite<R extends GeneratedPaperSpinnerLite<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Displays the spinner.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isActive() {
		return getElement().getProperty("active", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Displays the spinner.
	 * 
	 * @param active
	 *            the boolean value to set
	 * @return this instance, for method chaining
	 */
	public R setActive(boolean active) {
		getElement().setProperty("active", active);
		return get();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Alternative text content for accessibility support. If alt is present, it
	 * will add an aria-label whose content matches alt when active. If alt is
	 * not present, it will default to 'loading' as the alt value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAlt() {
		return getElement().getProperty("alt");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Alternative text content for accessibility support. If alt is present, it
	 * will add an aria-label whose content matches alt when active. If alt is
	 * not present, it will default to 'loading' as the alt value.
	 * 
	 * @param alt
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public R setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
		return get();
	}
}