/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.combobox;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import elemental.json.JsonObject;
import com.vaadin.flow.component.NotSupported;
import com.vaadin.flow.component.Component;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * Element for internal use only.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.ComboBoxDropdownWrapperElement#UNKNOWN",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-combo-box-dropdown-wrapper")
@HtmlImport("frontend://bower_components/vaadin-combo-box/src/vaadin-combo-box-dropdown-wrapper.html")
public abstract class GeneratedVaadinComboBoxDropdownWrapper<R extends GeneratedVaadinComboBoxDropdownWrapper<R>>
        extends Component implements HasStyle {

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
     * 
     * @return the {@code touchDevice} property from the webcomponent
     */
    protected boolean isTouchDeviceBoolean() {
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
    protected void setTouchDevice(boolean touchDevice) {
        getElement().setProperty("touchDevice", touchDevice);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code opened} property from the webcomponent
     */
    protected boolean isOpenedBoolean() {
        return getElement().getProperty("opened", false);
    }

    /**
     * @param opened
     *            the boolean value to set
     */
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element to position/align the dropdown by.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code positionTarget} property from the webcomponent
     */
    protected JsonObject getPositionTargetJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("positionTarget");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The element to position/align the dropdown by.
     * </p>
     * 
     * @param positionTarget
     *            the JsonObject value to set
     */
    protected void setPositionTarget(JsonObject positionTarget) {
        getElement().setPropertyJson("positionTarget", positionTarget);
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
     * 
     * @return the {@code loading} property from the webcomponent
     */
    protected boolean isLoadingBoolean() {
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
    protected void setLoading(boolean loading) {
        getElement().setProperty("loading", loading);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to propagate the {@code theme} attribute from the host element.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code theme} property from the webcomponent
     */
    protected String getThemeString() {
        return getElement().getProperty("theme");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Used to propagate the {@code theme} attribute from the host element.
     * </p>
     * 
     * @param theme
     *            the String value to set
     */
    protected void setTheme(String theme) {
        getElement().setProperty("theme", theme == null ? "" : theme);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Gets the index of the item with the provided label.
     * </p>
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>double</code>. Functions with return types different than void are
     * not supported at this moment.
     * 
     * @param label
     *            Missing documentation!
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
     * <p>
     * This function is not supported by Flow because it returns a
     * <code>java.lang.String</code>. Functions with return types different than
     * void are not supported at this moment.
     * 
     * @param item
     *            Missing documentation!
     */
    @NotSupported
    protected void getItemLabel(JsonObject item) {
    }

    protected void ensureItemsRendered() {
        getElement().callFunction("ensureItemsRendered");
    }

    protected void adjustScrollPosition() {
        getElement().callFunction("adjustScrollPosition");
    }

    protected void updateViewportBoundaries() {
        getElement().callFunction("updateViewportBoundaries");
    }
}