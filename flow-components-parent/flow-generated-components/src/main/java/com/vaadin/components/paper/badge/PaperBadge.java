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
package com.vaadin.components.paper.badge;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.components.NotSupported;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code <paper-badge>} is a circular text badge that is displayed on the top
 * right corner of an element, representing a status or a notification. It will
 * badge the anchor element specified in the {@code for} attribute, or, if that
 * doesn't exist, centered to the parent node containing it.
 * 
 * Badges can also contain an icon by adding the {@code icon} attribute and
 * setting it to the id of the desired icon. Please note that you should still
 * set the {@code label} attribute in order to keep the element accessible. Also
 * note that you will need to import the {@code iron-iconset} that includes the
 * icons you want to use. See [iron-icon](../iron-icon) for more information on
 * how to import and use icon sets.
 * 
 * Example:
 * 
 * <div style="display:inline-block"> <span>Inbox</span> <paper-badge
 * label="3"></paper-badge> </div>
 * 
 * <div> <paper-button id="btn">Status</paper-button> <paper-badge
 * icon="favorite" for="btn" label="favorite icon"></paper-badge> </div>
 * 
 * <div> <paper-icon-button id="account-box" icon="account-box"
 * alt="account-box"></paper-icon-button> <paper-badge icon="social:mood"
 * for="account-box" label="mood icon"></paper-badge> </div>
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --paper-badge-background} |
 * The background color of the badge | {@code --accent-color}
 * {@code --paper-badge-opacity} | The opacity of the badge | {@code 1.0}
 * {@code --paper-badge-text-color} | The color of the badge text |
 * {@code white} {@code --paper-badge-width} | The width of the badge circle |
 * {@code 20px} {@code --paper-badge-height} | The height of the badge circle |
 * {@code 20px} {@code --paper-badge-margin-left} | Optional spacing added to
 * the left of the badge. | {@code 0px} {@code --paper-badge-margin-bottom} |
 * Optional spacing added to the bottom of the badge. | {@code 0px}
 * {@code --paper-badge} | Mixin applied to the badge | {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: paper-badge#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("paper-badge")
@HtmlImport("frontend://bower_components/paper-badge/paper-badge.html")
public class PaperBadge<R extends PaperBadge<R>> extends Component
		implements
			HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The id of the element that the badge is anchored to. This element must be
	 * a sibling of the badge.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getFor() {
		return getElement().getProperty("for");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The id of the element that the badge is anchored to. This element must be
	 * a sibling of the badge.
	 * 
	 * @param _for
	 * @return this instance, for method chaining
	 */
	public R setFor(java.lang.String _for) {
		getElement().setProperty("for", _for == null ? "" : _for);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label displayed in the badge. The label is centered, and ideally
	 * should have very few characters.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getLabel() {
		return getElement().getProperty("label");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The label displayed in the badge. The label is centered, and ideally
	 * should have very few characters.
	 * 
	 * @param label
	 * @return this instance, for method chaining
	 */
	public R setLabel(java.lang.String label) {
		getElement().setProperty("label", label == null ? "" : label);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An iron-icon ID. When given, the badge content will use an
	 * {@code <iron-icon>} element displaying the given icon ID rather than the
	 * label text. However, the label text will still be used for accessibility
	 * purposes.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getIcon() {
		return getElement().getProperty("icon");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * An iron-icon ID. When given, the badge content will use an
	 * {@code <iron-icon>} element displaying the given icon ID rather than the
	 * label text. However, the label text will still be used for accessibility
	 * purposes.
	 * 
	 * @param icon
	 * @return this instance, for method chaining
	 */
	public R setIcon(java.lang.String icon) {
		getElement().setProperty("icon", icon == null ? "" : icon);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the target element that this badge is anchored to. It is either
	 * the element given by the {@code for} attribute, or the immediate parent
	 * of the badge.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public JsonObject getTarget() {
		return (JsonObject) getElement().getPropertyRaw("target");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Returns the target element that this badge is anchored to. It is either
	 * the element given by the {@code for} attribute, or the immediate parent
	 * of the badge.
	 * 
	 * @param target
	 * @return this instance, for method chaining
	 */
	public R setTarget(elemental.json.JsonObject target) {
		getElement().setPropertyJson("target", target);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be called to manually notify a resizable and its descendant
	 * resizables of a resize change.
	 */
	public void notifyResize() {
		getElement().callFunction("notifyResize");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Used to assign the closest resizable ancestor to this resizable if the
	 * ancestor detects a request for notifications.
	 * 
	 * @param parentResizable
	 */
	public void assignParentResizable(elemental.json.JsonObject parentResizable) {
		getElement().callFunction("assignParentResizable", parentResizable);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Used to remove a resizable descendant from the list of descendants that
	 * should be notified of a resize change.
	 * 
	 * @param target
	 */
	public void stopResizeNotificationsFor(elemental.json.JsonObject target) {
		getElement().callFunction("stopResizeNotificationsFor", target);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This method can be overridden to filter nested elements that should or
	 * should not be notified by the current element. Return true if an element
	 * should be notified, or false if it should not be notified.
	 * 
	 * @param element
	 * @return It would return a boolean
	 */
	@NotSupported
	protected void resizerShouldNotify(elemental.json.JsonObject element) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Repositions the badge relative to its anchor element. This is called
	 * automatically when the badge is attached or an {@code iron-resize} event
	 * is fired (for exmaple if the window has resized, or your target is a
	 * custom element that implements IronResizableBehavior).
	 * 
	 * You should call this in all other cases when the achor's position might
	 * have changed (for example, if it's visibility has changed, or you've
	 * manually done a page re-layout).
	 */
	public void updatePosition() {
		getElement().callFunction("updatePosition");
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