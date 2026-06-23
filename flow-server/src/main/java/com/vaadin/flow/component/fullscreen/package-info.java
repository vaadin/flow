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
 * Server-side access to the browser's Fullscreen API.
 * <p>
 * Use
 * {@link com.vaadin.flow.component.fullscreen.Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(component)} to make a button (or other clickable
 * component) enter fullscreen, and
 * {@link com.vaadin.flow.component.fullscreen.Fullscreen#stateSignal()} to
 * observe the current
 * {@link com.vaadin.flow.component.fullscreen.FullscreenState fullscreen
 * state}. {@link com.vaadin.flow.component.fullscreen.Fullscreen#exit()} leaves
 * fullscreen again.
 * <p>
 * Entering fullscreen goes through {@code onClick} rather than an ordinary
 * server-side click listener because the browser only honours a fullscreen
 * request while it is handling a genuine user gesture, which is no longer valid
 * by the time a server round trip completes.
 */
@NullMarked
package com.vaadin.flow.component.fullscreen;

import org.jspecify.annotations.NullMarked;
