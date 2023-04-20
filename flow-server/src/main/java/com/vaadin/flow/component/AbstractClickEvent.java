/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.component;

/**
 * Abstract super class event for e.g. DoubleClickEvent and SingleClickEvent.
 *
 * @author Vaadin Ltd
 * @since 24.1
 *
 * @param <C>
 *            The source component type
 */
public abstract class AbstractClickEvent<C extends Component>
        extends ComponentEvent<C> {
    private final int screenX;
    private final int screenY;

    private final int clientX;
    private final int clientY;

    private final int button;
    private final boolean ctrlKey;
    private final boolean shiftKey;
    private final boolean altKey;
    private final boolean metaKey;

    /**
     * Creates a new click event.
     *
     * @param source
     *            the component that fired the event
     * @param fromClient
     *            <code>true</code> if the event was originally fired on the
     *            client, <code>false</code> if the event originates from
     *            server-side logic
     * @param screenX
     *            the x coordinate of the click event, relative to the upper
     *            left corner of the screen, -1 if unknown
     * @param screenY
     *            the y coordinate of the click event, relative to the upper
     *            left corner of the screen, -i if unknown
     * @param clientX
     *            the x coordinate of the click event, relative to the upper
     *            left corner of the browser viewport, -1 if unknown
     * @param clientY
     *            the y coordinate of the click event, relative to the upper
     *            left corner of the browser viewport, -1 if unknown
     * @param button
     *            the id of the pressed mouse button
     * @param ctrlKey
     *            <code>true</code> if the control key was down when the event
     *            was fired, <code>false</code> otherwise
     * @param shiftKey
     *            <code>true</code> if the shift key was down when the event was
     *            fired, <code>false</code> otherwise
     * @param altKey
     *            <code>true</code> if the alt key was down when the event was
     *            fired, <code>false</code> otherwise
     * @param metaKey
     *            <code>true</code> if the meta key was down when the event was
     *            fired, <code>false</code> otherwise
     *
     */
    public AbstractClickEvent(Component source, boolean fromClient, int screenX,
            int screenY, int clientX, int clientY, int button, boolean ctrlKey,
            boolean shiftKey, boolean altKey, boolean metaKey) {
        super((C) source, fromClient);
        this.screenX = screenX;
        this.screenY = screenY;
        this.clientX = clientX;
        this.clientY = clientY;
        this.button = button;
        this.ctrlKey = ctrlKey;
        this.shiftKey = shiftKey;
        this.altKey = altKey;
        this.metaKey = metaKey;
    }

    /**
     * Gets the x coordinate of the click event, relative to the upper left
     * corner of the browser viewport.
     *
     * @return the x coordinate, -1 if unknown
     */
    public int getClientX() {
        return clientX;
    }

    /**
     * Gets the y coordinate of the click event, relative to the upper left
     * corner of the browser viewport.
     *
     * @return the y coordinate, -1 if unknown
     */
    public int getClientY() {
        return clientY;
    }

    /**
     * Gets the x coordinate of the click event, relative to the upper left
     * corner of the screen.
     *
     * @return the x coordinate, -1 if unknown
     */
    public int getScreenX() {
        return screenX;
    }

    /**
     * Gets the y coordinate of the click event, relative to the upper left
     * corner of the screen.
     *
     * @return the y coordinate, -1 if unknown
     */
    public int getScreenY() {
        return screenY;
    }

    /**
     * Gets the id of the pressed mouse button.
     * <ul>
     * <li>-1: No button
     * <li>0: The primary button, typically the left mouse button
     * <li>1: The middle button,
     * <li>2: The secondary button, typically the right mouse button
     * <li>3: The first additional button, typically the back button
     * <li>4: The second additional button, typically the forward button
     * <li>5+: More additional buttons without any typical meanings
     * </ul>
     *
     * @return the button id, or -1 if no button was pressed
     */
    public int getButton() {
        return button;
    }

    /**
     * Checks whether the ctrl key was was down when the event was fired.
     *
     * @return <code>true</code> if the ctrl key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isCtrlKey() {
        return ctrlKey;
    }

    /**
     * Checks whether the alt key was was down when the event was fired.
     *
     * @return <code>true</code> if the alt key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isAltKey() {
        return altKey;
    }

    /**
     * Checks whether the meta key was was down when the event was fired.
     *
     * @return <code>true</code> if the meta key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isMetaKey() {
        return metaKey;
    }

    /**
     * Checks whether the shift key was was down when the event was fired.
     *
     * @return <code>true</code> if the shift key was down when the event was
     *         fired, <code>false</code> otherwise
     */
    public boolean isShiftKey() {
        return shiftKey;
    }
}
