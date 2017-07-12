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
package com.vaadin.components.paper.item;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.ui.HasComponents;
import com.vaadin.components.paper.item.PaperItemBody;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Use {@code <paper-item-body>} in a {@code <paper-item>} or
 * {@code <paper-icon-item>} to make two- or three- line items. It is a flex
 * item that is a vertical flexbox.
 * 
 * <paper-item> <paper-item-body two-line> <div>Show your status</div> <div
 * secondary>Your status is visible to everyone</div> </paper-item-body>
 * </paper-item>
 * 
 * The child elements with the {@code secondary} attribute is given secondary
 * text styling.
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-item-body-two-line-min-height} | Minimum height of a two-line
 * item | {@code 72px} {@code --paper-item-body-three-line-min-height} | Minimum
 * height of a three-line item | {@code 88px}
 * {@code --paper-item-body-secondary-color} | Foreground color for the
 * {@code secondary} area | {@code --secondary-text-color}
 * {@code --paper-item-body-secondary} | Mixin applied to the {@code secondary}
 * area | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-item-body#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-item-body")
@HtmlImport("frontend://bower_components/paper-item/paper-item-body.html")
public class PaperItemBody extends Component implements HasStyle, HasComponents {

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends PaperItemBody> R getSelf() {
		return (R) this;
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public PaperItemBody(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public PaperItemBody() {
	}
}