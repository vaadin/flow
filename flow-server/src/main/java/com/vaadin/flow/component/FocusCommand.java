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
package com.vaadin.flow.component;

import java.util.List;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsCommand;

/**
 * The command that {@link Focusable#focus(FocusOption...)} schedules: focus the
 * element the invocation is scheduled on, with the given options.
 * <p>
 * The focus is server-initiated, which the generated script marks for the
 * client so that the resulting {@link FocusNotifier.FocusEvent} reports
 * {@code isFromClient() == false}. A driver that acts on this command instead
 * of running the script is responsible for the same.
 *
 * @param options
 *            the options passed to {@link Focusable#focus(FocusOption...)}, in
 *            the order they were given; only the last {@link FocusOption} of
 *            each kind reaches the browser
 * @see BlurCommand
 */
public record FocusCommand(List<FocusOption> options) implements JsCommand {

    private static final String FOCUS_SCRIPT = """
            setTimeout(() => {
                try {
                   this._nextFocusIsFromClient = false;
                   this.focus();
                } finally {
                   this._nextFocusIsFromClient = true;
                }
            }, 0)
            """;

    private static final String FOCUS_WITH_OPTIONS_SCRIPT = """
            setTimeout(() => {
                try {
                   this._nextFocusIsFromClient = false;
                   this.focus($0);
                } finally {
                   this._nextFocusIsFromClient = true;
                }
            }, 0)
            """;

    /**
     * Creates a focus command with the given options.
     *
     * @param options
     *            the focus options, not <code>null</code> and with no
     *            <code>null</code> elements
     */
    public FocusCommand {
        options = List.copyOf(options);
    }

    /**
     * Creates a focus command with the given options.
     *
     * @param options
     *            zero or more focus options, with no <code>null</code> elements
     */
    public FocusCommand(FocusOption... options) {
        this(List.of(options));
    }

    @Override
    public String getExpression() {
        return optionsJson() == null ? FOCUS_SCRIPT : FOCUS_WITH_OPTIONS_SCRIPT;
    }

    @Override
    public List<Object> getParameters() {
        ObjectNode json = optionsJson();
        return json == null ? List.of() : List.of(json);
    }

    /**
     * The options as the browser receives them, or <code>null</code> when every
     * option is at its default and {@link Element#focus()} is called without
     * arguments.
     */
    private @Nullable ObjectNode optionsJson() {
        return FocusOption.buildOptions(options.toArray(new FocusOption[0]));
    }
}
