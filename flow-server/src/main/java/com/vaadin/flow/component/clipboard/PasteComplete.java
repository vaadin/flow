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
 * Fired by the {@code onComplete} step of a {@link PasteFileHandler#session()}
 * once all expected files of a paste have been delivered to the {@code onFile}
 * step.
 * <p>
 * "Expected" comes from the browser's {@link Clipboard#PASTE_FILE_COUNT_HEADER}
 * value; uploads that fail in transit (network errors, rejected uploads) never
 * arrive at the server and therefore do not count, so a paste with a lost
 * upload never fires {@code onComplete}. Sessions for different pastes are
 * independent: a paste continues delivering files (and eventually fires
 * {@code onComplete}) even when a newer paste's session has already started
 * &mdash; the two complete on their own timelines.
 *
 * @param pasteId
 *            the paste sequence number that just finished delivering
 * @param receivedFiles
 *            the number of files actually delivered for this paste; equals the
 *            {@link PasteStart#totalFiles()} value the paste started with
 */
public record PasteComplete(long pasteId,
        int receivedFiles) implements Serializable {
}
