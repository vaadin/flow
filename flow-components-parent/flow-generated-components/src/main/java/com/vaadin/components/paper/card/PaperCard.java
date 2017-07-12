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
package com.vaadin.components.paper.card;

import com.vaadin.ui.Component;
import com.vaadin.ui.HasStyle;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.components.paper.card.PaperCard;
import com.vaadin.ui.HasComponents;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * Material design:
 * [Cards](https://www.google.com/design/spec/components/cards.html)
 * 
 * {@code paper-card} is a container with a drop shadow.
 * 
 * Example:
 * 
 * <paper-card heading="Card Title"> <div class="card-content">Some
 * content</div> <div class="card-actions"> <paper-button>Some
 * action</paper-button> </div> </paper-card>
 * 
 * Example - top card image:
 * 
 * <paper-card heading="Card Title" image="/path/to/image.png" alt="image"> ...
 * </paper-card>
 * 
 * ### Accessibility
 * 
 * By default, the {@code aria-label} will be set to the value of the
 * {@code heading} attribute.
 * 
 * ### Styling
 * 
 * The following custom properties and mixins are available for styling:
 * 
 * Custom property | Description | Default
 * ----------------|-------------|----------
 * {@code --paper-card-background-color} | The background color of the card |
 * {@code --primary-background-color} {@code --paper-card-header-color} | The
 * color of the header text | {@code #000} {@code --paper-card-header} | Mixin
 * applied to the card header section | {@code {@code --paper-card-header-text}
 * | Mixin applied to the title in the card header section | {@code
 * {@code --paper-card-header-image} | Mixin applied to the image in the card
 * header section | {@code {@code --paper-card-header-image-text} | Mixin
 * applied to the text overlapping the image in the card header section |
 * {@code {@code --paper-card-content} | Mixin applied to the card content
 * section| {@code {@code --paper-card-actions} | Mixin applied to the card
 * action section | {@code {@code --paper-card} | Mixin applied to the card |
 * {@code
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.13-SNAPSHOT",
		"WebComponent: paper-card#2.0.0", "Flow#0.1.13-SNAPSHOT"})
@Tag("paper-card")
@HtmlImport("frontend://bower_components/paper-card/paper-card.html")
public class PaperCard extends Component implements HasStyle, HasComponents {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The title of the card.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getHeading() {
		return getElement().getProperty("heading");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The title of the card.
	 * 
	 * @param heading
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setHeading(java.lang.String heading) {
		getElement().setProperty("heading", heading == null ? "" : heading);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The url of the title image of the card.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getImage() {
		return getElement().getProperty("image");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The url of the title image of the card.
	 * 
	 * @param image
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setImage(java.lang.String image) {
		getElement().setProperty("image", image == null ? "" : image);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The text alternative of the card's title image.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getAlt() {
		return getElement().getProperty("alt");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The text alternative of the card's title image.
	 * 
	 * @param alt
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, any change to the image url property will cause the
	 * {@code placeholder} image to be shown until the image is fully rendered.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPreloadImage() {
		return getElement().getProperty("preloadImage", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, any change to the image url property will cause the
	 * {@code placeholder} image to be shown until the image is fully rendered.
	 * 
	 * @param preloadImage
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setPreloadImage(boolean preloadImage) {
		getElement().setProperty("preloadImage", preloadImage);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code preloadImage} is true, setting {@code fadeImage} to true will
	 * cause the image to fade into place.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFadeImage() {
		return getElement().getProperty("fadeImage", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code preloadImage} is true, setting {@code fadeImage} to true will
	 * cause the image to fade into place.
	 * 
	 * @param fadeImage
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setFadeImage(boolean fadeImage) {
		getElement().setProperty("fadeImage", fadeImage);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This image will be used as a background/placeholder until the src image
	 * has loaded. Use of a data-URI for placeholder is encouraged for instant
	 * rendering.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPlaceholderImage() {
		return getElement().getProperty("placeholderImage");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This image will be used as a background/placeholder until the src image
	 * has loaded. Use of a data-URI for placeholder is encouraged for instant
	 * rendering.
	 * 
	 * @param placeholderImage
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setPlaceholderImage(
			java.lang.String placeholderImage) {
		getElement().setProperty("placeholderImage",
				placeholderImage == null ? "" : placeholderImage);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The z-depth of the card, from 0-5.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getElevation() {
		return getElement().getProperty("elevation", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The z-depth of the card, from 0-5.
	 * 
	 * @param elevation
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setElevation(double elevation) {
		getElement().setProperty("elevation", elevation);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true to animate the card shadow when setting a new {@code z}
	 * value.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAnimatedShadow() {
		return getElement().getProperty("animatedShadow", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true to animate the card shadow when setting a new {@code z}
	 * value.
	 * 
	 * @param animatedShadow
	 * @return this instance, for method chaining
	 */
	public <R extends PaperCard> R setAnimatedShadow(boolean animatedShadow) {
		getElement().setProperty("animatedShadow", animatedShadow);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only property used to pass down the {@code animatedShadow} value to
	 * the underlying paper-material style (since they have different names).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isAnimated() {
		return getElement().getProperty("animated", false);
	}

	/**
	 * Gets the narrow typed reference to this object. Subclasses should
	 * override this method to support method chaining using the inherited type.
	 * 
	 * @return This object casted to its type.
	 */
	protected <R extends PaperCard> R getSelf() {
		return (R) this;
	}

	/**
	 * Adds the given components as children of this component.
	 * 
	 * @param components
	 *            the components to add
	 * @see HasComponents#add(Component...)
	 */
	public PaperCard(com.vaadin.ui.Component... components) {
		add(components);
	}

	/**
	 * Default constructor.
	 */
	public PaperCard() {
	}
}