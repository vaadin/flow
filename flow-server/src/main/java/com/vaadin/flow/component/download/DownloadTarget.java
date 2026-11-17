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
     * Content from a handler or another URL of the same origin is saved even if
     * it is served inline, for example by a handler on which
     * {@link com.vaadin.flow.server.streams.AbstractDownloadHandler#inline()
     * inline()} was called. For a URL of another origin, the server of that URL
     * decides: content it serves inline may be shown instead of saved.
     */
    SAVE,

    /**
     * Opens the content in a new browser tab. The browser shows the content
     * when it can, for example a PDF, an image or plain text, and saves it as a
     * file otherwise.
     * <p>
     * The new tab is opened without access to the opening page
     * ({@code noopener}) and without a {@code Referer} header
     * ({@code noreferrer}).
     */
    NEW_TAB
}
