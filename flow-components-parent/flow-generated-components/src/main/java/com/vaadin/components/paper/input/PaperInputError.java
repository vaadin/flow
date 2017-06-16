package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;

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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: paper-input-error#2.0.1", "Flow#0.1.10-SNAPSHOT"})
@Tag("paper-input-error")
@HtmlImport("frontend://bower_components/paper-input/paper-input-error.html")
public class PaperInputError<R extends PaperInputError<R>> extends Component {

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
	 * @return This instance, for method chaining.
	 */
	public R setInvalid(boolean invalid) {
		getElement().setProperty("invalid", invalid);
		return getSelf();
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