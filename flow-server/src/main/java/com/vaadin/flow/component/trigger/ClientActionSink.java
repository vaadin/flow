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

import java.io.Serializable;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Receives the client-side function a {@link ClientAction} renders to, and is
 * responsible for getting it invoked in the browser.
 * <p>
 * Implemented by whatever accepts client actions — typically a renderer that
 * passes the function into its own client-side template so a {@code @click}
 * binding can call it. The function takes {@code (event, context)}: the client
 * event that fired it, and an object describing what it fired for. A row
 * renderer supplies {@code {item, index, key}} for the row the event came from;
 * that context is what {@link ClientValue#itemProperty(String)} reads.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@FunctionalInterface
public interface ClientActionSink extends Serializable {

    /**
     * Takes the rendered action function and arranges for it to be called on
     * the client.
     *
     * @param action
     *            the action function, to be invoked as
     *            {@code action(event, context)}, not {@code null}
     * @return a registration that undoes the installation, never {@code null}
     */
    Registration install(JsFunction action);
}
