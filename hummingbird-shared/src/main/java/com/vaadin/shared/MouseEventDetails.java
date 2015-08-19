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
package com.vaadin.shared;

import java.io.Serializable;

import elemental.json.JsonObject;

/**
 * Helper class to store and transfer mouse event details.
 */
public class MouseEventDetails implements Serializable {

    /**
     * Constants for mouse buttons.
     *
     * @author Vaadin Ltd
     * @version @VERSION@
     * @since 7.0
     *
     */
    public enum MouseButton {
        LEFT("left"), RIGHT("right"), MIDDLE("middle");

        private String name;

        private MouseButton(String name) {
            this.name = name;
        }

        /**
         * Returns a human readable text representing the button
         *
         * @return
         */
        public String getName() {
            return name;
        }

    }

    // private static final char DELIM = ',';
    // From com.google.gwt.user.client.Event
    private static final int ONDBLCLICK = 0x00002;

    private JsonObject json;

    public MouseEventDetails(JsonObject json) {
        this.json = json;
    }

    public MouseButton getButton() {
        return MouseButton.valueOf(json.getString("event.button"));
    }

    public int getClientX() {
        return (int) json.getNumber("event.clientX");
    }

    public int getClientY() {
        return (int) json.getNumber("event.clientY");
    }

    public boolean isAltKey() {
        return json.getBoolean("event.altKey");
    }

    public boolean isCtrlKey() {
        return json.getBoolean("event.ctrlKey");
    }

    public boolean isMetaKey() {
        return json.getBoolean("event.metaKey");
    }

    public boolean isShiftKey() {
        return json.getBoolean("event.shiftKey");
    }

    public int getRelativeX() {
        return (int) json.getNumber("event.relativeX");
    }

    public int getRelativeY() {
        return (int) json.getNumber("event.relativeY");
    }

    @Override
    public String toString() {
        return json.toString();
    }

    public String getButtonName() {
        return getButton() == null ? "" : getButton().getName();
    }

    public int getType() {
        return (int) json.getNumber("event.type");
    }

    public boolean isDoubleClick() {
        return getType() == ONDBLCLICK;
    }

    public static String[] getEventProperties() {
        return new String[] { "event.button", "event.clientX", "event.clientY",
                "event.type", "event.altKey", "event.metaKey", "event.ctrlKey",
                "event.shiftKey", "event.relativeX", "event.relativeY" };
    }
}
