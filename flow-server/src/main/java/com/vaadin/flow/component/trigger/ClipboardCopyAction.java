/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.component.trigger;

import java.util.Objects;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.trigger.internal.ConfigContext;
import com.vaadin.flow.internal.JacksonUtils;

/**
 * Copies a value to the user's clipboard via
 * {@code navigator.clipboard.writeText}.
 * <p>
 * The Clipboard API requires the call to happen inside a short-lived user
 * gesture (click, key press, …). Bind this action to a {@link Trigger} that
 * fires during such a gesture, e.g. {@link ClickTrigger}.
 *
 * <pre>{@code
 * Argument<String> value = new PropertyArgument<>(textField, "value",
 *         String.class);
 * new ClickTrigger(button).triggers(new ClipboardCopyAction(value));
 * }</pre>
 */
public class ClipboardCopyAction extends AbstractAction {

    public static final String TYPE_ID = "flow:clipboard-copy";

    private final Argument<String> textArgument;

    /**
     * Creates a clipboard-copy action that copies the value produced by the
     * given argument.
     *
     * @param textArgument
     *            the argument supplying the text to copy, not {@code null}
     */
    public ClipboardCopyAction(Argument<String> textArgument) {
        super(TYPE_ID);
        this.textArgument = Objects.requireNonNull(textArgument);
    }

    /**
     * @return the argument supplying the text
     */
    public Argument<String> getTextArgument() {
        return textArgument;
    }

    @Override
    public ObjectNode buildClientConfig(ConfigContext context) {
        ObjectNode node = JacksonUtils.createObjectNode();
        node.put("text", context.registerArgument(textArgument));
        return node;
    }
}
