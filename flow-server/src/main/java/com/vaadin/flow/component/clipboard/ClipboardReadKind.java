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
package com.vaadin.flow.component.clipboard;

/**
 * Which slice of a clipboard read a {@link ClipboardBinding} read action asks
 * for. Recorded on the {@link ClipboardClient.ReadHandle} so a test driver can
 * tell what the application wanted without re-deriving it from the delivered
 * payload — the production read always fetches the full
 * {@link ClipboardPayload} regardless of kind, and the binding extracts the
 * requested field on delivery.
 *
 * @since 25.2
 */
public enum ClipboardReadKind {

    /** The full {@link ClipboardPayload} ({@link ClipboardBinding#read}). */
    READ,

    /**
     * Only the {@code text/plain} field ({@link ClipboardBinding#readText}).
     */
    READ_TEXT,

    /** Only the {@code text/html} field ({@link ClipboardBinding#readHtml}). */
    READ_HTML
}
