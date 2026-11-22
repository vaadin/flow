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
package com.vaadin.flow.component.download;

/**
 * Where the browser puts the content of a {@link Download}.
 */
public enum DownloadTarget {

    /**
     * Saves the content as a file, as the browser does for any download.
     * Content from a handler is served as an attachment, unless the handler
     * calls {@link com.vaadin.flow.server.streams.DownloadEvent#inline()} or
     * {@link com.vaadin.flow.server.streams.DownloadEvent#inline(String)}
     * itself. Content from another URL of the same origin is saved even if it
     * is served inline. For a URL of another origin, the server of that URL
     * decides: content it serves inline may be shown instead of saved.
     */
    SAVE_AS_FILE,

    /**
     * Opens the content in a new browser tab. The browser shows the content
     * when it can, for example a PDF, an image or plain text, and saves it as a
     * file otherwise.
     * <p>
     * Content from a handler is served inline for this use only, unless the
     * handler calls
     * {@link com.vaadin.flow.server.streams.DownloadEvent#attachment(String)}
     * itself, see {@link com.vaadin.flow.server.streams.DownloadDisposition}.
     * The handler itself is not changed, so the same instance can also be used
     * where the content is downloaded.
     * <p>
     * The new tab is opened without access to the opening page
     * ({@code noopener}) and without a {@code Referer} header
     * ({@code noreferrer}).
     */
    NEW_TAB
}
