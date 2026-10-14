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
import com.vaadin.flow.component.trigger.internal.DownloadAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.TransferProgressAwareHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Entry point for starting file downloads from a click on a button or any other
 * clickable component:
 *
 * <pre>{@code
 * Button export = new Button("Export");
 * Download.onClick(export, DownloadHandler.fromInputStream(event -> {
 *     byte[] report = reportService.createReport();
 *     return new DownloadResponse(new ByteArrayInputStream(report),
 *             "report.pdf", "application/pdf", report.length);
 * }));
 * }</pre>
 *
 * The download starts in the browser's own click handler, without waiting for a
 * server round trip. Unlike an {@code Anchor} wrapping a button, the component
 * stays a real button: it keeps its look, keyboard handling and click
 * listeners.
 * <p>
 * The content of a {@link DownloadHandler} is created lazily: the handler runs
 * only when the browser requests the file, so the file name, content type and
 * other response headers can be decided at that point. If the handler throws or
 * returns an error response, the browser reports a failed download instead of
 * saving an empty file. Use the transfer callbacks of the handler (for example
 * {@link TransferProgressAwareHandler#whenComplete(com.vaadin.flow.function.SerializableConsumer)
 * whenComplete}) to react to success or failure on the server. The browser does
 * not report whether the user actually saved the file.
 * 
 * @since 25.4
 */
public final class Download implements Serializable {

    private Download() {
        // utility class
    }

    /**
     * Starts a download when the given component is clicked. The version that
     * takes a handler is for content produced on the server; the versions that
     * take a URL are for content the browser can already address.
     * <p>
     * The handler runs only when the browser requests the file, once per click.
     * The file name and response headers come from the handler, for example
     * from
     * {@link com.vaadin.flow.server.streams.DownloadEvent#setFileName(String)
     * DownloadEvent.setFileName} or the file name of a
     * {@link com.vaadin.flow.server.streams.DownloadResponse DownloadResponse}.
     * <p>
     * The handler is registered for the clicked component and follows its
     * lifecycle: it is only served while the component is attached, visible and
     * enabled. A button that disables itself on click can be disabled before
     * the browser requests the file; pass
     * {@link DownloadHandler#allowDisabled() handler.allowDisabled()} to serve
     * the file in that case.
     *
     * @param component
     *            the component whose clicks start the download, not
     *            {@code null}
     * @param handler
     *            produces the file when the browser requests it, not
     *            {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a registration for removing the download from the component;
     *         after removal the handler is no longer served
     */
    public static <T extends Component & ClickNotifier<?>> Registration onClick(
            T component, DownloadHandler handler) {
        return bind(component, new DownloadAction(
                Objects.requireNonNull(handler, "handler must not be null")));
    }

    /**
     * Starts a download when the given component is clicked. The version that
     * takes a URL is for content the browser can already address, such as a
     * static file or a REST endpoint; the version that takes a handler is for
     * content produced on the server.
     *
     * @param component
     *            the component whose clicks start the download, not
     *            {@code null}
     * @param url
     *            the URL to download from, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a registration for removing the download from the component
     */
    public static <T extends Component & ClickNotifier<?>> Registration onClick(
            T component, String url) {
        return bind(component, new DownloadAction(url));
    }

    /**
     * Like {@link #onClick(Component, String)} but suggests {@code fileName} as
     * the name of the saved file. Browsers use the suggestion only for URLs of
     * the same origin; for other URLs the server has to send the name in a
     * {@code Content-Disposition} header.
     *
     * @param component
     *            the component whose clicks start the download, not
     *            {@code null}
     * @param url
     *            the URL to download from, not {@code null}
     * @param fileName
     *            the suggested file name, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a registration for removing the download from the component
     */
    public static <T extends Component & ClickNotifier<?>> Registration onClick(
            T component, String url, String fileName) {
        return bind(component, new DownloadAction(url, fileName));
    }

    private static Registration bind(Component component,
            DownloadAction action) {
        Objects.requireNonNull(component, "component must not be null");
        Trigger trigger = new ClickTrigger(component);
        trigger.triggers(action);
        return trigger::remove;
    }
}
