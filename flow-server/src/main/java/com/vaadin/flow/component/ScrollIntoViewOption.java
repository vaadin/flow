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

import java.io.Serializable;

/**
 * Marker interface for scrollIntoView options.
 * <p>
 * Implementations of this interface can be passed to
 * {@link com.vaadin.flow.dom.Element#scrollIntoView(ScrollIntoViewOption...)}
 * and {@link Component#scrollIntoView(ScrollIntoViewOption...)}.
 * <p>
 * See <a href=
 * "https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView">Element.scrollIntoView()</a>
 * for more information.
 */
public interface ScrollIntoViewOption extends Serializable {

    /**
     * Scroll behavior option for scrollIntoView operations.
     * <p>
     * Controls whether scrolling is instant or smooth.
     */
    enum Behavior implements ScrollIntoViewOption {
        /**
         * Scrolling happens instantly in a single jump.
         */
        AUTO,

        /**
         * Scrolling is animated smoothly.
         */
        SMOOTH
    }

    /**
     * Vertical alignment option for scrollIntoView operations.
     * <p>
     * Defines the vertical alignment of the element within the visible area.
     */
    class Block implements ScrollIntoViewOption {
        /**
         * Aligns the element to the top of the scrolling area.
         */
        public static final Block START = new Block("start");

        /**
         * Aligns the element to the center of the scrolling area.
         */
        public static final Block CENTER = new Block("center");

        /**
         * Aligns the element to the bottom of the scrolling area.
         */
        public static final Block END = new Block("end");

        /**
         * Aligns the element to the nearest edge of the scrolling area.
         */
        public static final Block NEAREST = new Block("nearest");

        private final String value;

        private Block(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Horizontal alignment option for scrollIntoView operations.
     * <p>
     * Defines the horizontal alignment of the element within the visible area.
     */
    class Inline implements ScrollIntoViewOption {
        /**
         * Aligns the element to the left of the scrolling area.
         */
        public static final Inline START = new Inline("start");

        /**
         * Aligns the element to the center of the scrolling area.
         */
        public static final Inline CENTER = new Inline("center");

        /**
         * Aligns the element to the right of the scrolling area.
         */
        public static final Inline END = new Inline("end");

        /**
         * Aligns the element to the nearest edge of the scrolling area.
         */
        public static final Inline NEAREST = new Inline("nearest");

        private final String value;

        private Inline(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
