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
 * Actions that run in the browser when something fires them, and the values
 * they read when they do.
 * <p>
 * Most applications never touch this package: they bind an action to a
 * component through a feature facade ({@code Clipboard.onClick(button)},
 * {@code Fullscreen.onClick(button)}). The types here are for the case where
 * the thing that fires the action is rendered on the client and has no
 * server-side component of its own — a {@code LitRenderer} row template, for
 * example. See {@link com.vaadin.flow.component.trigger.ClientAction}.
 */
package com.vaadin.flow.component.trigger;
