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
package com.vaadin.ui.formlayout;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-form-layout>} is a Polymer 2 element providing configurable
 * responsive layout for form elements.
 * </p>
 * <p>
 * {@code }`html <vaadin-form-layout>
 * </p>
 * <vaadin-form-item> <label slot="label">First Name</label> <input
 * class="full-width" value="Jane"> </vaadin-form-item> <vaadin-form-item>
 * <label slot="label">Last Name</label> <input class="full-width" value="Doe">
 * </vaadin-form-item> <vaadin-form-item> <label slot="label">Email</label>
 * <input class="full-width" value="jane.doe@example.com"> </vaadin-form-item>
 * </vaadin-form-layout> {@code }`
 * <p>
 * It supports any child elements as layout items.
 * </p>
 * <p>
 * By default, it makes a layout of two columns if the element width is equal or
 * wider than 40em, and a single column layout otherwise.
 * </p>
 * <p>
 * The number of columns and the responsive behavior are customizable with the
 * {@code responsiveSteps} property.
 * </p>
 * <h3>Spanning Items on Multiple Columns</h3>
 * <p>
 * You can use {@code colspan} attribute on the items. In the example below, the
 * first text field spans on two columns:
 * </p>
 * <p>
 * {@code }`html <vaadin-form-layout>
 * </p>
 * <vaadin-form-item colspan="2"> <label slot="label">Address</label> <input
 * class="full-width"> </vaadin-form-item> <vaadin-form-item> <label
 * slot="label">First Name</label> <input class="full-width" value="Jane">
 * </vaadin-form-item> <vaadin-form-item> <label slot="label">Last Name</label>
 * <input class="full-width" value="Doe"> </vaadin-form-item>
 * </vaadin-form-layout> {@code }` <h3>Explicit New Row</h3>
 * <p>
 * Use the {@code <br>} line break element to wrap the items on a new row:
 * </p>
 * <p>
 * {@code }`html <vaadin-form-layout>
 * </p>
 * <vaadin-form-item> <label slot="label">Email</label> <input
 * class="full-width"> </vaadin-form-item> <br>
 * <vaadin-form-item> <label slot="label">Confirm Email</label> <input
 * class="full-width"> </vaadin-form-item> </vaadin-form-layout> {@code }` <h3>
 * CSS Properties Reference</h3>
 * <p>
 * The following custom CSS properties are available on the
 * {@code <vaadin-form-layout>} element:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom CSS property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --vaadin-form-layout-column-gap}</td>
 * <td>Length of the gap between columns</td>
 * <td>{@code 2em}</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.FormLayoutElement#1.0.3", "Flow#1.0-SNAPSHOT"})
@Tag("vaadin-form-layout")
@HtmlImport("frontend://bower_components/vaadin-form-layout/vaadin-form-layout.html")
public class GeneratedVaadinFormLayout<R extends GeneratedVaadinFormLayout<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasComponents {

/**
	 * <p>Description copied from corresponding location in WebComponent:</p>
	<p>Allows specifying a responsive behavior with the number of columns
	and the label position depending on the layout width.</p>
	<p>Format: array of objects, each object defines one responsive step
	with {@code minWidth} CSS length, {@code columns} number, and optional
	{@code labelsPosition} string of {@code &quot;aside&quot;} or {@code &quot;top&quot;}. At least one item is required.</p>
	<h4>Examples</h4>
	<dl>
	  <dt>{@code [{columns: 1}]}</dt>
	  <dd>
	    <p>The layout is always a single column, labels aside.
	  </dd>
	  <dt><pre><code>[
	  {minWidth: 0, columns: 1},
	  {minWidth: '40em', columns: 2}
	]</code></pre></dt>
	  <dd>
	    <p>Sets two responsive steps:
	    <ol>
	      <li>When the layout width is < 40em, one column, labels aside.
	      <li>Width >= 40em, two columns, labels aside.
	    </ol>
	  </dd>
	  <dt><pre><code>[
	  {minWidth: 0, columns: 1, labelsPosition: 'top'},
	  {minWidth: '20em', columns: 1},
	  {minWidth: '40em', columns: 2}
	]</code></pre></dt>
	  <dd>
	    <p>Default value. Three responsive steps:
	    <ol>
	      <li>Width < 20em, one column, labels on top.
	      <li>20em <= width < 40em, one column, labels aside.
	      <li>Width >= 40em, two columns, labels aside.
	    </ol>
	  </dd>
	</dl><p>This property is not synchronized automatically from the client side, so the returned value may not be the same as in client side.

	 * @return the {@code responsiveSteps} property from the webcomponent
	 */
	protected JsonObject protectedGetResponsiveSteps() {
		return (JsonObject) getElement().getPropertyRaw("responsiveSteps");
	}

/**
	 * <p>Description copied from corresponding location in WebComponent:</p>
	<p>Allows specifying a responsive behavior with the number of columns
	and the label position depending on the layout width.</p>
	<p>Format: array of objects, each object defines one responsive step
	with {@code minWidth} CSS length, {@code columns} number, and optional
	{@code labelsPosition} string of {@code &quot;aside&quot;} or {@code &quot;top&quot;}. At least one item is required.</p>
	<h4>Examples</h4>
	<dl>
	  <dt>{@code [{columns: 1}]}</dt>
	  <dd>
	    <p>The layout is always a single column, labels aside.
	  </dd>
	  <dt><pre><code>[
	  {minWidth: 0, columns: 1},
	  {minWidth: '40em', columns: 2}
	]</code></pre></dt>
	  <dd>
	    <p>Sets two responsive steps:
	    <ol>
	      <li>When the layout width is < 40em, one column, labels aside.
	      <li>Width >= 40em, two columns, labels aside.
	    </ol>
	  </dd>
	  <dt><pre><code>[
	  {minWidth: 0, columns: 1, labelsPosition: 'top'},
	  {minWidth: '20em', columns: 1},
	  {minWidth: '40em', columns: 2}
	]</code></pre></dt>
	  <dd>
	    <p>Default value. Three responsive steps:
	    <ol>
	      <li>Width < 20em, one column, labels on top.
	      <li>20em <= width < 40em, one column, labels aside.
	      <li>Width >= 40em, two columns, labels aside.
	    </ol>
	  </dd>
	</dl>

	 * @param responsiveSteps the JsonObject value to set
	 */
	protected void setResponsiveSteps(elemental.json.JsonObject responsiveSteps) {
		getElement().setPropertyJson("responsiveSteps", responsiveSteps);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set custom CSS property values and update the layout.
	 * </p>
	 * 
	 * @param ...args Missing documentation!
	 */
	protected void updateStyles(elemental.json.JsonObject _Args) {
		getElement().callFunction("updateStyles", _Args);
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedVaadinFormLayout(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedVaadinFormLayout() {
	}
}