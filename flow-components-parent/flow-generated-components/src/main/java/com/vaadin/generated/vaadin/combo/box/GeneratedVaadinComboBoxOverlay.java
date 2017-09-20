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
package com.vaadin.generated.vaadin.combo.box;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.ComponentSupplier;
import com.vaadin.ui.common.HasStyle;
import javax.annotation.Generated;
import com.vaadin.ui.event.Tag;
import com.vaadin.ui.common.HtmlImport;
import elemental.json.JsonObject;
import com.vaadin.ui.common.NotSupported;

@Generated({"Generator: com.vaadin.generator.ComponentGenerator#0.1-SNAPSHOT",
		"WebComponent: Vaadin.ComboBoxOverlayElement#3.0.0-alpha3",
		"Flow#0.1-SNAPSHOT"})
@Tag("vaadin-combo-box-overlay")
@HtmlImport("frontend://bower_components/vaadin-combo-box/vaadin-combo-box-overlay.html")
public class GeneratedVaadinComboBoxOverlay<R extends GeneratedVaadinComboBoxOverlay<R>>
		extends
			Component implements ComponentSupplier<R>, HasStyle {

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the device supports touch events.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isTouchDevice() {
		return getElement().getProperty("touchDevice", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * True if the device supports touch events.
	 * </p>
	 * 
	 * @param touchDevice
	 *            the boolean value to set
	 */
	public void setTouchDevice(boolean touchDevice) {
		getElement().setProperty("touchDevice", touchDevice);
	}

	/**
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 */
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
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * {@code true} when new items are being loaded.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public boolean isLoading() {
		return getElement().getProperty("loading", false);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * {@code true} when new items are being loaded.
	 * </p>
	 * 
	 * @param loading
	 *            the boolean value to set
	 */
	public void setLoading(boolean loading) {
		getElement().setProperty("loading", loading);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Vertical offset for the overlay position.
	 * <p>
	 * This property is not synchronized automatically from the client side, so
	 * the returned value may not be the same as in client side.
	 * </p>
	 */
	public double getVerticalOffset() {
		return getElement().getProperty("verticalOffset", 0.0);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Vertical offset for the overlay position.
	 * </p>
	 * 
	 * @param verticalOffset
	 *            the double value to set
	 */
	public void setVerticalOffset(double verticalOffset) {
		getElement().setProperty("verticalOffset", verticalOffset);
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Gets the index of the item with the provided label.
	 * </p>
	 * 
	 * @param label
	 *            Missing documentation!
	 * @return It would return a double
	 */
	@NotSupported
	protected void indexOfLabel(JsonObject label) {
	}

	/**
	 * <p>
	 * Description copied from corresponding location in WebComponent:
	 * </p>
	 * <p>
	 * Gets the label string for the item based on the {@code _itemLabelPath}.
	 * </p>
	 * 
	 * @param item
	 *            Missing documentation!
	 * @return It would return a class java.lang.String
	 */
	@NotSupported
	protected void getItemLabel(elemental.json.JsonObject item) {
	}

	public void ensureItemsRendered() {
		getElement().callFunction("ensureItemsRendered");
	}

	public void adjustScrollPosition() {
		getElement().callFunction("adjustScrollPosition");
	}

	public void updateViewportBoundaries() {
		getElement().callFunction("updateViewportBoundaries");
	}
}