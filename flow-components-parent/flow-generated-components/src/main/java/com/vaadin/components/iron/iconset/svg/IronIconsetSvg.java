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
package com.vaadin.components.iron.iconset.svg;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.NotSupported;
import elemental.json.JsonObject;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * The {@code iron-iconset-svg} element allows users to define their own icon
 * sets that contain svg icons. The svg icon elements should be children of the
 * {@code iron-iconset-svg} element. Multiple icons should be given distinct
 * id's.
 * 
 * Using svg elements to create icons has a few advantages over traditional
 * bitmap graphics like jpg or png. Icons that use svg are vector based so they
 * are resolution independent and should look good on any device. They are
 * stylable via css. Icons can be themed, colorized, and even animated.
 * 
 * Example:
 * 
 * <iron-iconset-svg name="my-svg-icons" size="24"> <svg> <defs> <g id="shape">
 * <rect x="12" y="0" width="12" height="24" /> <circle cx="12" cy="12" r="12"
 * /> </g> </defs> </svg> </iron-iconset-svg>
 * 
 * This will automatically register the icon set "my-svg-icons" to the iconset
 * database. To use these icons from within another element, make a
 * {@code iron-iconset} element and call the {@code byId} method to retrieve a
 * given iconset. To apply a particular icon inside an element use the
 * {@code applyIcon} method. For example:
 * 
 * iconset.applyIcon(iconNode, 'car');
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: iron-iconset-svg#2.0.0", "Flow#0.1.12-SNAPSHOT"})
@Tag("iron-iconset-svg")
@HtmlImport("frontend://bower_components/iron-iconset-svg/iron-iconset-svg.html")
public class IronIconsetSvg<R extends IronIconsetSvg<R>> extends Component
		implements
			HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the iconset.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getName() {
		return getElement().getProperty("name");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the iconset.
	 * 
	 * @param name
	 * @return This instance, for method chaining.
	 */
	public R setName(java.lang.String name) {
		getElement().setProperty("name", name == null ? "" : name);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The size of an individual icon. Note that icons must be square.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getSize() {
		return getElement().getProperty("size", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The size of an individual icon. Note that icons must be square.
	 * 
	 * @param size
	 * @return This instance, for method chaining.
	 */
	public R setSize(double size) {
		getElement().setProperty("size", size);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable mirroring of icons where specified when they are
	 * stamped. Icons that should be mirrored should be decorated with a
	 * {@code mirror-in-rtl} attribute.
	 * 
	 * NOTE: For performance reasons, direction will be resolved once per
	 * document per iconset, so moving icons in and out of RTL subtrees will not
	 * cause their mirrored state to change.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isRtlMirroring() {
		return getElement().getProperty("rtlMirroring", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set to true to enable mirroring of icons where specified when they are
	 * stamped. Icons that should be mirrored should be decorated with a
	 * {@code mirror-in-rtl} attribute.
	 * 
	 * NOTE: For performance reasons, direction will be resolved once per
	 * document per iconset, so moving icons in and out of RTL subtrees will not
	 * cause their mirrored state to change.
	 * 
	 * @param rtlMirroring
	 * @return This instance, for method chaining.
	 */
	public R setRtlMirroring(boolean rtlMirroring) {
		getElement().setProperty("rtlMirroring", rtlMirroring);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Construct an array of all icon names in this iconset.
	 * 
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void getIconNames() {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Applies an icon to the given element.
	 * 
	 * An svg icon is prepended to the element's shadowRoot if it exists,
	 * otherwise to the element itself.
	 * 
	 * If RTL mirroring is enabled, and the icon is marked to be mirrored in
	 * RTL, the element will be tested (once and only once ever for each
	 * iconset) to determine the direction of the subtree the element is in.
	 * This direction will apply to all future icon applications, although only
	 * icons marked to be mirrored will be affected.
	 * 
	 * @param element
	 * @param iconName
	 * @return It would return a interface elemental.json.JsonObject
	 */
	@NotSupported
	protected void applyIcon(JsonObject element, java.lang.String iconName) {
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Remove an icon from the given element by undoing the changes effected by
	 * {@code applyIcon}.
	 * 
	 * @param element
	 */
	public void removeIcon(elemental.json.JsonObject element) {
		getElement().callFunction("removeIcon", element);
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