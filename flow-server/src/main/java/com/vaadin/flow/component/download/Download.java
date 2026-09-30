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

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.TransferProgressAwareHandler;

/**
 * Entry point for starting file downloads from a click. Bind a download to a
 * button (or any other clickable component) by chaining off
 * {@link #onClick(Component)}:
 *
 * <pre>{@code
 * Button export = new Button("Export");
 * Download.onClick(export).start(DownloadHandler.fromInputStream(event -> {
 *     byte[] report = reportService.createReport();
 *     return new DownloadResponse(new ByteArrayInputStream(report),
 *             "report.pdf", "application/pdf", report.length);
 * }));
 * }</pre>
 *
 * The content is created lazily: the {@link DownloadHandler} runs only when the
 * browser requests the file, so the file name, content type and other response
 * headers can be decided at that point. If the handler throws or returns an
 * error response, the browser reports a failed download instead of saving an
 * empty file. Use the transfer callbacks of the handler (for example
 * {@link TransferProgressAwareHandler#whenComplete(com.vaadin.flow.function.SerializableConsumer)
 * whenComplete}) to react to success or failure on the server. The browser does
 * not report whether the user actually saved the file.
 *
 * @see DownloadBinding
 */
public final class Download implements Serializable {

    private Download() {
        // utility class
    }

    /**
     * Starts a download bound to clicks on the given component — the entry
     * point for "Download" and "Export" buttons. Chain what to download onto
     * the returned {@link DownloadBinding}:
     *
     * <pre>{@code
     * Button download = new Button("Download");
     * Download.onClick(download).start(DownloadHandler.forFile(file));
     * }</pre>
     *
     * The download starts in the browser's own click handler, without waiting
     * for a server round trip. Unlike an {@code Anchor} wrapping a button, the
     * component stays a real button: it keeps its look, keyboard handling and
     * click listeners.
     *
     * @param component
     *            the component whose clicks start the download, not
     *            {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a binding for chaining what to download on click
     * @see DownloadBinding
     */
    public static <T extends Component & ClickNotifier<?>> DownloadBinding onClick(
            T component) {
        Objects.requireNonNull(component, "component must not be null");
        return new DownloadBinding(new ClickTrigger(component));
    }
}
