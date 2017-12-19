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
package com.vaadin.flow.component.notification;

import javax.annotation.Generated;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentSupplier;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.shared.Registration;

/**
 * <p>
 * Description copied from corresponding location in WebComponent:
 * </p>
 * <p>
 * {@code <vaadin-notification>} is a Polymer 2 element providing accessible and
 * customizable notifications (toasts).
 * </p>
 * <p>
 * {@code
<vaadin-notification>
<template>
Your work has been saved
</template>
</vaadin-notification>}
 * </p>
 * <h3>Styling</h3>
 * <p>
 * <a href=
 * "https://cdn.vaadin.com/vaadin-valo-theme/0.3.1/demo/customization.html"
 * >Generic styling/theming documentation</a>
 * </p>
 * <p>
 * The following Shadow DOM parts are available for styling:
 * </p>
 * <table>
 * <thead>
 * <tr>
 * <th>Part name</th>
 * <th>Description</th>
 * <th>Theme for Element</th>
 * </tr>
 * </thead> <tbody>
 * <tr>
 * <td>{@code content}</td>
 * <td>The container of all regions</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code top-stretch}</td>
 * <td>top-stretch container</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code top}</td>
 * <td>top container</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code top-start}</td>
 * <td>top-start container, child of top</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code top-center}</td>
 * <td>top-center container, child of top</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code top-end}</td>
 * <td>top-end container, child of top</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code middle}</td>
 * <td>middle container</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code bottom}</td>
 * <td>bottom container</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code bottom-start}</td>
 * <td>bottom-start container, child of bottom</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code bottom-center}</td>
 * <td>bottom-center container, child of bottom</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code bottom-end}</td>
 * <td>bottom-end container, child of bottom</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * <tr>
 * <td>{@code bottom-stretch}</td>
 * <td>bottom-stretch container, child of bottom</td>
 * <td>vaadin-notification-overlay</td>
 * </tr>
 * </tbody>
 * </table>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.0-SNAPSHOT",
        "WebComponent: Vaadin.NotificationElement#1.0.0-alpha3",
        "Flow#1.0-SNAPSHOT" })
@Tag("vaadin-notification")
@HtmlImport("frontend://bower_components/vaadin-notification/vaadin-notification.html")
public class GeneratedVaadinNotification<R extends GeneratedVaadinNotification<R>>
        extends Component implements HasStyle, ComponentSupplier<R> {

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The duration in milliseconds to show the notification. Set to {@code 0}
     * or a negative number to disable the notification auto-closing.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code duration} property from the webcomponent
     */
    public double getDuration() {
        return getElement().getProperty("duration", 0.0);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The duration in milliseconds to show the notification. Set to {@code 0}
     * or a negative number to disable the notification auto-closing.
     * </p>
     * 
     * @param duration
     *            the double value to set
     */
    public void setDuration(double duration) {
        getElement().setProperty("duration", duration);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * True if the notification is currently displayed.
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
     * True if the notification is currently displayed.
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
     * Vertical alignment of the notification in the viewport Valid values are
     * {@code top-stretch|top|middle|bottom|bottom-stretch}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code verticalAlign} property from the webcomponent
     */
    public String getVerticalAlign() {
        return getElement().getProperty("verticalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Vertical alignment of the notification in the viewport Valid values are
     * {@code top-stretch|top|middle|bottom|bottom-stretch}
     * </p>
     * 
     * @param verticalAlign
     *            the String value to set
     */
    public void setVerticalAlign(String verticalAlign) {
        getElement().setProperty("verticalAlign",
                verticalAlign == null ? "" : verticalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Horizontal alignment of the notification in the viewport Only applies for
     * notifications in {@code top} or {@code bottom} containers. Valid values
     * are {@code start|center|end} Horizontal alignment is skipped in case
     * verticalAlign is set to {@code top-stretch|middle|bottom-stretch}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code horizontalAlign} property from the webcomponent
     */
    public String getHorizontalAlign() {
        return getElement().getProperty("horizontalAlign");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Horizontal alignment of the notification in the viewport Only applies for
     * notifications in {@code top} or {@code bottom} containers. Valid values
     * are {@code start|center|end} Horizontal alignment is skipped in case
     * verticalAlign is set to {@code top-stretch|middle|bottom-stretch}
     * </p>
     * 
     * @param horizontalAlign
     *            the String value to set
     */
    public void setHorizontalAlign(String horizontalAlign) {
        getElement().setProperty("horizontalAlign",
                horizontalAlign == null ? "" : horizontalAlign);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Opens the notification.
     * </p>
     */
    public void open() {
        getElement().callFunction("open");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Closes the notification.
     * </p>
     */
    public void close() {
        getElement().callFunction("close");
    }

    @DomEvent("opened-changed")
    public static class OpenedChangeEvent<R extends GeneratedVaadinNotification<R>>
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
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return addListener(OpenedChangeEvent.class,
                (ComponentEventListener) listener);
    }
}