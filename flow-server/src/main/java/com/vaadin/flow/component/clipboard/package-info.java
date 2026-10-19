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
 * Server-side access to the browser's clipboard — both writing (copy) and
 * reading (paste).
 * <p>
 * Use
 * {@link com.vaadin.flow.component.clipboard.Clipboard#onClick(com.vaadin.flow.component.Component)
 * Clipboard.onClick(component)} to copy text, an image, or custom content to
 * the clipboard when a component is clicked. Use
 * {@link com.vaadin.flow.component.clipboard.Clipboard#onPaste(com.vaadin.flow.component.Component, com.vaadin.flow.function.SerializableConsumer)
 * Clipboard.onPaste(...)} to react to text the user pastes onto a component,
 * and {@code Clipboard.onFilePaste(...)} to receive pasted files.
 * <p>
 * Copying goes through {@code onClick} rather than an ordinary server-side
 * click listener because the browser only grants clipboard write access while
 * it is handling a genuine user gesture, which is no longer valid by the time a
 * server round trip completes. Clipboard access also requires a secure context
 * (HTTPS or {@code localhost}).
 */
@NullMarked
package com.vaadin.flow.component.clipboard;

import org.jspecify.annotations.NullMarked;
