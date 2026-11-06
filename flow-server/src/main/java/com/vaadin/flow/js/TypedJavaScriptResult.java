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
package com.vaadin.flow.js;

import java.util.Objects;

import tools.jackson.databind.JavaType;

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonCodec;

/**
 * A {@link JavaScriptResult} that converts the JSON value of an untyped result
 * to the type that a {@link JsDefinition} method declares. It subscribes to the
 * untyped result only when a handler is added, so a call whose result is not
 * read still has no result channel.
 * <p>
 * PROTOTYPE.
 */
record TypedJavaScriptResult<T>(PendingJavaScriptResult untyped,
        JavaType type) implements JavaScriptResult<T> {

    TypedJavaScriptResult {
        Objects.requireNonNull(untyped);
        Objects.requireNonNull(type);
    }

    @Override
    public boolean cancelExecution() {
        return untyped.cancelExecution();
    }

    @Override
    public boolean isSentToBrowser() {
        return untyped.isSentToBrowser();
    }

    @Override
    public void then(SerializableConsumer<T> resultHandler,
            SerializableConsumer<String> errorHandler) {
        Objects.requireNonNull(resultHandler, "Result handler cannot be null");
        untyped.then(
                json -> resultHandler.accept(JacksonCodec.decodeAs(json, type)),
                errorHandler);
    }
}
