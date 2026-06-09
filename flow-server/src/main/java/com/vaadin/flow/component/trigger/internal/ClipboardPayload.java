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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * Textual clipboard contents delivered to {@link ReadFromClipboardAction}'s
 * handler. Either field may be {@code null} if the corresponding MIME type was
 * not present on the clipboard item.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param text
 *            {@code text/plain} contents, or {@code null} if not present
 * @param html
 *            {@code text/html} contents, or {@code null} if not present
 */
public record ClipboardPayload(@Nullable String text,
        @Nullable String html) implements Serializable {
}
