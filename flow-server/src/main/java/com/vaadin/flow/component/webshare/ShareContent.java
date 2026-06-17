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
package com.vaadin.flow.component.webshare;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.LiteralInput;
import com.vaadin.flow.component.trigger.internal.PropertyInput;

/**
 * Payload for {@link WebShareBinding#share}. Any combination of {@code title},
 * {@code text}, and {@code url} can be set; the Web Share API requires at least
 * one of them to be present.
 * <p>
 * Use the static factory:
 *
 * <pre>{@code
 * WebShare.onClick(button).share(ShareContent.create().title("Hello")
 *         .text("World").url("https://vaadin.com"));
 * }</pre>
 */
public final class ShareContent implements Serializable {

    private Action.@Nullable Input<String> titleInput;
    private Action.@Nullable Input<String> textInput;
    private Action.@Nullable Input<String> urlInput;

    private ShareContent() {
    }

    /**
     * Creates a new empty content builder.
     *
     * @return a new builder
     */
    public static ShareContent create() {
        return new ShareContent();
    }

    /**
     * Sets the title of the share payload.
     *
     * @param literal
     *            the value, not {@code null}
     * @return this builder
     */
    public ShareContent title(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        this.titleInput = new LiteralInput<>(literal);
        return this;
    }

    /**
     * Sets the title of the share payload, taken from the {@code value}
     * property of the given component (typically an input field). The value is
     * read on the client when the trigger fires.
     *
     * @param source
     *            the component whose {@code value} property should be read, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     * @return this builder
     */
    public <C extends Component & HasValue<?, String>> ShareContent title(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        this.titleInput = new PropertyInput<>(source, "value", String.class);
        return this;
    }

    /**
     * Sets the free-form text of the share payload.
     *
     * @param literal
     *            the value, not {@code null}
     * @return this builder
     */
    public ShareContent text(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        this.textInput = new LiteralInput<>(literal);
        return this;
    }

    /**
     * Sets the free-form text of the share payload, taken from the
     * {@code value} property of the given component. The value is read on the
     * client when the trigger fires.
     *
     * @param source
     *            the component whose {@code value} property should be read, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     * @return this builder
     */
    public <C extends Component & HasValue<?, String>> ShareContent text(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        this.textInput = new PropertyInput<>(source, "value", String.class);
        return this;
    }

    /**
     * Sets the URL of the share payload.
     *
     * @param literal
     *            the value, not {@code null}
     * @return this builder
     */
    public ShareContent url(String literal) {
        Objects.requireNonNull(literal, "literal must not be null");
        this.urlInput = new LiteralInput<>(literal);
        return this;
    }

    /**
     * Sets the URL of the share payload, taken from the {@code value} property
     * of the given component. The value is read on the client when the trigger
     * fires.
     *
     * @param source
     *            the component whose {@code value} property should be read, not
     *            {@code null}
     * @param <C>
     *            component type implementing {@code HasValue<?, String>}
     * @return this builder
     */
    public <C extends Component & HasValue<?, String>> ShareContent url(
            C source) {
        Objects.requireNonNull(source, "source must not be null");
        this.urlInput = new PropertyInput<>(source, "value", String.class);
        return this;
    }

    Action.@Nullable Input<String> getTitleInput() {
        return titleInput;
    }

    Action.@Nullable Input<String> getTextInput() {
        return textInput;
    }

    Action.@Nullable Input<String> getUrlInput() {
        return urlInput;
    }
}
