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
package com.vaadin.components.iron.overlay.behavior;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code iron-overlay-backdrop} is a backdrop used by
 * {@code Polymer.IronOverlayBehavior}. It should be a singleton.
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling.
 * 
 * Custom property | Description | Default
 * --------------------------------------
 * -----|------------------------|---------
 * {@code --iron-overlay-backdrop-background-color} | Backdrop background color
 * | #000 {@code --iron-overlay-backdrop-opacity} | Backdrop opacity | 0.6
 * {@code --iron-overlay-backdrop} | Mixin applied to
 * {@code iron-overlay-backdrop}. | {} {@code --iron-overlay-backdrop-opened} |
 * Mixin applied to {@code iron-overlay-backdrop} when it is displayed | {}
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: iron-overlay-backdrop#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("iron-overlay-backdrop")
@HtmlImport("frontend://bower_components/iron-overlay-behavior/iron-overlay-backdrop.html")
public class IronOverlayBackdrop<R extends IronOverlayBackdrop<R>>
		extends
			Component implements HasStyle, HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the backdrop is opened.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns true if the backdrop is opened.
	 * 
	 * @param opened
	 * @return This instance, for method chaining.
	 */
	public R setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Appends the backdrop to document body if needed.
	 */
	public void prepare() {
		getElement().callFunction("prepare");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Shows the backdrop.
	 */
	public void open() {
		getElement().callFunction("open");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Hides the backdrop.
	 */
	public void close() {
		getElement().callFunction("close");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Removes the backdrop from document body if needed.
	 */
	public void complete() {
		getElement().callFunction("complete");
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