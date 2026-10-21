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
 * Triggers that fire on the client and the actions they run, such as a
 * {@link com.vaadin.flow.component.trigger.ClickTrigger} running a
 * {@link com.vaadin.flow.component.trigger.DownloadAction}.
 * <p>
 * Creating triggers and actions and wiring them together is public API. Passing
 * data from a trigger or from the page into an action, and implementing custom
 * triggers and actions, is not yet: the input types live in the internal
 * package, and the methods that render triggers and actions to JavaScript are
 * for internal use only. Both may be changed or removed in any release.
 */
@NullMarked
package com.vaadin.flow.component.trigger;

import org.jspecify.annotations.NullMarked;
