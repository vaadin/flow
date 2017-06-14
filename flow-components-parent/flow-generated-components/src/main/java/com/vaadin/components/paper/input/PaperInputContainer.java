package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import elemental.json.JsonObject;
import com.vaadin.shared.Registration;
import com.vaadin.flow.dom.DomEventListener;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * `<paper-input-container>` is a container for a `<label>`, an `<iron-input>`
 * or `<textarea>` and optional add-on elements such as an error message or
 * character counter, used to implement Material Design text fields.
 * 
 * For example:
 * 
 * <paper-input-container> <label slot="label">Your name</label> <iron-input
 * slot="input"> <input> </iron-input> // In Polymer 1.0, you would use `<input
 * is="iron-input" slot="input">` instead of the above. </paper-input-container>
 * 
 * You can style the nested <input> however you want; if you want it to look
 * like a Material Design input, you can style it with the
 * --paper-input-container-shared-input-style mixin.
 * 
 * Do not wrap `<paper-input-container>` around elements that already include
 * it, such as `<paper-input>`. Doing so may cause events to bounce infinitely
 * between the container and its contained element.
 * 
 * ### Listening for input changes
 * 
 * By default, it listens for changes on the `bind-value` attribute on its
 * children nodes and perform tasks such as auto-validating and label styling
 * when the `bind-value` changes. You can configure the attribute it listens to
 * with the `attr-for-value` attribute.
 * 
 * ### Using a custom input element
 * 
 * You can use a custom input element in a `<paper-input-container>`, for
 * example to implement a compound input field like a social security number
 * input. The custom input element should have the `paper-input-input` class,
 * have a `notify:true` value property and optionally implements
 * `Polymer.IronValidatableBehavior` if it is validatable.
 * 
 * <paper-input-container attr-for-value="ssn-value"> <label slot="label">Social
 * security number</label> <ssn-input slot="input"
 * class="paper-input-input"></ssn-input> </paper-input-container>
 * 
 * 
 * If you're using a `<paper-input-container>` imperatively, it's important to
 * make sure that you attach its children (the `iron-input` and the optional
 * `label`) before you attach the `<paper-input-container>` itself, so that it
 * can be set up correctly.
 * 
 * ### Validation
 * 
 * If the `auto-validate` attribute is set, the input container will validate
 * the input and update the container styling when the input value changes.
 * 
 * ### Add-ons
 * 
 * Add-ons are child elements of a `<paper-input-container>` with the `add-on`
 * attribute and implements the `Polymer.PaperInputAddonBehavior` behavior. They
 * are notified when the input value or validity changes, and may implement
 * functionality such as error messages or character counters. They appear at
 * the bottom of the input.
 * 
 * ### Prefixes and suffixes These are child elements of a
 * `<paper-input-container>` with the `prefix` or `suffix` attribute, and are
 * displayed inline with the input, before or after.
 * 
 * <paper-input-container> <div slot="prefix">$</div> <label
 * slot="label">Total</label> <iron-input slot="input"> <input> </iron-input> //
 * In Polymer 1.0, you would use `<input is="iron-input" slot="input">` instead
 * of the above. <paper-icon-button slot="suffix"
 * icon="clear"></paper-icon-button> </paper-input-container>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- `--paper-input-container-color` |
 * Label and underline color when the input is not focused |
 * `--secondary-text-color` `--paper-input-container-focus-color` | Label and
 * underline color when the input is focused | `--primary-color`
 * `--paper-input-container-invalid-color` | Label and underline color when the
 * input is is invalid | `--error-color` `--paper-input-container-input-color` |
 * Input foreground color | `--primary-text-color` `--paper-input-container` |
 * Mixin applied to the container | `{}` `--paper-input-container-disabled` |
 * Mixin applied to the container when it's disabled | `{}`
 * `--paper-input-container-label` | Mixin applied to the label | `{}`
 * `--paper-input-container-label-focus` | Mixin applied to the label when the
 * input is focused | `{}` `--paper-input-container-label-floating` | Mixin
 * applied to the label when floating | `{}` `--paper-input-container-input` |
 * Mixin applied to the input | `{}` `--paper-input-container-input-focus` |
 * Mixin applied to the input when focused | `{}`
 * `--paper-input-container-input-invalid` | Mixin applied to the input when
 * invalid | `{}` `--paper-input-container-input-webkit-spinner` | Mixin applied
 * to the webkit spinner | `{}` `--paper-input-container-input-webkit-clear` |
 * Mixin applied to the webkit clear button | `{}`
 * `--paper-input-container-ms-clear` | Mixin applied to the Internet Explorer
 * clear button | `{}` `--paper-input-container-underline` | Mixin applied to
 * the underline | `{}` `--paper-input-container-underline-focus` | Mixin
 * applied to the underline when the input is focused | `{}`
 * `--paper-input-container-underline-disabled` | Mixin applied to the underline
 * when the input is disabled | `{}` `--paper-input-prefix` | Mixin applied to
 * the input prefix | `{}` `--paper-input-suffix` | Mixin applied to the input
 * suffix | `{}`
 * 
 * This element is `display:block` by default, but you can set the `inline`
 * attribute to make it `display:inline-block`.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.9-SNAPSHOT",
		"WebComponent: paper-input/paper-input-container.html/paper-input-container#2.0.1",
		"Flow#0.1.9-SNAPSHOT"})
@Tag("paper-input-container")
public class PaperInputContainer extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to disable the floating label. The label disappears when the
	 * input value is not null.
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
	 */
	public void setNoLabelFloat(boolean noLabelFloat) {
		getElement().setProperty("noLabelFloat", noLabelFloat);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to always float the floating label.
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
	 */
	public void setAlwaysFloatLabel(boolean alwaysFloatLabel) {
		getElement().setProperty("alwaysFloatLabel", alwaysFloatLabel);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The attribute to listen for value changes on.
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
	 */
	public void setAttrForValue(java.lang.String attrForValue) {
		getElement().setProperty("attrForValue",
				attrForValue == null ? "" : attrForValue);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to auto-validate the input value when it changes.
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
	 */
	public void setAutoValidate(boolean autoValidate) {
		getElement().setProperty("autoValidate", autoValidate);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input is invalid. This property is set automatically when the
	 * input value changes if auto-validating, or when the `iron-input-validate`
	 * event is heard from a child.
	 */
	public boolean isInvalid() {
		return getElement().getProperty("invalid", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input is invalid. This property is set automatically when the
	 * input value changes if auto-validating, or when the `iron-input-validate`
	 * event is heard from a child.
	 * 
	 * @param invalid
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input has focus.
	 */
	public boolean isFocused() {
		return getElement().getProperty("focused", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the input has focus.
	 * 
	 * @param focused
	 */
	public void setFocused(boolean focused) {
		getElement().setProperty("focused", focused);
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

	public Registration addFocusedChangedListener(DomEventListener listener) {
		return getElement().addEventListener("focused-changed", listener);
	}
}