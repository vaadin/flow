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
package com.vaadin.ui.combobox;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * The overlay element.
 * </p>
 * <h3>Styling</h3>
 * <p>
 * See <a href=
 * "https://github.com/vaadin/vaadin-overlay/blob/master/vaadin-overlay.html">
 * {@code <vaadin-overlay>} documentation</a> for
 * {@code <vaadin-combo-box-overlay>} parts.
 * </p>
 */
@Generated({"Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
		"WebComponent: Vaadin.VaadinComboBoxOverlay#UNKNOWN",
		"Flow#1.0-SNAPSHOT"})
@Tag("vaadin-combo-box-overlay")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-dropdown.html")
public class GeneratedVaadinComboBoxOverlay<R extends GeneratedVaadinComboBoxOverlay<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * This property is synchronized automatically from client side when a
	 * 'opened-changed' event happens.
	 * 
	 * @return the {@code opened} property from the webcomponent
	 */
	@Synchronize(property = "opened", value = "opened-changed")
	public boolean isOpened() {
		return getElement().getProperty("opened", false);
	}

	/**
	 * @param opened
	 *            the boolean value to set
	 */
	public void setOpened(boolean opened) {
		getElement().setProperty("opened", opened);
	}

	/**
	 * This property is synchronized automatically from client side when a
	 * 'template-changed' event happens.
	 * 
	 * @return the {@code template} property from the webcomponent
	 */
	@Synchronize(property = "template", value = "template-changed")
	protected JsonObject protectedGetTemplate() {
		return (JsonObject) getElement().getPropertyRaw("template");
	}

	/**
	 * @param template
	 *            the JsonObject value to set
	 */
	protected void setTemplate(elemental.json.JsonObject template) {
		getElement().setPropertyJson("template", template);
	}

	/**
	 * This property is synchronized automatically from client side when a
	 * 'content-changed' event happens.
	 * 
	 * @return the {@code content} property from the webcomponent
	 */
	@Synchronize(property = "content", value = "content-changed")
	protected JsonObject protectedGetContent() {
		return (JsonObject) getElement().getPropertyRaw("content");
	}

	/**
	 * @param content
	 *            the JsonObject value to set
	 */
	protected void setContent(elemental.json.JsonObject content) {
		getElement().setPropertyJson("content", content);
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * 
	 * @return the {@code withBackdrop} property from the webcomponent
	 */
	public boolean isWithBackdrop() {
		return getElement().getProperty("withBackdrop", false);
	}

	/**
	 * @param withBackdrop
	 *            the boolean value to set
	 */
	public void setWithBackdrop(boolean withBackdrop) {
		getElement().setProperty("withBackdrop", withBackdrop);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When true move focus to the first focusable element in the overlay, or to
	 * the overlay if there are no focusable elements.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 * 
	 * @return the {@code focusTrap} property from the webcomponent
	 */
	public boolean isFocusTrap() {
		return getElement().getProperty("focusTrap", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * When true move focus to the first focusable element in the overlay, or to
	 * the overlay if there are no focusable elements.
	 * </p>
	 * 
	 * @param focusTrap
	 *            the boolean value to set
	 */
	public void setFocusTrap(boolean focusTrap) {
		getElement().setProperty("focusTrap", focusTrap);
	}

	/**
	 * @param sourceEvent
	 *            Missing documentation!
	 */
	protected void close(elemental.json.JsonObject sourceEvent) {
		getElement().callFunction("close", sourceEvent);
	}

	@DomEvent("vaadin-overlay-close")
	public static class VaadinOverlayCloseEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public VaadinOverlayCloseEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinOverlayCloseListener(
			ComponentEventListener<VaadinOverlayCloseEvent<R>> listener) {
		return addListener(VaadinOverlayCloseEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("vaadin-overlay-escape-press")
	public static class VaadinOverlayEscapePressEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public VaadinOverlayEscapePressEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinOverlayEscapePressListener(
			ComponentEventListener<VaadinOverlayEscapePressEvent<R>> listener) {
		return addListener(VaadinOverlayEscapePressEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("vaadin-overlay-open")
	public static class VaadinOverlayOpenEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public VaadinOverlayOpenEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinOverlayOpenListener(
			ComponentEventListener<VaadinOverlayOpenEvent<R>> listener) {
		return addListener(VaadinOverlayOpenEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("vaadin-overlay-outside-click")
	public static class VaadinOverlayOutsideClickEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public VaadinOverlayOutsideClickEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addVaadinOverlayOutsideClickListener(
			ComponentEventListener<VaadinOverlayOutsideClickEvent<R>> listener) {
		return addListener(VaadinOverlayOutsideClickEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("opened-changed")
	public static class OpenedChangeEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public OpenedChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addOpenedChangeListener(
			ComponentEventListener<OpenedChangeEvent<R>> listener) {
		return addListener(OpenedChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("template-changed")
	public static class TemplateChangeEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public TemplateChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addTemplateChangeListener(
			ComponentEventListener<TemplateChangeEvent<R>> listener) {
		return addListener(TemplateChangeEvent.class,
				(ComponentEventListener) listener);
	}

	@DomEvent("content-changed")
	public static class ContentChangeEvent<R extends GeneratedVaadinComboBoxOverlay<R>>
			extends
				ComponentEvent<R> {
		public ContentChangeEvent(R source, boolean fromClient) {
			super(source, fromClient);
		}
	}

	public Registration addContentChangeListener(
			ComponentEventListener<ContentChangeEvent<R>> listener) {
		return addListener(ContentChangeEvent.class,
				(ComponentEventListener) listener);
	}
}