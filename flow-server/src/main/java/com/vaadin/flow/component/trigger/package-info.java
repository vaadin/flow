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
/**
 * Server-side API for wiring client-side triggers (DOM events, shortcuts, …) to
 * client-side actions (clipboard copy, set property, run JS, …) reading values
 * from outputs, without a server round-trip when not needed.
 * <p>
 * The trigger API is intentionally open for extension: applications and add-ons
 * declare new trigger, action and output types by extending
 * {@link com.vaadin.flow.component.trigger.AbstractTrigger},
 * {@link com.vaadin.flow.component.trigger.AbstractAction} or
 * {@link com.vaadin.flow.component.trigger.AbstractOutput} and pairing them
 * with a JavaScript handler registered under the same type id against
 * {@code window.Vaadin.Flow.triggers}.
 */
@NullMarked
package com.vaadin.flow.component.trigger;

import org.jspecify.annotations.NullMarked;
