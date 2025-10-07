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

package com.vaadin.flow.component;

/**
 * Test event with a bean parameter to verify @EventData works with complex
 * types, not just primitives.
 */
@DomEvent("bean-event")
public class EventWithBeanData extends ComponentEvent<Component> {

    /**
     * Test bean for event data.
     */
    public static class MouseDetails {
        private int clientX;
        private int clientY;
        private int button;

        public MouseDetails() {
        }

        public MouseDetails(int clientX, int clientY, int button) {
            this.clientX = clientX;
            this.clientY = clientY;
            this.button = button;
        }

        public int getClientX() {
            return clientX;
        }

        public void setClientX(int clientX) {
            this.clientX = clientX;
        }

        public int getClientY() {
            return clientY;
        }

        public void setClientY(int clientY) {
            this.clientY = clientY;
        }

        public int getButton() {
            return button;
        }

        public void setButton(int button) {
            this.button = button;
        }
    }

    private final MouseDetails details;

    public EventWithBeanData(Component source, boolean fromClient,
            @EventData("event.detail") MouseDetails details) {
        super(source, fromClient);
        this.details = details;
    }

    public MouseDetails getDetails() {
        return details;
    }
}
