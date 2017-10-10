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
package com.vaadin.ui.renderers;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.vaadin.function.ValueProvider;
import com.vaadin.util.JsonSerializer;

/**
 * A renderer that allows the usage of HTML and Polymer data binding syntax as
 * output. The output of the TemplateRenderer is meant to be used inside
 * {@code <template>} elements.
 * 
 * @author Vaadin Ltd.
 *
 * @param <SOURCE>
 *            the type of the input object used inside the template
 * 
 * @see ValueProvider
 * @see https://www.polymer-project.org/2.0/docs/devguide/templates
 */
public class TemplateRenderer<SOURCE> implements Serializable {

	private String template;
	private Map<String, ValueProvider<SOURCE, ?>> valueProviders;

	/**
	 * Creates a new TemplateRenderer based on the provided template. The
	 * template accepts anything that is allowed inside a {@code <template>}
	 * element, and works with Polymer data binding syntax.
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * {@code
	 * // Prints the index of the item inside a repeating list
	 * TemplateRenderer.of("[[index]]");
	 * 
	 *  // Prints the property of an item
	 * TemplateRenderer.of("<div>Property: [[item.property]]</div>");
	 * }
	 * </pre>
	 * 
	 * @param template
	 *            the template used to render items, not <code>null</code>
	 * @return an initialized TemplateRenderer
	 * @see TemplateRenderer#withProperty(String, ValueProvider)
	 */
	public static <T> TemplateRenderer<T> of(String template) {
		Objects.requireNonNull(template);
		TemplateRenderer<T> renderer = new TemplateRenderer<>();
		renderer.template = template;
		return renderer;
	}

	/**
	 * Sets a property to be used inside the template. Each property is
	 * referenced inside the template by using the {@code [[item.property]]}
	 * syntax.
	 * <p>
	 * Examples:
	 * 
	 * <pre>
	 * {@code
	 * // Regular property
	 * TemplateRenderer.<Person> of("<div>Name: [[item.name]]</div>")
	 * 			.withProperty("name", Person::getName);
	 * 
	 * // Property that uses a bean. Note that in this case the entire "Adress" object will be sent to the template
	 * TemplateRenderer.<Person> of("<span>Street: [[item.address.street]]</span>")
	 * 			.withProperty("address", Person::getAddress); 
	 * 
	 * // In this case only the street is sent
	 * TemplateRenderer.<Person> of("<span>Street: [[item.street]]</span>")
	 * 			.withProperty("street", person -> person.getAddress().getStreet()); // this will send just the street name
	 * }
	 * </pre>
	 * 
	 * Any types supported by the {@link JsonSerializer} are valid types for the
	 * TemplateRenderer.
	 * 
	 * @param property
	 *            the name of the property used inside the template
	 * @param provider
	 *            a {@link ValueProvider} that provides the actual
	 * @return this instance for method chaining
	 */
	public TemplateRenderer<SOURCE> withProperty(String property,
			ValueProvider<SOURCE, ?> provider) {
		valueProviders.put(property, provider);
		return this;
	}

	protected TemplateRenderer() {
		valueProviders = new HashMap<>();
	}

	/**
	 * Gets the template set for this renderer.
	 * 
	 * @return the template, never <code>null</code>
	 */
	public String getTemplate() {
		return template;
	}

	/**
	 * Gets the property mapped to {@link ValueProvider}s in this renderer. The
	 * returned map is immutable.
	 * 
	 * @return the mapped properties, never <code>null</code>
	 */
	public Map<String, ValueProvider<SOURCE, ?>> getValueProviders() {
		return Collections.unmodifiableMap(valueProviders);
	}
}
