package com.vaadin.components.paper.card;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;

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
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.10-SNAPSHOT",
		"WebComponent: paper-card#2.0.0", "Flow#0.1.10-SNAPSHOT"})
@Tag("paper-card")
@HtmlImport("frontend://bower_components/paper-card/paper-card.html")
public class PaperCard extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The title of the card.
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
	 */
	public void setHeading(java.lang.String heading) {
		getElement().setProperty("heading", heading == null ? "" : heading);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The url of the title image of the card.
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
	 */
	public void setImage(java.lang.String image) {
		getElement().setProperty("image", image == null ? "" : image);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The text alternative of the card's title image.
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
	 */
	public void setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, any change to the image url property will cause the
	 * {@code placeholder} image to be shown until the image is fully rendered.
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
	 */
	public void setPreloadImage(boolean preloadImage) {
		getElement().setProperty("preloadImage", preloadImage);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code preloadImage} is true, setting {@code fadeImage} to true will
	 * cause the image to fade into place.
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
	 */
	public void setFadeImage(boolean fadeImage) {
		getElement().setProperty("fadeImage", fadeImage);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This image will be used as a background/placeholder until the src image
	 * has loaded. Use of a data-URI for placeholder is encouraged for instant
	 * rendering.
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
	 */
	public void setPlaceholderImage(java.lang.String placeholderImage) {
		getElement().setProperty("placeholderImage",
				placeholderImage == null ? "" : placeholderImage);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The z-depth of the card, from 0-5.
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
	 */
	public void setElevation(double elevation) {
		getElement().setProperty("elevation", elevation);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Set this to true to animate the card shadow when setting a new {@code z}
	 * value.
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
	 */
	public void setAnimatedShadow(boolean animatedShadow) {
		getElement().setProperty("animatedShadow", animatedShadow);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only property used to pass down the {@code animatedShadow} value to
	 * the underlying paper-material style (since they have different names).
	 */
	public boolean isAnimated() {
		return getElement().getProperty("animated", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only property used to pass down the {@code animatedShadow} value to
	 * the underlying paper-material style (since they have different names).
	 * 
	 * @param animated
	 */
	public void setAnimated(boolean animated) {
		getElement().setProperty("animated", animated);
	}
}