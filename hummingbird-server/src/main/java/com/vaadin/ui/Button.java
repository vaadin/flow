/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.ui;

import java.io.Serializable;
import java.util.EventListener;

import com.vaadin.annotations.Tag;
import com.vaadin.server.Resource;
import com.vaadin.shared.MouseEventDetails;
import com.vaadin.shared.ui.button.ButtonServerRpc;
import com.vaadin.shared.ui.button.ButtonState;

/**
 * A generic button component.
 *
 * @author Vaadin Ltd.
 * @since 3.0
 */
@SuppressWarnings("serial")
@Tag("button")
public class Button extends AbstractFocusable {

    private ButtonServerRpc rpc = new ButtonServerRpc() {

        @Override
        public void click(MouseEventDetails mouseEventDetails) {
            fireClick(mouseEventDetails);
        }

        @Override
        public void disableOnClick() throws RuntimeException {
            setEnabled(false);
            // Makes sure the enabled=false state is noticed at once - otherwise
            // a following setEnabled(true) call might have no effect. see
            // ticket #10030
            getUI().getConnectorTracker().getDiffState(Button.this)
                    .put("enabled", false);
        }
    };

    /**
     * Creates a new push button.
     */
    public Button() {
    }

    /**
     * Creates a new push button with the given caption.
     *
     * @param caption
     *            the Button caption.
     */
    public Button(String caption) {
        this();
        setCaption(caption);
    }

    @Override
    public void setCaption(String caption) {
        super.setCaption(caption);
        getElement().setTextContent(caption);
    }

    /**
     * Creates a new push button with the given icon.
     *
     * @param icon
     *            the icon
     */
    public Button(Resource icon) {
        this();
        setIcon(icon);
    }

    /**
     * Creates a new push button with the given caption and icon.
     *
     * @param caption
     *            the caption
     * @param icon
     *            the icon
     */
    public Button(String caption, Resource icon) {
        this();
        setCaption(caption);
        setIcon(icon);
    }

    /**
     * Creates a new push button with a click listener.
     *
     * @param caption
     *            the Button caption.
     * @param listener
     *            the Button click listener.
     */
    public Button(String caption, ClickListener listener) {
        this(caption);
        addClickListener(listener);
    }

    /**
     * Click event. This event is thrown, when the button is clicked.
     *
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public static class ClickEvent extends Component.Event {

        private final MouseEventDetails details;

        /**
         * New instance of text change event.
         *
         * @param source
         *            the Source of the event.
         */
        public ClickEvent(Component source) {
            super(source);
            details = null;
        }

        /**
         * Constructor with mouse details
         *
         * @param source
         *            The source where the click took place
         * @param details
         *            Details about the mouse click
         */
        public ClickEvent(Component source, MouseEventDetails details) {
            super(source);
            this.details = details;
        }

        /**
         * Gets the Button where the event occurred.
         *
         * @return the Source of the event.
         */
        public Button getButton() {
            return (Button) getSource();
        }

        /**
         * Returns the mouse position (x coordinate) when the click took place.
         * The position is relative to the browser client area.
         *
         * @return The mouse cursor x position or -1 if unknown
         */
        public int getClientX() {
            if (null != details) {
                return details.getClientX();
            } else {
                return -1;
            }
        }

        /**
         * Returns the mouse position (y coordinate) when the click took place.
         * The position is relative to the browser client area.
         *
         * @return The mouse cursor y position or -1 if unknown
         */
        public int getClientY() {
            if (null != details) {
                return details.getClientY();
            } else {
                return -1;
            }
        }

        /**
         * Returns the relative mouse position (x coordinate) when the click
         * took place. The position is relative to the clicked component.
         *
         * @return The mouse cursor x position relative to the clicked layout
         *         component or -1 if no x coordinate available
         */
        public int getRelativeX() {
            if (null != details) {
                return details.getRelativeX();
            } else {
                return -1;
            }
        }

        /**
         * Returns the relative mouse position (y coordinate) when the click
         * took place. The position is relative to the clicked component.
         *
         * @return The mouse cursor y position relative to the clicked layout
         *         component or -1 if no y coordinate available
         */
        public int getRelativeY() {
            if (null != details) {
                return details.getRelativeY();
            } else {
                return -1;
            }
        }

        /**
         * Checks if the Alt key was down when the mouse event took place.
         *
         * @return true if Alt was down when the event occured, false otherwise
         *         or if unknown
         */
        public boolean isAltKey() {
            if (null != details) {
                return details.isAltKey();
            } else {
                return false;
            }
        }

        /**
         * Checks if the Ctrl key was down when the mouse event took place.
         *
         * @return true if Ctrl was pressed when the event occured, false
         *         otherwise or if unknown
         */
        public boolean isCtrlKey() {
            if (null != details) {
                return details.isCtrlKey();
            } else {
                return false;
            }
        }

        /**
         * Checks if the Meta key was down when the mouse event took place.
         *
         * @return true if Meta was pressed when the event occured, false
         *         otherwise or if unknown
         */
        public boolean isMetaKey() {
            if (null != details) {
                return details.isMetaKey();
            } else {
                return false;
            }
        }

        /**
         * Checks if the Shift key was down when the mouse event took place.
         *
         * @return true if Shift was pressed when the event occured, false
         *         otherwise or if unknown
         */
        public boolean isShiftKey() {
            if (null != details) {
                return details.isShiftKey();
            } else {
                return false;
            }
        }
    }

    /**
     * Interface for listening for a {@link ClickEvent} fired by a
     * {@link Component}.
     *
     * @author Vaadin Ltd.
     * @since 3.0
     */
    public interface ClickListener extends EventListener, Serializable {

        /**
         * Called when a {@link Button} has been clicked. A reference to the
         * button is given by {@link ClickEvent#getButton()}.
         *
         * @param event
         *            An event containing information about the click.
         */
        public void buttonClick(ClickEvent event);
    }

    /**
     * Adds the button click listener.
     *
     * @param listener
     *            the Listener to be added.
     */
    public void addClickListener(ClickListener listener) {
        if (!hasListeners(ClickListener.class)) {
            getElement().addEventData("click",
                    MouseEventDetails.getEventProperties());
            getElement().addEventListener("click", e -> {
                fireClick(new MouseEventDetails(e));
            });
        }
        addListener(ClickListener.class, listener);
    }

    /**
     * Removes the button click listener.
     *
     * @param listener
     *            the Listener to be removed.
     */
    public void removeClickListener(ClickListener listener) {
        removeListener(ClickListener.class, listener);
    }

    /**
     * Simulates a button click, notifying all server-side listeners.
     *
     * No action is taken is the button is disabled.
     */
    public void click() {
        if (isEnabled() && !isReadOnly()) {
            fireClick();
        }
    }

    /**
     * Fires a click event to all listeners without any event details.
     *
     * In subclasses, override {@link #fireClick(MouseEventDetails)} instead of
     * this method.
     */
    protected void fireClick() {
        fireEvent(new Button.ClickEvent(this));
    }

    /**
     * Fires a click event to all listeners.
     *
     * @param details
     *            MouseEventDetails from which keyboard modifiers and other
     *            information about the mouse click can be obtained. If the
     *            button was clicked by a keyboard event, some of the fields may
     *            be empty/undefined.
     */
    protected void fireClick(MouseEventDetails details) {
        fireEvent(new Button.ClickEvent(this, details));
    }

    /**
     * Determines if a button is automatically disabled when clicked. See
     * {@link #setDisableOnClick(boolean)} for details.
     *
     * @return true if the button is disabled when clicked, false otherwise
     */
    public boolean isDisableOnClick() {
        return getState(false).disableOnClick;
    }

    /**
     * Determines if a button is automatically disabled when clicked. If this is
     * set to true the button will be automatically disabled when clicked,
     * typically to prevent (accidental) extra clicks on a button.
     * <p>
     * Note that this is only used when the click comes from the user, not when
     * calling {@link #click()}.
     * </p>
     *
     * @param disableOnClick
     *            true to disable button when it is clicked, false otherwise
     */
    public void setDisableOnClick(boolean disableOnClick) {
        getState().disableOnClick = disableOnClick;
    }

    @Override
    protected ButtonState getState() {
        return (ButtonState) super.getState();
    }

    @Override
    protected ButtonState getState(boolean markAsDirty) {
        return (ButtonState) super.getState(markAsDirty);
    }

    /**
     * Sets the component's icon and alt text.
     *
     * An alt text is shown when an image could not be loaded, and read by
     * assisitve devices.
     *
     * @param icon
     *            the icon to be shown with the component's caption.
     * @param iconAltText
     *            String to use as alt text
     */
    public void setIcon(Resource icon, String iconAltText) {
        super.setIcon(icon);
        getState().iconAltText = iconAltText == null ? "" : iconAltText;
    }

    /**
     * Returns the icon's alt text.
     *
     * @return String with the alt text
     */
    public String getIconAlternateText() {
        return getState(false).iconAltText;
    }

    public void setIconAlternateText(String iconAltText) {
        getState().iconAltText = iconAltText;
    }

    /**
     * Set whether the caption text is rendered as HTML or not. You might need
     * to re-theme button to allow higher content than the original text style.
     *
     * If set to true, the captions are passed to the browser as html and the
     * developer is responsible for ensuring no harmful html is used. If set to
     * false, the content is passed to the browser as plain text.
     *
     * @param htmlContentAllowed
     *            <code>true</code> if caption is rendered as HTML,
     *            <code>false</code> otherwise
     */
    public void setHtmlContentAllowed(boolean htmlContentAllowed) {
        getState().captionAsHtml = htmlContentAllowed;
    }

    /**
     * Return HTML rendering setting
     *
     * @return <code>true</code> if the caption text is to be rendered as HTML,
     *         <code>false</code> otherwise
     */
    public boolean isHtmlContentAllowed() {
        return getState(false).captionAsHtml;
    }

}
