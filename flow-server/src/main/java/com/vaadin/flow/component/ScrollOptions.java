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
import java.util.Locale;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.internal.JacksonUtils;

/**
 * Options for scrollIntoView.
 * <p>
 * See https://developer.mozilla.org/en-US/docs/Web/API/Element/scrollIntoView
 *
 * @deprecated Use
 *             {@link com.vaadin.flow.dom.Element#scrollIntoView(ScrollIntoViewOption...)}
 *             with {@link ScrollIntoViewOption.Behavior},
 *             {@link ScrollIntoViewOption.Block}, and
 *             {@link ScrollIntoViewOption.Inline} options instead
 **/
@Deprecated(since = "25.0", forRemoval = true)
public class ScrollOptions implements Serializable {
    /**
     * Scroll behavior for scrollIntoView.
     */
    public enum Behavior {
        AUTO, SMOOTH;
    }

    /**
     * Alignment for scrollIntoView.
     */
    public enum Alignment {
        START, CENTER, END, NEAREST;
    }

    private Behavior behavior = Behavior.SMOOTH;
    private Alignment block = Alignment.START;
    private Alignment inline = Alignment.NEAREST;

    /** Create an instance with the default options. */
    public ScrollOptions() {
    }

    /**
     * Create an instance with the given scroll behavior.
     *
     * @param behavior
     *            the behavior
     */
    public ScrollOptions(Behavior behavior) {
        this.behavior = behavior;
    }

    /**
     * Create an instance with the given scroll options.
     *
     * @param behavior
     *            the behavior
     * @param block
     *            the vertical alignment
     * @param inline
     *            the horizontal alignment
     */
    public ScrollOptions(Behavior behavior, Alignment block, Alignment inline) {
        this.behavior = behavior;
        this.block = block;
        this.inline = inline;
    }

    /**
     * Sets the scroll behavior.
     *
     * @param behavior
     *            the behavior
     */
    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    /**
     * Gets the scroll behavior.
     *
     * @return the behavior
     */
    public Behavior getBehavior() {
        return behavior;
    }

    /**
     * Sets the vertical alignment.
     *
     * @param block
     *            the vertical alignment
     */
    public void setBlock(Alignment block) {
        this.block = block;
    }

    /**
     * Gets the vertical alignment.
     *
     * @return the vertical alignment
     */
    public Alignment getBlock() {
        return block;
    }

    /**
     * Sets the horizontal alignment.
     *
     * @param inline
     *            the horizontal alignment
     */
    public void setInline(Alignment inline) {
        this.inline = inline;
    }

    /**
     * Gets the horizontal alignment.
     *
     * @return the horizontal alignment
     */
    public Alignment getInline() {
        return inline;
    }

    /**
     * Convert to json in a form compatible with element.scrollIntoView.
     *
     * @return a json object as a string
     */
    public String toJson() {
        ObjectNode json = JacksonUtils.createObjectNode();
        if (behavior != Behavior.AUTO) {
            json.put("behavior", behavior.name().toLowerCase(Locale.ENGLISH));
        }
        if (block != Alignment.START) {
            json.put("block", block.name().toLowerCase(Locale.ENGLISH));
        }
        if (inline != Alignment.NEAREST) {
            json.put("inline", inline.name().toLowerCase(Locale.ENGLISH));
        }
        return json.toString();
    }
}
