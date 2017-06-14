package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import elemental.json.JsonObject;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * `<paper-input-error>` is an error message for use with
 * `<paper-input-container>`. The error is displayed when the
 * `<paper-input-container>` is `invalid`.
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
 * `--paper-input-container-invalid-color` | The foreground color of the error |
 * `--error-color` `--paper-input-error` | Mixin applied to the error | `{}`
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.9-SNAPSHOT",
		"WebComponent: paper-input/paper-input-error.html/paper-input-error#2.0.1",
		"Flow#0.1.9-SNAPSHOT"})
@Tag("paper-input-error")
public class PaperInputError extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * True if the error is showing.
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
	 */
	public void setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This overrides the update function in PaperInputAddonBehavior.
	 * 
	 * @param state
	 */
	public void update(JsonObject state) {
		getElement().callFunction("update", state);
	}
}