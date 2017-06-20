package com.vaadin.components.iron.a11y.announcer;

import com.vaadin.ui.Component;
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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: Polymer.IronA11yAnnouncer#2.0.0", "Flow#0.1.10-SNAPSHOT"})
@Tag("iron-a11y-announcer")
@HtmlImport("frontend://bower_components/iron-a11y-announcer/iron-a11y-announcer.html")
public class PolymerIronA11yAnnouncer extends Component {

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
	 */
	public void setMode(java.lang.String mode) {
		getElement().setProperty("mode", mode == null ? "" : mode);
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
}