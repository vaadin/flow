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
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <paper-input-error>} is an error message for use with
 * {@code <paper-input-container>}. The error is displayed when the
 * {@code <paper-input-container>} is {@code invalid}.
 * 
 * <paper-input-container> <input pattern="[0-9]*"> <paper-input-error
 * slot="add-on">Only numbers are allowed!</paper-input-error>
 * </paper-input-container>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-input-container-invalid-color} | The foreground color of the
 * error | {@code --error-color} {@code --paper-input-error} | Mixin applied to
 * the error | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-input-error#2.0.1", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-input-error")
@HtmlImport("frontend://bower_components/paper-input/paper-input-error.html")
public class PaperInputError<R extends PaperInputError<R>> extends Component
		implements
			HasStyle,
			HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the error is showing.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the error is showing.
	 * 
	 * @param invalid
	 * @return this instance, for method chaining
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
	}

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
	 * {@link PaperInputError#update(UpdateState)} method.
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
	protected R getSelf() {
		return (R) this;
	}
}