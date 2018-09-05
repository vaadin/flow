/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.component.dialog;

import javax.annotation.Generated;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import elemental.json.JsonObject;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.component.Component;

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
 * "https://github.com/vaadin/vaadin-overlay/blob/master/src/vaadin-overlay.html"
 * >{@code <vaadin-overlay>} documentation</a> for
 * {@code <vaadin-dialog-overlay>} parts.
 * </p>
 */
@Generated({ "Generator: com.vaadin.generator.ComponentGenerator#1.1-SNAPSHOT",
        "WebComponent: Vaadin.DialogOverlayElement#UNKNOWN",
        "Flow#1.1-SNAPSHOT" })
@Tag("vaadin-dialog-overlay")
@HtmlImport("frontend://bower_components/vaadin-dialog/src/vaadin-dialog.html")
public abstract class GeneratedVaadinDialogOverlay<R extends GeneratedVaadinDialogOverlay<R>>
        extends Component implements HasStyle {

    /**
     * This property is synchronized automatically from client side when a
     * 'opened-changed' event happens.
     * 
     * @return the {@code opened} property from the webcomponent
     */
    @Synchronize(property = "opened", value = "opened-changed")
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
     * The template of the overlay content.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'template-changed' event happens.
     * </p>
     * 
     * @return the {@code template} property from the webcomponent
     */
    @Synchronize(property = "template", value = "template-changed")
    protected JsonObject getTemplateJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("template");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * The template of the overlay content.
     * </p>
     * 
     * @param template
     *            the JsonObject value to set
     */
    protected void setTemplate(JsonObject template) {
        getElement().setPropertyJson("template", template);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Optional argument for {@code Polymer.Templatize.templatize}.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code instanceProps} property from the webcomponent
     */
    protected JsonObject getInstancePropsJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("instanceProps");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * Optional argument for {@code Polymer.Templatize.templatize}.
     * </p>
     * 
     * @param instanceProps
     *            the JsonObject value to set
     */
    protected void setInstanceProps(JsonObject instanceProps) {
        getElement().setPropertyJson("instanceProps", instanceProps);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * References the content container after the template is stamped.
     * <p>
     * This property is synchronized automatically from client side when a
     * 'content-changed' event happens.
     * </p>
     * 
     * @return the {@code content} property from the webcomponent
     */
    @Synchronize(property = "content", value = "content-changed")
    protected JsonObject getContentJsonObject() {
        return (JsonObject) getElement().getPropertyRaw("content");
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * References the content container after the template is stamped.
     * </p>
     * 
     * @param content
     *            the JsonObject value to set
     */
    protected void setContent(JsonObject content) {
        getElement().setPropertyJson("content", content);
    }

    /**
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * 
     * @return the {@code withBackdrop} property from the webcomponent
     */
    protected boolean isWithBackdropBoolean() {
        return getElement().getProperty("withBackdrop", false);
    }

    /**
     * @param withBackdrop
     *            the boolean value to set
     */
    protected void setWithBackdrop(boolean withBackdrop) {
        getElement().setProperty("withBackdrop", withBackdrop);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true the overlay won't disable the main content, showing it doesn’t
     * change the functionality of the user interface.
     * <p>
     * This property is not synchronized automatically from the client side, so
     * the returned value may not be the same as in client side.
     * </p>
     * 
     * @return the {@code modeless} property from the webcomponent
     */
    protected boolean isModelessBoolean() {
        return getElement().getProperty("modeless", false);
    }

    /**
     * <p>
     * Description copied from corresponding location in WebComponent:
     * </p>
     * <p>
     * When true the overlay won't disable the main content, showing it doesn’t
     * change the functionality of the user interface.
     * </p>
     * 
     * @param modeless
     *            the boolean value to set
     */
    protected void setModeless(boolean modeless) {
        getElement().setProperty("modeless", modeless);
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
    protected boolean isFocusTrapBoolean() {
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
    protected void setFocusTrap(boolean focusTrap) {
        getElement().setProperty("focusTrap", focusTrap);
    }

    protected void updateStyles() {
        getElement().callFunction("updateStyles");
    }

    /**
     * @param sourceEvent
     *            Missing documentation!
     */
    protected void close(JsonObject sourceEvent) {
        getElement().callFunction("close", sourceEvent);
    }

    @DomEvent("vaadin-overlay-close")
    public static class VaadinOverlayCloseEvent<R extends GeneratedVaadinDialogOverlay<R>>
            extends ComponentEvent<R> {
        public VaadinOverlayCloseEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vaadin-overlay-close} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addVaadinOverlayCloseListener(
            ComponentEventListener<VaadinOverlayCloseEvent<R>> listener) {
        return addListener(VaadinOverlayCloseEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("vaadin-overlay-escape-press")
    public static class VaadinOverlayEscapePressEvent<R extends GeneratedVaadinDialogOverlay<R>>
            extends ComponentEvent<R> {
        public VaadinOverlayEscapePressEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vaadin-overlay-escape-press} events fired by
     * the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addVaadinOverlayEscapePressListener(
            ComponentEventListener<VaadinOverlayEscapePressEvent<R>> listener) {
        return addListener(VaadinOverlayEscapePressEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("vaadin-overlay-open")
    public static class VaadinOverlayOpenEvent<R extends GeneratedVaadinDialogOverlay<R>>
            extends ComponentEvent<R> {
        public VaadinOverlayOpenEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vaadin-overlay-open} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addVaadinOverlayOpenListener(
            ComponentEventListener<VaadinOverlayOpenEvent<R>> listener) {
        return addListener(VaadinOverlayOpenEvent.class,
                (ComponentEventListener) listener);
    }

    @DomEvent("vaadin-overlay-outside-click")
    public static class VaadinOverlayOutsideClickEvent<R extends GeneratedVaadinDialogOverlay<R>>
            extends ComponentEvent<R> {
        public VaadinOverlayOutsideClickEvent(R source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    /**
     * Adds a listener for {@code vaadin-overlay-outside-click} events fired by
     * the webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Registration addVaadinOverlayOutsideClickListener(
            ComponentEventListener<VaadinOverlayOutsideClickEvent<R>> listener) {
        return addListener(VaadinOverlayOutsideClickEvent.class,
                (ComponentEventListener) listener);
    }

    public static class OpenedChangeEvent<R extends GeneratedVaadinDialogOverlay<R>>
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

    public static class TemplateChangeEvent<R extends GeneratedVaadinDialogOverlay<R>>
            extends ComponentEvent<R> {
        private final JsonObject template;

        public TemplateChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.template = source.getTemplateJsonObject();
        }

        public JsonObject getTemplate() {
            return template;
        }
    }

    /**
     * Adds a listener for {@code template-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addTemplateChangeListener(
            ComponentEventListener<TemplateChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("template",
                        event -> listener.onComponentEvent(
                                new TemplateChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }

    public static class ContentChangeEvent<R extends GeneratedVaadinDialogOverlay<R>>
            extends ComponentEvent<R> {
        private final JsonObject content;

        public ContentChangeEvent(R source, boolean fromClient) {
            super(source, fromClient);
            this.content = source.getContentJsonObject();
        }

        public JsonObject getContent() {
            return content;
        }
    }

    /**
     * Adds a listener for {@code content-changed} events fired by the
     * webcomponent.
     * 
     * @param listener
     *            the listener
     * @return a {@link Registration} for removing the event listener
     */
    protected Registration addContentChangeListener(
            ComponentEventListener<ContentChangeEvent<R>> listener) {
        return getElement()
                .addPropertyChangeListener("content",
                        event -> listener.onComponentEvent(
                                new ContentChangeEvent<R>((R) this,
                                        event.isUserOriginated())));
    }
}