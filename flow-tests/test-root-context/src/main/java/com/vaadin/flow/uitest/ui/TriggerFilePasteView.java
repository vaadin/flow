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
package com.vaadin.flow.uitest.ui;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.PasteFile;
import com.vaadin.flow.component.clipboard.PasteFileHandler;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.router.Route;

/**
 * Registers a {@link Clipboard#onFilePaste} listener using a
 * {@link PasteFileHandler#batch() batch} so the IT can verify all three phases
 * &mdash; {@code onStart} per paste, {@code onFile} per file (rendered as text
 * in a {@link Div}, parsed HTML through {@link Html}, or {@code image/*} via
 * {@link Image} with a data URL), and {@code onComplete} when the paste's
 * declared files have all arrived.
 * <p>
 * Files accumulate across pastes &mdash; each paste runs its own independent
 * batch, so two pastes in sequence (or even overlapping) both finish on their
 * own timelines. {@code #file-count} tracks total files delivered;
 * {@code #completed-count} tracks how many batches have hit {@code onComplete}.
 * <p>
 * Note that there is no {@code @Push} on this view: each upload runs on a
 * separate HTTP request, but {@code onFilePaste} dispatches a round trip once a
 * paste's uploads have finished, which flushes the queued UI updates from the
 * batch callbacks to the client.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerFilePasteView")
public class TriggerFilePasteView extends Div {

    public TriggerFilePasteView() {
        Div target = new Div();
        target.setId("target");
        // paste only fires on focused elements; tabindex makes the div
        // focusable, matching the regular onPaste view.
        target.getElement().setAttribute("tabindex", "0");
        target.setText("Focus me and paste a file");

        // Renders one slot per delivered file. The view does not clear on a
        // new paste — every paste's files stay visible, which matches the
        // framework's "no cancellation" promise.
        Div status = new Div();
        status.setId("file-status");
        // onFile counter: total files delivered across all pastes.
        Div count = new Div("0");
        count.setId("file-count");
        // onComplete counter: number of paste sessions that fully finished.
        Div completed = new Div("0");
        completed.setId("completed-count");

        add(target, status, count, completed);

        AtomicInteger receivedFiles = new AtomicInteger();
        AtomicInteger completedSessions = new AtomicInteger();

        Clipboard.onFilePaste(target,
                PasteFileHandler.batch().onStart(start -> {
                    // Visible only via the test app — included so a developer
                    // running the view by hand sees the session boundary.
                    Div banner = new Div();
                    banner.addClassName("paste-banner");
                    banner.setText("Paste " + start.pasteId() + ": "
                            + start.totalFiles() + " file(s) incoming");
                    status.add(banner);
                }).onFile(file -> {
                    status.add(renderSlot(file));
                    count.setText(
                            Integer.toString(receivedFiles.incrementAndGet()));
                }).onComplete(complete -> {
                    completed.setText(Integer
                            .toString(completedSessions.incrementAndGet()));
                }).build());
    }

    private static Div renderSlot(PasteFile file) {
        Div slot = new Div();
        slot.addClassName("file-slot");
        String contentType = file.contentType();
        if (contentType != null && contentType.startsWith("image/")) {
            // Inline the bytes as a data URL so the IT can read the image
            // off the src attribute without a follow-up request.
            String dataUrl = "data:" + contentType + ";base64,"
                    + Base64.getEncoder().encodeToString(file.bytes());
            Image image = new Image(dataUrl, file.fileName());
            image.addClassName("file-slot-image");
            slot.add(image);
        } else if (contentType != null && contentType.startsWith("text/html")) {
            // Html requires a single root element; the IT's synthetic
            // payload wraps the snippet in one.
            slot.add(
                    new Html(new String(file.bytes(), StandardCharsets.UTF_8)));
        } else {
            // Treat anything else as plain text. The IT's text/plain case
            // lands here.
            slot.setText(new String(file.bytes(), StandardCharsets.UTF_8));
        }
        return slot;
    }
}
