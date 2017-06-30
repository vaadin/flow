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
package com.vaadin.components.iron.a11y.announcer;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code iron-a11y-announcer} is a singleton element that is intended to add
 * a11y to features that require on-demand announcement from screen readers. In
 * order to make use of the announcer, it is best to request its availability in
 * the announcing element.
 * 
 * Example:
 * 
 * Polymer({
 * 
 * is: 'x-chatty',
 * 
 * attached: function() { // This will create the singleton element if it has
 * not // been created yet: Polymer.IronA11yAnnouncer.requestAvailability(); }
 * });
 * 
 * After the {@code iron-a11y-announcer} has been made available, elements can
 * make announces by firing bubbling {@code iron-announce} events.
 * 
 * Example:
 * 
 * this.fire('iron-announce', { text: 'This is an announcement!' }, { bubbles:
 * true });
 * 
 * Note: announcements are only audible if you have a screen reader enabled.
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.11-SNAPSHOT",
		"WebComponent: Polymer.IronA11yAnnouncer#2.0.0", "Flow#0.1.11-SNAPSHOT"})
@Tag("iron-a11y-announcer")
@HtmlImport("frontend://bower_components/iron-a11y-announcer/iron-a11y-announcer.html")
public class IronA11yAnnouncer<R extends IronA11yAnnouncer<R>>
		extends
			Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value of mode is used to set the {@code aria-live} attribute for the
	 * element that will be announced. Valid values are: {@code off},
	 * {@code polite} and {@code assertive}.
	 */
	public String getMode() {
		return getElement().getProperty("mode");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The value of mode is used to set the {@code aria-live} attribute for the
	 * element that will be announced. Valid values are: {@code off},
	 * {@code polite} and {@code assertive}.
	 * 
	 * @param mode
	 * @return This instance, for method chaining.
	 */
	public R setMode(java.lang.String mode) {
		getElement().setProperty("mode", mode == null ? "" : mode);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Cause a text string to be announced by screen readers.
	 * 
	 * @param text
	 */
	public void announce(java.lang.String text) {
		getElement().callFunction("announce", text);
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