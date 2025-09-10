/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.component.login;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.shared.SlotUtils;
import com.vaadin.flow.component.shared.internal.OverlayAutoAddController;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.Style;

/**
 * Server-side component for the {@code <vaadin-login-overlay>} component.
 *
 * On {@link LoginForm.LoginEvent} component becomes disabled. Disabled
 * component stops to process login events, however the
 * {@link LoginForm.ForgotPasswordEvent} event is processed anyway. To enable
 * use the {@link com.vaadin.flow.component.HasEnabled#setEnabled(boolean)}
 * method. Setting error {@link #setError(boolean)} true makes component
 * automatically enabled for the next login attempt.
 *
 * @author Vaadin Ltd
 */
@Tag("vaadin-login-overlay")
@NpmPackage(value = "@vaadin/login", version = "25.0.0-alpha18")
@JsModule("@vaadin/login/src/vaadin-login-overlay.js")
public class LoginOverlay extends AbstractLogin implements HasStyle {

    private Component title;
    private LoginOverlayFooter footer;
    private LoginOverlayCustomFormArea customFormArea;

    public LoginOverlay() {
        init();
    }

    public LoginOverlay(LoginI18n i18n) {
        super(i18n);
        init();
    }

    private void init() {
        // Initialize auto-add behavior
        OverlayAutoAddController<LoginOverlay> autoAddController = new OverlayAutoAddController<>(
                this);
        // Skip auto-adding when navigating to a new view before opening.
        // Handles cases where LoginOverlay is used in a login view, in which
        // case it should not be auto-added if the view redirects to a different
        // view if the user is already authenticated
        autoAddController.setSkipOnNavigation(true);
    }

    /**
     * Closes the login overlay.
     * <p>
     * This automatically removes the overlay from the {@link UI}, unless it was
     * manually added to a parent component.
     */
    public void close() {
        setOpened(false);
    }

    @Synchronize(property = "opened", value = "opened-changed")
    public boolean isOpened() {
        return getElement().getProperty("opened", false);
    }

    /**
     * Opens or closes the login overlay. Opening the overlay automatically
     * enables it in case it was disabled.
     * <p>
     * If an overlay was not added manually to a parent component, it will be
     * automatically added to the {@link UI} when opened, and automatically
     * removed from the UI when closed. Note that the overlay is then scoped to
     * the UI, and not the current view. As such, when navigating away from a
     * view, the overlay will still be opened or stay open. In order to close
     * the overlay when navigating away from a view, it should either be
     * explicitly added as a child to the view, or it should be explicitly
     * closed when leaving the view.
     *
     * @param opened
     *            {@code true} to open the login overlay, {@code false} to close
     *            it
     */
    public void setOpened(boolean opened) {
        if (opened) {
            setEnabled(true);
        }
        getElement().setProperty("opened", opened);
    }

    /**
     * Sets the application title. Detaches the component title if it was set
     * earlier. Note: the method calls {@link #setTitle(Component)}, which will
     * reset the custom title, if it was set.
     *
     * Title is a part of the I18n object. See {@link #setI18n(LoginI18n)}.
     *
     * @see #getTitleAsText()
     */
    public void setTitle(String title) {
        setTitle((Component) null);
        getElement().setProperty("title", title);
    }

    /**
     * Returns the value of the title property or a text content of the title if
     * it was set via {@link #setTitle(Component)}
     *
     * @return the string value of title
     */
    @Synchronize(property = "title", value = "title-changed")
    public String getTitleAsText() {
        if (title != null) {
            return title.getElement().getText();
        }
        return getElement().getProperty("title");
    }

    /**
     * Sets the application title, <code>null</code> to remove any previous
     * title and to display title set via {@link #setTitle(String)}.
     *
     * @see #getTitle()
     * @param title
     *            the title component to set, or <code>null</code> to remove any
     *            previously set title
     */
    public void setTitle(Component title) {
        if (this.title != null) {
            this.title.getElement().removeFromParent();
        }

        this.title = title;
        if (title == null) {
            return;
        }

        SlotUtils.addToSlot(this, "title", title);
    }

    /**
     * Returns custom title component which was set via
     * {@link #setTitle(Component)}
     *
     * @return the title component, <code>null</code> if nothing was set
     */
    public Component getTitle() {
        return title;
    }

    /**
     * Sets the application description.
     *
     * Description is a part of I18n object. See {@link #setI18n(LoginI18n)}.
     *
     * @see #getDescription()
     * @param description
     *            the description string
     */
    public void setDescription(String description) {
        getElement().setProperty("description", description);
    }

    /**
     * @return the value of description property
     */
    @Synchronize(property = "description", value = "description-changed")
    public String getDescription() {
        return getElement().getProperty("description");
    }

    /**
     * Gets the object from which components can be added or removed from the
     * overlay custom form area. This area is displayed only if there's at least
     * one component added with {@link LoginOverlayContent#add(Component...)}.
     *
     * Fields that are part of custom form area are not automatically submitted
     * as part of the {@link LoginForm.LoginEvent}, and are not supported when
     * setting {@code action} as their values will not be part of the login
     * request.
     *
     * @since 24.2
     * @return the custom form area object
     */
    public LoginOverlayCustomFormArea getCustomFormArea() {
        if (this.customFormArea == null) {
            this.customFormArea = new LoginOverlayCustomFormArea(this);
        }
        return this.customFormArea;
    }

    /**
     * Gets the object from which components can be added or removed from the
     * overlay footer area. This area is displayed only if there's at least one
     * component added with {@link LoginOverlayContent#add(Component...)}.
     *
     * @since 24.2
     * @return the footer object
     */
    public LoginOverlayFooter getFooter() {
        if (this.footer == null) {
            this.footer = new LoginOverlayFooter(this);
        }
        return this.footer;
    }

    /**
     * Class for adding and removing components to the custom form area of the
     * overlay.
     */
    final public static class LoginOverlayCustomFormArea
            extends LoginOverlayContent {
        private LoginOverlayCustomFormArea(LoginOverlay overlay) {
            super("custom-form-area", overlay);
        }
    }

    /**
     * Class for adding and removing components to the footer area of the
     * overlay.
     */
    final public static class LoginOverlayFooter extends LoginOverlayContent {
        private LoginOverlayFooter(LoginOverlay overlay) {
            super("footer", overlay);
        }
    }

    /**
     * This class defines the common behavior for adding/removing components to
     * the custom form area and footer parts.
     */
    abstract static class LoginOverlayContent implements Serializable {
        private final LoginOverlay overlay;
        private final String slot;

        protected LoginOverlayContent(String slot, LoginOverlay overlay) {
            this.slot = slot;
            this.overlay = overlay;
        }

        /**
         * Adds the given components to the container.
         *
         * @param components
         *            the components to be added.
         */
        public void add(Component... components) {
            Objects.requireNonNull(components, "Components should not be null");
            for (Component component : components) {
                Objects.requireNonNull(component,
                        "Component to add cannot be null");
                SlotUtils.addToSlot(overlay, slot, component);
            }
        }

        /**
         * Removes the given components from the container.
         *
         * @param components
         *            the components to be removed.
         */
        public void remove(Component... components) {
            Objects.requireNonNull(components, "Components should not be null");
            for (Component component : components) {
                Objects.requireNonNull(component,
                        "Component to remove cannot be null");
                Element element = component.getElement();

                if (overlay.getElement().equals(element.getParent())
                        && Objects.equals(element.getAttribute("slot"), slot)) {
                    element.removeAttribute("slot");
                    overlay.getElement().removeChild(element);
                }
            }
        }

        /**
         * Removes all components from the container.
         */
        public void removeAll() {
            SlotUtils.clearSlot(overlay, slot);
        }
    }

    /**
     * @throws UnsupportedOperationException
     *             LoginOverlay does not support adding styles to overlay
     *             wrapper
     */
    @Override
    public Style getStyle() {
        throw new UnsupportedOperationException(
                "LoginOverlay does not support adding styles to overlay wrapper");
    }
}
