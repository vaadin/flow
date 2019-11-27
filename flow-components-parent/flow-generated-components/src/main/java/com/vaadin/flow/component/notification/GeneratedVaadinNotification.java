/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
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
 * {@code <vaadin-notification>} is a Web Component providing accessible and
 * customizable notifications (toasts). The content of the notification can be
 * populated in two ways: imperatively by using renderer callback function and
 * declaratively by using Polymer's Templates.
 * </p>
 * <h3>Rendering</h3>
 * <p>
 * By default, the notification uses the content provided by using the renderer
 * callback function.
 * </p>
 * <p>
 * The renderer function provides {@code root}, {@code notification} arguments.
 * Generate DOM content, append it to the {@code root} element and control the
 * state of the host element by accessing {@code notification}. Before
 * generating new content, users are able to check if there is already content
 * in {@code root} for reusing it.
 * </p>
 * <p>
 * &lt;vaadin-notification
 * id=&quot;notification&quot;&gt;&lt;/vaadin-notification&gt;
 * {@code const notification = document.querySelector('#notification');
 * notification.renderer = function(root) root.textContent = &quot;Your work has
 * been saved&quot;; };}
 * </p>
 * <p>
 * Renderer is called on the opening of the notification. DOM generated during
 * the renderer call can be reused in the next renderer call and will be
 * provided with the {@code root} argument. On first call it will be empty.
 * </p>
 * <h3>Polymer Templates</h3>
 * <p>
 * Alternatively, the content can be provided with Polymer's Template.
 * Notification finds the first child template and uses that in case renderer
 * callback function is not provided. You can also set a custom template using
 * the {@code template} property.
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
 * {@code <vaadin-notification>} uses {@code <vaadin-notification-card>}
 * internal themable component as the actual visible notification cards. See the
 * stylable parts the <a href=
 * "https://vaadin.com/components/vaadin-notification/html-api/elements/Vaadin.NotificationCard"
 * >{@code <vaadin-notification-card>} API</a>.
 * </p>
 * <p>
 * Note: the {@code theme} attribute value set on {@code <vaadin-notification>}
 * is propagated to the internal {@code <vaadin-notification-card>}.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.2-SNAPSHOT",
        "WebComponent: Vaadin.NotificationElement#1.2.0", "Flow#1.2-SNAPSHOT" })
@Tag("vaadin-notification")
@HtmlImport("frontend://bower_components/vaadin-notification/src/vaadin-notification.html")
public abstract class GeneratedVaadinNotification<R extends GeneratedVaadinNotification<R>>
        extends Component {

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
    protected double getDurationDouble() {
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
    protected void setDuration(double duration) {
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
    protected boolean isOpenedBoolean() {
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
    protected void setOpened(boolean opened) {
        getElement().setProperty("opened", opened);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Alignment of the notification in the viewport Valid values are
     * {@code top-stretch|top-start|top-center|top-end|middle|bottom-start|bottom-center|bottom-end|bottom-stretch}
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code position} property from the webcomponent
     */
    protected String getPositionString() {
        return getElement().getProperty("position");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Alignment of the notification in the viewport Valid values are
     * {@code top-stretch|top-start|top-center|top-end|middle|bottom-start|bottom-center|bottom-end|bottom-stretch}
     * </p>
     * 
     * @param position
     *            the String value to set
     */
    protected void setPosition(String position) {
        getElement().setProperty("position", position == null ? "" : position);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Manually invoke existing renderer.
     * </p>
     */
    protected void render() {
        getElement().callFunction("render");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Opens the notification.
     * </p>
     */
    protected void open() {
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
    protected void close() {
        getElement().callFunction("close");
    }

    public static class OpenedChangeEvent<R extends GeneratedVaadinNotification<R>>
            extends ComponentEvent<R> {
        private final boolean opened;

        public OpenedChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.opened = source.isOpenedBoolean();
        }

        public boolean isOpened() {
            return opened;
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
    protected Registration addOpenedChangeListener(
            ComponentEventListener<OpenedChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("opened",
                        event -> listener.onComponentEvent(
                                new OpenedChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}