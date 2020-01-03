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
package com.vaadin.flow.component.contextmenu;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
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
        "WebComponent: Vaadin.DeviceDetectorElement#UNKNOWN",
        "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-device-detector")
@HtmlImport("frontend://bower_components/vaadin-context-menu/src/vaadin-device-detector.html")
public abstract class GeneratedVaadinDeviceDetector<R extends GeneratedVaadinDeviceDetector<R>>
        extends Component implements HasStyle {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true}, when running in a phone.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'phone-changed' event happens.
     * </p>
     * 
     * @return the {@code phone} property from the webcomponent
     */
    @Synchronize(property = "phone", value = "phone-changed")
    protected boolean isPhoneBoolean() {
        return getElement().getProperty("phone", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true}, when running in a touch device.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'touch-changed' event happens.
     * </p>
     * 
     * @return the {@code touch} property from the webcomponent
     */
    @Synchronize(property = "touch", value = "touch-changed")
    protected boolean isTouchBoolean() {
        return getElement().getProperty("touch", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true}, when running in a touch device.
     * </p>
     * 
     * @param touch
     *            the boolean value to set
     */
    protected void setTouch(boolean touch) {
        getElement().setProperty("touch", touch);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true}, when running in a tablet/desktop device.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'wide-changed' event happens.
     * </p>
     * 
     * @return the {@code wide} property from the webcomponent
     */
    @Synchronize(property = "wide", value = "wide-changed")
    protected boolean isWideBoolean() {
        return getElement().getProperty("wide", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * {@code true}, when running in a tablet/desktop device.
     * </p>
     * 
     * @param wide
     *            the boolean value to set
     */
    protected void setWide(boolean wide) {
        getElement().setProperty("wide", wide);
    }

    public static class PhoneChangeEvent<R extends GeneratedVaadinDeviceDetector<R>>
            extends ComponentEvent<R> {
        private final boolean phone;

        public PhoneChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.phone = source.isPhoneBoolean();
        }

        public boolean isPhone() {
            return phone;
        }
    }

    /**
     * Adds a listener for {@code phone-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addPhoneChangeListener(
            ComponentEventListener<PhoneChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("phone",
                        event -> listener.onComponentEvent(
                                new PhoneChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class TouchChangeEvent<R extends GeneratedVaadinDeviceDetector<R>>
            extends ComponentEvent<R> {
        private final boolean touch;

        public TouchChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.touch = source.isTouchBoolean();
        }

        public boolean isTouch() {
            return touch;
        }
    }

    /**
     * Adds a listener for {@code touch-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addTouchChangeListener(
            ComponentEventListener<TouchChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("touch",
                        event -> listener.onComponentEvent(
                                new TouchChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class WideChangeEvent<R extends GeneratedVaadinDeviceDetector<R>>
            extends ComponentEvent<R> {
        private final boolean wide;

        public WideChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.wide = source.isWideBoolean();
        }

        public boolean isWide() {
            return wide;
        }
    }

    /**
     * Adds a listener for {@code wide-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addWideChangeListener(
            ComponentEventListener<WideChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("wide",
                        event -> listener.onComponentEvent(
                                new WideChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}