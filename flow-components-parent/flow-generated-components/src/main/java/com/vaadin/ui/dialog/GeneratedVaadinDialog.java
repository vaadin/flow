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
package com.vaadin.ui.dialog;

import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasStyle;
import com.vaadin.ui.common.ComponentSupplier;
import javax.annotation.Generated;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.Synchronize;
import com.vaadin.ui.event.DomEvent;
import com.vaadin.ui.event.ComponentEvent;
import com.vaadin.ui.event.ComponentEventListener;
import com.vaadin.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-dialog>} is a Polymer 2 element for customised modal dialogs.
 * </p>
 * <p>
 * &lt;vaadin-dialog opened&gt; &lt;template&gt; Sample dialog &lt;/template&gt;
 * &lt;/vaadin-dialog&gt;
 * </p>
 * <h3>Styling</h3>
 * <p>
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
 * <p>
 * See <a href=
 * "https://github.com/vaadin/vaadin-overlay/blob/master/vaadin-overlay.html">
 * {@code <vaadin-overlay>} documentation</a> for
 * {@code <vaadin-dialog-overlay>} parts.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.VaadinDialog#null", "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-dialog")
@HtmlImport("frontend://bower_components/vaadin-dialog/vaadin-dialog.html")
public class GeneratedVaadinDialog<R extends GeneratedVaadinDialog<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the overlay is currently displayed.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * </p>
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
    public boolean isOpened() {
        return getElement().getProperty("opened", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the overlay is currently displayed.
     * </p>
     * 
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
     * Set to true to disable closing dialog on outside click
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noCloseOnOutsideClick} property from the webcomponent
     */
    public boolean isNoCloseOnOutsideClick() {
        return getElement().getProperty("noCloseOnOutsideClick", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable closing dialog on outside click
     * </p>
     * 
     * @param noCloseOnOutsideClick
     *            the boolean value to set
     */
    public void setNoCloseOnOutsideClick(boolean noCloseOnOutsideClick) {
        getElement().setProperty("noCloseOnOutsideClick",
                noCloseOnOutsideClick);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable closing dialog on Escape press
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code noCloseOnEsc} property from the webcomponent
     */
    public boolean isNoCloseOnEsc() {
        return getElement().getProperty("noCloseOnEsc", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Set to true to disable closing dialog on Escape press
     * </p>
     * 
     * @param noCloseOnEsc
     *            the boolean value to set
     */
    public void setNoCloseOnEsc(boolean noCloseOnEsc) {
        getElement().setProperty("noCloseOnEsc", noCloseOnEsc);
    }

    @DomEvent("opened-changed")
    public static class OpenedChangeEvent<R extends GeneratedVaadinDialog<R>>
            extends ComponentEvent<R> {
        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code opened-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     */
    public Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return addListener(OpenedChangeEvent.class,
                (ComponentEventListener) listener);
    }
}