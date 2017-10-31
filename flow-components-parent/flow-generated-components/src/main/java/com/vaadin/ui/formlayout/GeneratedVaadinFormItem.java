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
import com.vaadin.ui.common.HasClickListeners;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-form-item>} is a Polymer 2 element providing labelled form
 * item wrapper for using inside {@code <vaadin-form-layout>}.
 * </p>
 * <p>
 * {@code <vaadin-form-item>} accepts any number of children as the input
 * content, and also has a separate named {@code label} slot:
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;Label aside&lt;/label&gt; &lt;input&gt; &lt;/vaadin-form-item&gt; {@code }</code>
 * </p>
 * <p>
 * Any content can be used. For instance, you can have multiple input elements
 * with surrounding text. The label can be an element of any type:
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-form-item&gt; &lt;span slot=&quot;label&quot;&gt;Date of Birth&lt;/span&gt; &lt;input placeholder=&quot;YYYY&quot; size=&quot;4&quot;&gt; - &lt;input placeholder=&quot;MM&quot; size=&quot;2&quot;&gt; - &lt;input placeholder=&quot;DD&quot; size=&quot;2&quot;&gt;&lt;br&gt; &lt;em&gt;Example: 1900-01-01&lt;/em&gt; &lt;/vaadin-form-item&gt; {@code }</code>
 * </p>
 * <p>
 * The label is optional and can be omitted:
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-form-item&gt; &lt;input type=&quot;checkbox&quot;&gt; Subscribe to our Newsletter &lt;/vaadin-form-item&gt; {@code }</code>
 * </p>
 * <p>
 * By default, the {@code label} slot content is displayed aside of the input
 * content. When {@code label-position=&quot;top&quot;} is set, the
 * {@code label} slot content is displayed on top:
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-form-item label-position=&quot;top&quot;&gt; &lt;label slot=&quot;label&quot;&gt;Label on top&lt;/label&gt; &lt;input&gt; &lt;/vaadin-form-item&gt; {@code }</code>
 * </p>
 * <p>
 * <strong>Note:</strong> Normally, {@code <vaadin-form-item>} is used as a
 * child of a {@code <vaadin-form-layout>} element. Setting
 * {@code label-position} is unnecessary, because the {@code label-position}
 * attribute is triggered automatically by the parent
 * {@code <vaadin-form-layout>}, depending on its width and responsive behavior.
 * </p>
 * <h3>Input Width</h3>
 * <p>
 * By default, {@code <vaadin-form-item>} does not manipulate the width of the
 * slotted input elements. Optionally you can stretch the child input element to
 * fill the available width for the input content by adding the
 * {@code full-width} class:
 * </p>
 * <p>
 * {@code }
 * <code>html &lt;vaadin-form-item&gt; &lt;label slot=&quot;label&quot;&gt;Label&lt;/label&gt; &lt;input class=&quot;full-width&quot;&gt; &lt;/vaadin-form-item&gt; {@code }</code>
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The {@code label-position} host attribute can be used to target the label on
 * top state:
 * </p>
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
 * <p>
 * The following shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>label</td>
 * <td>The label slot container</td>
 * </tr>
 * </tbody>
 * </table>
 * <h3>Custom CSS Properties Reference</h3>
 * <p>
 * The following custom CSS properties are available on the
 * {@code <vaadin-form-item>} element:
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
 * <td>{@code --vaadin-form-item-label-width}</td>
 * <td>Width of the label column when the labels are aside</td>
 * <td>{@code 8em}</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-form-item-label-gap}</td>
 * <td>Length of the gap between the label column and the input column when the
 * labels are aside</td>
 * <td>{@code 1em}</td>
 * </tr>
 * <tr>
 * <td>{@code --vaadin-form-item-row-gap}</td>
 * <td>Height of the gap between the form item elements</td>
 * <td>{@code 1em}</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.FormItemElement#1.0.3", "Flow#1.0-SNAPSHOT"})
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