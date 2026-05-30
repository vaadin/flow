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

import java.io.Serializable;

/**
 * Fired by the {@code onStart} step of a {@link PasteFileHandler#session()}
 * once per paste, immediately before the paste's first {@link PasteFile} is
 * delivered. Carries the paste correlation token and the total file count the
 * browser declared (so a UI can show "0 / N" before any file has arrived).
 * <p>
 * The handler keeps a high-water mark of observed paste ids; an {@code onStart}
 * fires only for the latest paste seen, so late uploads from an older paste do
 * not regenerate a start event.
 *
 * @param pasteId
 *            the paste sequence number; matches the {@link PasteFile#pasteId()}
 *            of the files about to be delivered
 * @param totalFiles
 *            the number of files the browser said the paste contains
 */
public record PasteStart(long pasteId, int totalFiles) implements Serializable {
}
