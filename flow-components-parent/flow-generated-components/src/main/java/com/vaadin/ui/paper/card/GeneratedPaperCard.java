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
package com.vaadin.ui.paper.card;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.common.HasComponents;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Material design: <a
 * href="https://www.google.com/design/spec/components/cards.html">Cards</a>
 * </p>
 * <p>
 * {@code paper-card} is a container with a drop shadow.
 * </p>
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-card heading=&quot;Card Title&quot;&gt;
 *   &lt;div class=&quot;card-content&quot;&gt;Some content&lt;/div&gt;
 *   &lt;div class=&quot;card-actions&quot;&gt;
 *     &lt;paper-button&gt;Some action&lt;/paper-button&gt;
 *   &lt;/div&gt;
 * &lt;/paper-card&gt;
 * </code>
 * </pre>
 * <p>
 * Example - top card image:
 * </p>
 * 
 * <pre>
 * <code>&lt;paper-card heading=&quot;Card Title&quot; image=&quot;/path/to/image.png&quot; alt=&quot;image&quot;&gt;
 *   ...
 * &lt;/paper-card&gt;
 * </code>
 * </pre>
 * 
 * <h3>Accessibility</h3>
 * <p>
 * By default, the {@code aria-label} will be set to the value of the
 * {@code heading} attribute.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * The following custom properties and mixins are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Custom property</th>
 * <th>Description</th>
 * <th>Default</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code --paper-card-background-color}</td>
 * <td>The background color of the card</td>
 * <td>{@code --primary-background-color}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-header-color}</td>
 * <td>The color of the header text</td>
 * <td>{@code #000}</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-header}</td>
 * <td>Mixin applied to the card header section</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-header-text}</td>
 * <td>Mixin applied to the title in the card header section</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-header-image}</td>
 * <td>Mixin applied to the image in the card header section</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-header-image-text}</td>
 * <td>Mixin applied to the text overlapping the image in the card header
 * section</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-content}</td>
 * <td>Mixin applied to the card content section</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card-actions}</td>
 * <td>Mixin applied to the card action section</td>
 * <td>{@code</td>
 * </tr>
 * <tr>
 * <td>{@code --paper-card}</td>
 * <td>Mixin applied to the card</td>
 * <td>{@code</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: paper-card#2.0.0", "Flow#1.0-SNAPSHOT"})
@Tag("paper-card")
@HtmlImport("frontend://bower_components/paper-card/paper-card.html")
public class GeneratedPaperCard<R extends GeneratedPaperCard<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle, HasComponents {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The title of the card.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code heading} property from the webcomponent
	 */
	public String getHeading() {
		return getElement().getProperty("heading");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The title of the card.
	 * </p>
	 * 
	 * @param heading
	 *            the String value to set
	 */
	public void setHeading(java.lang.String heading) {
		getElement().setProperty("heading", heading == null ? "" : heading);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The url of the title image of the card.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code image} property from the webcomponent
	 */
	public String getImage() {
		return getElement().getProperty("image");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The url of the title image of the card.
	 * </p>
	 * 
	 * @param image
	 *            the String value to set
	 */
	public void setImage(java.lang.String image) {
		getElement().setProperty("image", image == null ? "" : image);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The text alternative of the card's title image.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code alt} property from the webcomponent
	 */
	public String getAlt() {
		return getElement().getProperty("alt");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The text alternative of the card's title image.
	 * </p>
	 * 
	 * @param alt
	 *            the String value to set
	 */
	public void setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When {@code true}, any change to the image url property will cause the
	 * {@code placeholder} image to be shown until the image is fully rendered.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code preloadImage} property from the webcomponent
	 */
	public boolean isPreloadImage() {
		return getElement().getProperty("preloadImage", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When {@code true}, any change to the image url property will cause the
	 * {@code placeholder} image to be shown until the image is fully rendered.
	 * </p>
	 * 
	 * @param preloadImage
	 *            the boolean value to set
	 */
	public void setPreloadImage(boolean preloadImage) {
		getElement().setProperty("preloadImage", preloadImage);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When {@code preloadImage} is true, setting {@code fadeImage} to true will
	 * cause the image to fade into place.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code fadeImage} property from the webcomponent
	 */
	public boolean isFadeImage() {
		return getElement().getProperty("fadeImage", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When {@code preloadImage} is true, setting {@code fadeImage} to true will
	 * cause the image to fade into place.
	 * </p>
	 * 
	 * @param fadeImage
	 *            the boolean value to set
	 */
	public void setFadeImage(boolean fadeImage) {
		getElement().setProperty("fadeImage", fadeImage);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This image will be used as a background/placeholder until the src image
	 * has loaded. Use of a data-URI for placeholder is encouraged for instant
	 * rendering.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code placeholderImage} property from the webcomponent
	 */
	public String getPlaceholderImage() {
		return getElement().getProperty("placeholderImage");
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * This image will be used as a background/placeholder until the src image
	 * has loaded. Use of a data-URI for placeholder is encouraged for instant
	 * rendering.
	 * </p>
	 * 
	 * @param placeholderImage
	 *            the String value to set
	 */
	public void setPlaceholderImage(java.lang.String placeholderImage) {
		getElement().setProperty("placeholderImage",
				placeholderImage == null ? "" : placeholderImage);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The z-depth of the card, from 0-5.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code elevation} property from the webcomponent
	 */
	public double getElevation() {
		return getElement().getProperty("elevation", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * The z-depth of the card, from 0-5.
	 * </p>
	 * 
	 * @param elevation
	 *            the double value to set
	 */
	public void setElevation(double elevation) {
		getElement().setProperty("elevation", elevation);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set this to true to animate the card shadow when setting a new {@code z}
	 * value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code animatedShadow} property from the webcomponent
	 */
	public boolean isAnimatedShadow() {
		return getElement().getProperty("animatedShadow", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Set this to true to animate the card shadow when setting a new {@code z}
	 * value.
	 * </p>
	 * 
	 * @param animatedShadow
	 *            the boolean value to set
	 */
	public void setAnimatedShadow(boolean animatedShadow) {
		getElement().setProperty("animatedShadow", animatedShadow);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Read-only property used to pass down the {@code animatedShadow} value to
	 * the underlying paper-material style (since they have different names).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code animated} property from the webcomponent
	 */
	public boolean isAnimated() {
		return getElement().getProperty("animated", false);
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public GeneratedPaperCard(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public GeneratedPaperCard() {
	}
}