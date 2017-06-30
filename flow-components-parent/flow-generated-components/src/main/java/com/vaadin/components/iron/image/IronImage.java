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
package com.vaadin.components.iron.image;

import com.vaadin.ui.Component;
import javax.annotation.Generated;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Synchronize;
import com.vaadin.annotations.DomEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.flow.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * Description copied from corresponding location in WebComponent:
 * 
 * {@code iron-image} is an element for displaying an image that provides useful
 * sizing and preloading options not found on the standard {@code <img>} tag.
 * 
 * The {@code sizing} option allows the image to be either cropped ({@code cover}
 * ) or letterboxed ({@code contain}) to fill a fixed user-size placed on the
 * element.
 * 
 * The {@code preload} option prevents the browser from rendering the image
 * until the image is fully loaded. In the interim, either the element's CSS
 * {@code background-color} can be be used as the placeholder, or the
 * {@code placeholder} property can be set to a URL (preferably a data-URI, for
 * instant rendering) for an placeholder image.
 * 
 * The {@code fade} option (only valid when {@code preload} is set) will cause
 * the placeholder image/color to be faded out once the image is rendered.
 * 
 * Examples:
 * 
 * Basically identical to {@code <img src="...">} tag:
 * 
 * <iron-image src="http://lorempixel.com/400/400"></iron-image>
 * 
 * Will letterbox the image to fit:
 * 
 * <iron-image style="width:400px; height:400px;" sizing="contain"
 * src="http://lorempixel.com/600/400"></iron-image>
 * 
 * Will crop the image to fit:
 * 
 * <iron-image style="width:400px; height:400px;" sizing="cover"
 * src="http://lorempixel.com/600/400"></iron-image>
 * 
 * Will show light-gray background until the image loads:
 * 
 * <iron-image style="width:400px; height:400px; background-color: lightgray;"
 * sizing="cover" preload src="http://lorempixel.com/600/400"></iron-image>
 * 
 * Will show a base-64 encoded placeholder image until the image loads:
 * 
 * <iron-image style="width:400px; height:400px;"
 * placeholder="data:image/gif;base64,..." sizing="cover" preload
 * src="http://lorempixel.com/600/400"></iron-image>
 * 
 * Will fade the light-gray background out once the image is loaded:
 * 
 * <iron-image style="width:400px; height:400px; background-color: lightgray;"
 * sizing="cover" preload fade src="http://lorempixel.com/600/400"></iron-image>
 * 
 * Custom property | Description | Default
 * ----------------|-------------|---------- {@code --iron-image-placeholder} |
 * Mixin applied to #placeholder | {@code {@code --iron-image-width} | Sets the
 * width of the wrapped image | {@code auto} {@code --iron-image-height} | Sets
 * the height of the wrapped image | {@code auto}
 */
@Generated({
		"Generator: com.vaadin.generator.ComponentGenerator#0.1.12-SNAPSHOT",
		"WebComponent: iron-image#2.1.1", "Flow#0.1.12-SNAPSHOT"})
@Tag("iron-image")
@HtmlImport("frontend://bower_components/iron-image/iron-image.html")
public class IronImage<R extends IronImage<R>> extends Component {

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * The URL of an image.
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
	 * The URL of an image.
	 * 
	 * @param src
	 * @return This instance, for method chaining.
	 */
	public R setSrc(java.lang.String src) {
		getElement().setProperty("src", src == null ? "" : src);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * A short text alternative for the image.
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
	 * A short text alternative for the image.
	 * 
	 * @param alt
	 * @return This instance, for method chaining.
	 */
	public R setAlt(java.lang.String alt) {
		getElement().setProperty("alt", alt == null ? "" : alt);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * CORS enabled images support:
	 * https://developer.mozilla.org/en-US/docs/Web/HTML/CORS_enabled_image
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getCrossorigin() {
		return getElement().getProperty("crossorigin");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * CORS enabled images support:
	 * https://developer.mozilla.org/en-US/docs/Web/HTML/CORS_enabled_image
	 * 
	 * @param crossorigin
	 * @return This instance, for method chaining.
	 */
	public R setCrossorigin(java.lang.String crossorigin) {
		getElement().setProperty("crossorigin",
				crossorigin == null ? "" : crossorigin);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When true, the image is prevented from loading and any placeholder is
	 * shown. This may be useful when a binding to the src property is known to
	 * be invalid, to prevent 404 requests.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPreventLoad() {
		return getElement().getProperty("preventLoad", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When true, the image is prevented from loading and any placeholder is
	 * shown. This may be useful when a binding to the src property is known to
	 * be invalid, to prevent 404 requests.
	 * 
	 * @param preventLoad
	 * @return This instance, for method chaining.
	 */
	public R setPreventLoad(boolean preventLoad) {
		getElement().setProperty("preventLoad", preventLoad);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Sets a sizing option for the image. Valid values are {@code contain}
	 * (full aspect ratio of the image is contained within the element and
	 * letterboxed) or {@code cover} (image is cropped in order to fully cover
	 * the bounds of the element), or {@code null} (default: image takes natural
	 * size).
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getSizing() {
		return getElement().getProperty("sizing");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Sets a sizing option for the image. Valid values are {@code contain}
	 * (full aspect ratio of the image is contained within the element and
	 * letterboxed) or {@code cover} (image is cropped in order to fully cover
	 * the bounds of the element), or {@code null} (default: image takes natural
	 * size).
	 * 
	 * @param sizing
	 * @return This instance, for method chaining.
	 */
	public R setSizing(java.lang.String sizing) {
		getElement().setProperty("sizing", sizing == null ? "" : sizing);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When a sizing option is used ({@code cover} or {@code contain}), this
	 * determines how the image is aligned within the element bounds.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public String getPosition() {
		return getElement().getProperty("position");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When a sizing option is used ({@code cover} or {@code contain}), this
	 * determines how the image is aligned within the element bounds.
	 * 
	 * @param position
	 * @return This instance, for method chaining.
	 */
	public R setPosition(java.lang.String position) {
		getElement().setProperty("position", position == null ? "" : position);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, any change to the {@code src} property will cause the
	 * {@code placeholder} image to be shown until the new image has loaded.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isPreload() {
		return getElement().getProperty("preload", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code true}, any change to the {@code src} property will cause the
	 * {@code placeholder} image to be shown until the new image has loaded.
	 * 
	 * @param preload
	 * @return This instance, for method chaining.
	 */
	public R setPreload(boolean preload) {
		getElement().setProperty("preload", preload);
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
	public String getPlaceholder() {
		return getElement().getProperty("placeholder");
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * This image will be used as a background/placeholder until the src image
	 * has loaded. Use of a data-URI for placeholder is encouraged for instant
	 * rendering.
	 * 
	 * @param placeholder
	 * @return This instance, for method chaining.
	 */
	public R setPlaceholder(java.lang.String placeholder) {
		getElement().setProperty("placeholder",
				placeholder == null ? "" : placeholder);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code preload} is true, setting {@code fade} to true will cause the
	 * image to fade into place.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public boolean isFade() {
		return getElement().getProperty("fade", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * When {@code preload} is true, setting {@code fade} to true will cause the
	 * image to fade into place.
	 * 
	 * @param fade
	 * @return This instance, for method chaining.
	 */
	public R setFade(boolean fade) {
		getElement().setProperty("fade", fade);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only value that is true when the image is loaded.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "loaded-changed" event happens.
	 */
	@Synchronize(property = "loaded", value = "loaded-changed")
	public boolean isLoaded() {
		return getElement().getProperty("loaded", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only value that is true when the image is loaded.
	 * 
	 * @param loaded
	 * @return This instance, for method chaining.
	 */
	public R setLoaded(boolean loaded) {
		getElement().setProperty("loaded", loaded);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only value that tracks the loading state of the image when the
	 * {@code preload} option is used.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "loading-changed" event happens.
	 */
	@Synchronize(property = "loading", value = "loading-changed")
	public boolean isLoading() {
		return getElement().getProperty("loading", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only value that tracks the loading state of the image when the
	 * {@code preload} option is used.
	 * 
	 * @param loading
	 * @return This instance, for method chaining.
	 */
	public R setLoading(boolean loading) {
		getElement().setProperty("loading", loading);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only value that indicates that the last set {@code src} failed to
	 * load.
	 * <p>
	 * This property is synchronized automatically from client side when a
	 * "error-changed" event happens.
	 */
	@Synchronize(property = "error", value = "error-changed")
	public boolean isError() {
		return getElement().getProperty("error", false);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Read-only value that indicates that the last set {@code src} failed to
	 * load.
	 * 
	 * @param error
	 * @return This instance, for method chaining.
	 */
	public R setError(boolean error) {
		getElement().setProperty("error", error);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to set the width of image (e.g. via binding); size may also
	 * be set via CSS.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getWidth() {
		return getElement().getProperty("width", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to set the width of image (e.g. via binding); size may also
	 * be set via CSS.
	 * 
	 * @param width
	 * @return This instance, for method chaining.
	 */
	public R setWidth(double width) {
		getElement().setProperty("width", width);
		return getSelf();
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to set the height of image (e.g. via binding); size may also
	 * be set via CSS.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
	public double getHeight() {
		return getElement().getProperty("height", 0.0);
	}

	/**
	 * Description copied from corresponding location in WebComponent:
	 * 
	 * Can be used to set the height of image (e.g. via binding); size may also
	 * be set via CSS.
	 * 
	 * @param height
	 * @return This instance, for method chaining.
	 */
	public R setHeight(double height) {
		getElement().setProperty("height", height);
		return getSelf();
	}

	@DomEvent("loaded-changed")
	public static class LoadedChangedEvent extends ComponentEvent<IronImage> {
		public LoadedChangedEvent(IronImage source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addLoadedChangedListener(
			ComponentEventListener<LoadedChangedEvent> listener) {
		return addListener(LoadedChangedEvent.class, listener);
	}

	@DomEvent("loading-changed")
	public static class LoadingChangedEvent extends ComponentEvent<IronImage> {
		public LoadingChangedEvent(IronImage source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addLoadingChangedListener(
			ComponentEventListener<LoadingChangedEvent> listener) {
		return addListener(LoadingChangedEvent.class, listener);
	}

	@DomEvent("error-changed")
	public static class ErrorChangedEvent extends ComponentEvent<IronImage> {
		public ErrorChangedEvent(IronImage source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addErrorChangedListener(
			ComponentEventListener<ErrorChangedEvent> listener) {
		return addListener(ErrorChangedEvent.class, listener);
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