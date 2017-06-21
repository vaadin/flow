package com.vaadin.components.paper.input;

import com.vaadin.ui.Component;
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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: paper-input-char-counter#2.0.1", "Flow#0.1.10-SNAPSHOT"})
@Tag("paper-input-char-counter")
@HtmlImport("frontend://bower_components/paper-input/paper-input-char-counter.html")
public class PaperInputCharCounter<R extends PaperInputCharCounter<R>>
		extends
			Component {

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