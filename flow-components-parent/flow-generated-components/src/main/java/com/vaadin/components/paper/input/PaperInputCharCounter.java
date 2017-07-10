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
import elemental.json.JsonObject;
import com.vaadin.components.JsonSerializable;
import com.vaadin.components.paper.input.PaperInputCharCounter;

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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-input-char-counter#2.0.1", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-input-char-counter")
@HtmlImport("frontend://bower_components/paper-input/paper-input-char-counter.html")
public class PaperInputCharCounter extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This overrides the update function in PaperInputAddonBehavior.
	 */
	public void update(UpdateState state) {
		getElement().callFunction("update", state.toJson());
	}

	/**
	 * Class that encapsulates the data to be sent to the
	 * {@link PaperInputCharCounter#update(UpdateState)} method.
	 */
	public static class UpdateState implements JsonSerializable {
		private JsonObject internalObject;

		public JsonObject getInputElement() {
			return internalObject.getObject("inputElement");
		}

		public UpdateState setInputElement(
				elemental.json.JsonObject inputElement) {
			this.internalObject.put("inputElement", inputElement);
			return this;
		}

		public String getValue() {
			return internalObject.getString("value");
		}

		public UpdateState setValue(java.lang.String value) {
			this.internalObject.put("value", value);
			return this;
		}

		public boolean isInvalid() {
			return internalObject.getBoolean("invalid");
		}

		public UpdateState setInvalid(boolean invalid) {
			this.internalObject.put("invalid", invalid);
			return this;
		}

		@Override
		public JsonObject toJson() {
			return internalObject;
		}

		@Override
		public UpdateState readJson(elemental.json.JsonObject value) {
			internalObject = value;
			return this;
		}
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends PaperInputCharCounter> R getSelf() {
		return (R) this;
	}
}