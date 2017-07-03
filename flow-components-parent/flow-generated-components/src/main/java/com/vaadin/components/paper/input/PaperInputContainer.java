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
import com.vaadin.annotations.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <paper-input-container>} is a container for a {@code <label>}, an
 * {@code <iron-input>} or {@code <textarea>} and optional add-on elements such
 * as an error message or character counter, used to implement Material Design
 * text fields.
 * 
 * For example:
 * 
 * <paper-input-container> <label slot="label">Your name</label> <iron-input
 * slot="input"> <input> </iron-input> // In Polymer 1.0, you would use
 * {@code <input is="iron-input" slot="input">} instead of the above.
 * </paper-input-container>
 * 
 * You can style the nested <input> however you want; if you want it to look
 * like a Material Design input, you can style it with the
 * --paper-input-container-shared-input-style mixin.
 * 
 * Do not wrap {@code <paper-input-container>} around elements that already
 * include it, such as {@code <paper-input>}. Doing so may cause events to
 * bounce infinitely between the container and its contained element.
 * 
 * ### Listening for input changes
 * 
 * By default, it listens for changes on the {@code bind-value} attribute on its
 * children nodes and perform tasks such as auto-validating and label styling
 * when the {@code bind-value} changes. You can configure the attribute it
 * listens to with the {@code attr-for-value} attribute.
 * 
 * ### Using a custom input element
 * 
 * You can use a custom input element in a {@code <paper-input-container>}, for
 * example to implement a compound input field like a social security number
 * input. The custom input element should have the {@code paper-input-input}
 * class, have a {@code notify:true} value property and optionally implements
 * {@code Polymer.IronValidatableBehavior} if it is validatable.
 * 
 * <paper-input-container attr-for-value="ssn-value"> <label slot="label">Social
 * security number</label> <ssn-input slot="input"
 * class="paper-input-input"></ssn-input> </paper-input-container>
 * 
 * 
 * If you're using a {@code <paper-input-container>} imperatively, it's
 * important to make sure that you attach its children (the {@code iron-input}
 * and the optional {@code label}) before you attach the
 * {@code <paper-input-container>} itself, so that it can be set up correctly.
 * 
 * ### Validation
 * 
 * If the {@code auto-validate} attribute is set, the input container will
 * validate the input and update the container styling when the input value
 * changes.
 * 
 * ### Add-ons
 * 
 * Add-ons are child elements of a {@code <paper-input-container>} with the
 * {@code add-on} attribute and implements the
 * {@code Polymer.PaperInputAddonBehavior} behavior. They are notified when the
 * input value or validity changes, and may implement functionality such as
 * error messages or character counters. They appear at the bottom of the input.
 * 
 * ### Prefixes and suffixes These are child elements of a
 * {@code <paper-input-container>} with the {@code prefix} or {@code suffix}
 * attribute, and are displayed inline with the input, before or after.
 * 
 * <paper-input-container> <div slot="prefix">$</div> <label
 * slot="label">Total</label> <iron-input slot="input"> <input> </iron-input> //
 * In Polymer 1.0, you would use {@code <input is="iron-input" slot="input">}
 * instead of the above. <paper-icon-button slot="suffix"
 * icon="clear"></paper-icon-button> </paper-input-container>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-input-container-color} | Label and underline color when the
 * input is not focused | {@code --secondary-text-color}
 * {@code --paper-input-container-focus-color} | Label and underline color when
 * the input is focused | {@code --primary-color}
 * {@code --paper-input-container-invalid-color} | Label and underline color
 * when the input is is invalid | {@code --error-color}
 * {@code --paper-input-container-input-color} | Input foreground color |
 * {@code --primary-text-color} {@code --paper-input-container} | Mixin applied
 * to the container | {@code {@code --paper-input-container-disabled} | Mixin
 * applied to the container when it's disabled | {@code
 * {@code --paper-input-container-label} | Mixin applied to the label | {@code
 * {@code --paper-input-container-label-focus} | Mixin applied to the label when
 * the input is focused | {@code {@code --paper-input-container-label-floating}
 * | Mixin applied to the label when floating | {@code
 * {@code --paper-input-container-input} | Mixin applied to the input | {@code
 * {@code --paper-input-container-input-focus} | Mixin applied to the input when
 * focused | {@code {@code --paper-input-container-input-invalid} | Mixin
 * applied to the input when invalid | {@code
 * {@code --paper-input-container-input-webkit-spinner} | Mixin applied to the
 * webkit spinner | {@code {@code --paper-input-container-input-webkit-clear} |
 * Mixin applied to the webkit clear button | {@code
 * {@code --paper-input-container-ms-clear} | Mixin applied to the Internet
 * Explorer clear button | {@code {@code --paper-input-container-underline} |
 * Mixin applied to the underline | {@code
 * {@code --paper-input-container-underline-focus} | Mixin applied to the
 * underline when the input is focused | {@code
 * {@code --paper-input-container-underline-disabled} | Mixin applied to the
 * underline when the input is disabled | {@code {@code --paper-input-prefix} |
 * Mixin applied to the input prefix | {@code {@code --paper-input-suffix} |
 * Mixin applied to the input suffix | {@code
 * 
 * This element is {@code display:block} by default, but you can set the
 * {@code inline} attribute to make it {@code display:inline-block}.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: paper-input-container#2.0.1", "Flow#0.1.12-SNAPSHOT"})
@Tag("paper-input-container")
@HtmlImport("frontend://bower_components/paper-input/paper-input-container.html")
public class PaperInputContainer<R extends PaperInputContainer<R>>
		extends
			Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. The label disappears when the
	 * input value is not null.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isNoLabelFloat() {
		return getElement().getProperty("noLabelFloat", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. The label disappears when the
	 * input value is not null.
	 * 
	 * @param noLabelFloat
	 * @return This instance, for method chaining.
	 */
	public R setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the floating label.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAlwaysFloatLabel() {
		return getElement().getProperty("alwaysFloatLabel", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the floating label.
	 * 
	 * @param alwaysFloatLabel
	 * @return This instance, for method chaining.
	 */
	public R setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to listen for value changes on.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAttrForValue() {
		return getElement().getProperty("attrForValue");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to listen for value changes on.
	 * 
	 * @param attrForValue
	 * @return This instance, for method chaining.
	 */
	public R setAttrForValue(java.lang.String attrForValue) {
		getElement().setProperty("attrForValue",
				attrForValue == null ? "" : attrForValue);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value when it changes.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAutoValidate() {
		return getElement().getProperty("autoValidate", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value when it changes.
	 * 
	 * @param autoValidate
	 * @return This instance, for method chaining.
	 */
	public R setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input is invalid. This property is set automatically when the
	 * input value changes if auto-validating, or when the
	 * {@code iron-input-validate} event is heard from a child.
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
	 * True if the input is invalid. This property is set automatically when the
	 * input value changes if auto-validating, or when the
	 * {@code iron-input-validate} event is heard from a child.
	 * 
	 * @param invalid
	 * @return This instance, for method chaining.
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input has focus.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "focused-changed" event happens.
	 */
	@Synchronize(property = "focused", value = "focused-changed")
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input has focus.
	 * 
	 * @param focused
	 * @return This instance, for method chaining.
	 */
	public R setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Call this to update the state of add-ons.
	 * 
	 * @param state
	 */
	public void updateAddons(JsonObject state) {
		getElement().callFunction("updateAddons", state);
	}

	@DomEvent("focused-changed")
	public static class FocusedChangedEvent
			extends
				ComponentEvent<PaperInputContainer> {
		public FocusedChangedEvent(PaperInputContainer source,
				boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addFocusedChangedListener(
			ComponentEventListener<FocusedChangedEvent> listener) {
		return addListener(FocusedChangedEvent.class, listener);
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