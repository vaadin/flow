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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Represents a clipboard event (paste, copy, or cut) received from the browser.
 * <p>
 * A clipboard event may contain text, HTML content, files, or a combination of
 * these.
 */
public class ClipboardEvent implements Serializable {

    private final String type;
    private final String text;
    private final String html;
    private final List<ClipboardFile> files;

    /**
     * Creates a new clipboard event.
     *
     * @param type
     *            the event type ("paste", "copy", or "cut")
     * @param text
     *            the plain text content, or {@code null} if none
     * @param html
     *            the HTML content, or {@code null} if none
     * @param files
     *            the list of pasted files, or an empty list if none
     */
    ClipboardEvent(String type, String text, String html,
            List<ClipboardFile> files) {
        this.type = type;
        this.text = text;
        this.html = html;
        this.files = files != null ? Collections.unmodifiableList(files)
                : Collections.emptyList();
    }

    /**
     * Gets the event type.
     *
     * @return the event type ("paste", "copy", or "cut")
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the plain text content from the clipboard event.
     *
     * @return the plain text, or {@code null} if no text was available
     */
    public String getText() {
        return text;
    }

    /**
     * Gets the HTML content from the clipboard event.
     *
     * @return the HTML content, or {@code null} if no HTML was available
     */
    public String getHtml() {
        return html;
    }

    /**
     * Checks whether this event contains plain text data.
     *
     * @return {@code true} if text data is available
     */
    public boolean hasText() {
        return text != null && !text.isEmpty();
    }

    /**
     * Checks whether this event contains HTML data.
     *
     * @return {@code true} if HTML data is available
     */
    public boolean hasHtml() {
        return html != null && !html.isEmpty();
    }

    /**
     * Checks whether this event contains file data.
     *
     * @return {@code true} if file data is available
     */
    public boolean hasFiles() {
        return !files.isEmpty();
    }

    /**
     * Gets the list of files from the clipboard event.
     *
     * @return an unmodifiable list of clipboard files, never {@code null}
     */
    public List<ClipboardFile> getFiles() {
        return files;
    }
}
