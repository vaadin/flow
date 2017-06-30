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
package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <paper-input-char-counter>} is a character counter for use with
 * {@code <paper-input-container>}. It shows the number of characters entered in
 * the input and the max length if it is specified.
 * 
 * <paper-input-container> <input maxlength="20">
 * <paper-input-char-counter></paper-input-char-counter>
 * </paper-input-container>
 * 
 * ### Styling
 * 
 * The following mixin is available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-input-char-counter}
 * | Mixin applied to the element | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.11-SNAPSHOT",
		"WebComponent: paper-input-char-counter#2.0.1", "Flow#0.1.11-SNAPSHOT"})
@Tag("paper-input-char-counter")
@HtmlImport("frontend://bower_components/paper-input/paper-input-char-counter.html")
public class PaperInputCharCounter<R extends PaperInputCharCounter<R>>
		extends
			Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This overrides the update function in PaperInputAddonBehavior.
	 */
	public void update() {
		getElement().callFunction("update");
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