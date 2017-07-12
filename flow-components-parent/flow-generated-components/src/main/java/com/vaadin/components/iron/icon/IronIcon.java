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
package com.vaadin.components.iron.icon;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.iron.icon.IronIcon;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * The {@code iron-icon} element displays an icon. By default an icon renders as
 * a 24px square.
 * 
 * Example using src:
 * 
 * <iron-icon src="star.png"></iron-icon>
 * 
 * Example setting size to 32px x 32px:
 * 
 * <iron-icon class="big" src="big_star.png"></iron-icon>
 * 
 * <style is="custom-style"> .big { --iron-icon-height: 32px; --iron-icon-width:
 * 32px; } </style>
 * 
 * The iron elements include several sets of icons. To use the default set of
 * icons, import {@code iron-icons.html} and use the {@code icon} attribute to
 * specify an icon:
 * 
 * <link rel="import" href="/components/iron-icons/iron-icons.html">
 * 
 * <iron-icon icon="menu"></iron-icon>
 * 
 * To use a different built-in set of icons, import the specific
 * {@code iron-icons/<iconset>-icons.html}, and specify the icon as
 * {@code <iconset>:<icon>}. For example, to use a communication icon, you would
 * use:
 * 
 * <link rel="import" href="/components/iron-icons/communication-icons.html">
 * 
 * <iron-icon icon="communication:email"></iron-icon>
 * 
 * You can also create custom icon sets of bitmap or SVG icons.
 * 
 * Example of using an icon named {@code cherry} from a custom iconset with the
 * ID {@code fruit}:
 * 
 * <iron-icon icon="fruit:cherry"></iron-icon>
 * 
 * See [iron-iconset](iron-iconset) and [iron-iconset-svg](iron-iconset-svg) for
 * more information about how to create a custom iconset.
 * 
 * See the [iron-icons demo](iron-icons?view=demo:demo/index.html) to see the
 * icons available in the various iconsets.
 * 
 * To load a subset of icons from one of the default {@code iron-icons} sets,
 * you can use the [poly-icon](https://poly-icon.appspot.com/) tool. It allows
 * you to select individual icons, and creates an iconset from them that you can
 * use directly in your elements.
 * 
 * ### Styling
 * 
 * The following custom properties are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --iron-icon} | Mixin applied
 * to the icon | {} {@code --iron-icon-width} | Width of the icon | {@code 24px}
 * {@code --iron-icon-height} | Height of the icon | {@code 24px}
 * {@code --iron-icon-fill-color} | Fill color of the svg icon |
 * {@code currentcolor} {@code --iron-icon-stroke-color} | Stroke color of the
 * svg icon | none
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: iron-icon#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("iron-icon")
@HtmlImport("frontend://bower_components/iron-icon/iron-icon.html")
public class IronIcon extends Component implements HasStyle {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the icon to use. The name should be of the form:
	 * {@code iconset_name:icon_name}.
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
	 * The name of the icon to use. The name should be of the form:
	 * {@code iconset_name:icon_name}.
	 * 
	 * @param icon
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronIcon> R setIcon(java.lang.String icon) {
		getElement().setProperty("icon", icon == null ? "" : icon);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the theme to used, if one is specified by the iconset.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getTheme() {
		return getElement().getProperty("theme");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The name of the theme to used, if one is specified by the iconset.
	 * 
	 * @param theme
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronIcon> R setTheme(java.lang.String theme) {
		getElement().setProperty("theme", theme == null ? "" : theme);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If using iron-icon without an iconset, you can set the src to be the URL
	 * of an individual icon image file. Note that this will take precedence
	 * over a given icon attribute.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getSrc() {
		return getElement().getProperty("src");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * If using iron-icon without an iconset, you can set the src to be the URL
	 * of an individual icon image file. Note that this will take precedence
	 * over a given icon attribute.
	 * 
	 * @param src
	 *            the String value to set
	 * @return this instance, for method chaining
	 */
	public <R extends IronIcon> R setSrc(java.lang.String src) {
		getElement().setProperty("src", src == null ? "" : src);
		return getSelf();
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends IronIcon> R getSelf() {
		return (R) this;
	}
}