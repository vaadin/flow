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
package com.vaadin.generated.vaadin.form.layout;

import com.vaadin.ui.Component;
import com.vaadin.ui.ComponentSupplier;
import com.vaadin.ui.HasStyle;
import com.vaadin.ui.HasClickListeners;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <vaadin-form-item>} is a Polymer 2 element providing labelled form
 * item wrapper for using inside {@code <vaadin-form-layout>}.
 * 
 * {@code <vaadin-form-item>} accepts any number of children as the input
 * content, and also has a separate named {@code label} slot:
 * 
 * {@code }`html <vaadin-form-item> <label slot="label">Label aside</label>
 * <input> </vaadin-form-item> {@code }`
 * 
 * Any content can be used. For instance, you can have multiple input elements
 * with surrounding text. The label can be an element of any type:
 * 
 * {@code }`html <vaadin-form-item> <span slot="label">Date of Birth</span>
 * <input placeholder="YYYY" size="4"> - <input placeholder="MM" size="2"> -
 * <input placeholder="DD" size="2"><br>
 * <em>Example: 1900-01-01</em> </vaadin-form-item> {@code }`
 * 
 * The label is optional and can be omitted:
 * 
 * {@code }`html <vaadin-form-item> <input type="checkbox"> Subscribe to our
 * Newsletter </vaadin-form-item> {@code }`
 * 
 * By default, the {@code label} slot content is displayed aside of the input
 * content. When {@code label-position="top"} is set, the {@code label} slot
 * content is displayed on top:
 * 
 * {@code }`html <vaadin-form-item label-position="top"> <label
 * slot="label">Label on top</label> <input> </vaadin-form-item> {@code }`
 * 
 ** Note:** Normally, {@code <vaadin-form-item>} is used as a child of a
 * {@code <vaadin-form-layout>} element. Setting {@code label-position} is
 * unnecessary, because the {@code label-position} attribute is triggered
 * automatically by the parent {@code <vaadin-form-layout>}, depending on its
 * width and responsive behavior.
 * 
 * ### Input Width
 * 
 * By default, {@code <vaadin-form-item>} does not manipulate the width of the
 * slotted input elements. Optionally you can stretch the child input element to
 * fill the available width for the input content by adding the
 * {@code full-width} class:
 * 
 * {@code }`html <vaadin-form-item> <label slot="label">Label</label> <input
 * class="full-width"> </vaadin-form-item> {@code }`
 * 
 * ### Styling
 * 
 * The {@code label-position} host attribute can be used to target the label on
 * top state:
 * 
 * <pre>
 * <code>
 * &lt;dom-module id="my-form-item-theme" theme-for="vaadin-form-item"&gt;
 *   &lt;template&gt;
 *     &lt;style&gt;
 *       :host {
 *         /&#42; default state styles, label aside &#42;/
 *       }
 * 
 *       :host([label-position="top"]) {
 *         /&#42; label on top state styles &#42;/
 *       }
 *     &lt;/style&gt;
 *   &lt;/template&gt;
 * &lt;/dom-module&gt;
 * </code>
 * </pre>
 * 
 * The following shadow DOM parts are available for styling:
 * 
 * Part name | Description ---|--- label | The label slot container
 * 
 * ### Custom CSS Properties Reference
 * 
 * The following custom CSS properties are available on the
 * {@code <vaadin-form-item>} element:
 * 
 * Custom CSS property | Description | Default ---|---|---
 * {@code --vaadin-form-item-label-width} | Width of the label column when the
 * labels are aside | {@code 8em} {@code --vaadin-form-item-label-gap} | Length
 * of the gap between the label column and the input column when the labels are
 * aside | {@code 1em} {@code --vaadin-form-item-row-gap} | Height of the gap
 * between the form item elements | {@code 1em}
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.17-SNAPSHOT",
		"WebComponent: Vaadin.FormItemElement#null", "Flow#0.1.17-SNAPSHOT"})
@Tag("vaadin-form-item")
@HtmlImport("frontend://bower_components/vaadin-form-layout/vaadin-form-item.html")
public class GeneratedVaadinFormItem<R extends GeneratedVaadinFormItem<R>>
		extends
			Component
		implements
			ComponentSupplier<R>,
			HasStyle,
			HasClickListeners<R>,
			HasComponents {

	/**
	 * Adds the given components as children of this component at the slot
	 * 'label'.
	 * 
	 * @param components
	 *            The components to add.
	 * @see <a
	 *      href="https://developer.mozilla.org/en-US/docs/Web/HTML/Element/slot">MDN
	 *      page about slots</a>
	 * @see <a
	 *      href="https://html.spec.whatwg.org/multipage/scripting.html#the-slot-element">Spec
	 *      website about slots</a>
	 * @return this instance, for method chaining
	 */
	public R addToLabel(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			component.getElement().setAttribute("slot", "label");
			getElement().appendChild(component.getElement());
		}
		return get();
	}

	@Override
	public void remove(com.vaadin.ui.Component... components) {
		for (Component component : components) {
			if (getElement().equals(component.getElement().getParent())) {
				component.getElement().removeAttribute("slot");
				getElement().removeChild(component.getElement());
			} else {
				throw new IllegalArgumentException("The given component ("
						+ component + ") is not a child of this component");
			}
		}
	}

	@Override
	public void removeAll() {
		getElement().getChildren().forEach(
				child -> child.removeAttribute("slot"));
		getElement().removeAllChildren();
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedVaadinFormItem(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedVaadinFormItem() {
	}
}