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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.DownloadAction;
import com.vaadin.flow.component.trigger.internal.OpenInNewTabAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.TransferProgressAwareHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Entry point for delivering content to the browser from a click on a button or
 * any other clickable component, either saved as a file or opened in a new
 * browser tab:
 *
 * <pre>{@code
 * Button export = new Button("Export");
 * Download.onClick(export, DownloadHandler.fromInputStream(event -> {
 *     byte[] report = reportService.createReport();
 *     return new DownloadResponse(new ByteArrayInputStream(report),
 *             "report.pdf", "application/pdf", report.length);
 * }));
 *
 * Button view = new Button("View invoice");
 * Download.onClick(view, invoiceHandler, DownloadTarget.NEW_TAB);
 * }</pre>
 *
 * The download starts, or the tab opens, in the browser's own click handler,
 * without waiting for a server round trip, so popup blockers let a new tab
 * through. Opening a tab from a server-side click listener, for example with
 * {@link com.vaadin.flow.component.page.Page#open(String) Page.open}, happens
 * only after the round trip and may be blocked. Unlike an {@code Anchor}
 * wrapping a button, the component stays a real button: it keeps its look,
 * keyboard handling and click listeners.
 * <p>
 * The content of a {@link DownloadHandler} is created lazily: the handler runs
 * only when the browser requests the content, so the file name, content type
 * and other response headers can be decided at that point. If the handler
 * throws or returns an error response, the browser reports a failed download
 * instead of saving an empty file, or shows the error in the new tab. Use the
 * transfer callbacks of the handler (for example
 * {@link TransferProgressAwareHandler#whenComplete(com.vaadin.flow.function.SerializableConsumer)
 * whenComplete}) to react to success or failure on the server. The browser does
 * not report whether the user actually saved or looked at the content.
 *
 * @see DownloadTarget
 * @since 25.4
 */
public final class Download implements Serializable {

    private Download() {
        // utility class
    }

    /**
     * Saves the content of a handler as a file when the given component is
     * clicked. Same as
     * {@link #onClick(Component, DownloadHandler, DownloadTarget)
     * onClick(component, handler, DownloadTarget.FILE)}. The versions that take
     * a handler are for content produced on the server; the versions that take
     * a URL are for content the browser can already address.
     *
     * @param component
     *            the component whose clicks start the download, not
     *            {@code null}
     * @param handler
     *            produces the file when the browser requests it, not
     *            {@code null}
     * @return a registration for removing the download from the component;
     *         after removal the handler is no longer served
     */
    public static Registration onClick(Component component,
            DownloadHandler handler) {
        return onClick(component, handler, DownloadTarget.FILE);
    }

    /**
     * Delivers the content of a handler to the given target when the given
     * component is clicked. The version without a target saves the content as a
     * file.
     * <p>
     * The handler runs only when the browser requests the content, once per
     * click. The file name and response headers come from the handler, for
     * example from
     * {@link com.vaadin.flow.server.streams.DownloadEvent#setFileName(String)
     * DownloadEvent.setFileName} or the file name of a
     * {@link com.vaadin.flow.server.streams.DownloadResponse DownloadResponse}.
     * <p>
     * With {@link DownloadTarget#NEW_TAB}, the browser shows the content only
     * when it is served inline. The built-in handlers, such as
     * {@link DownloadHandler#forFile(java.io.File)}, are switched to serve the
     * content inline. This changes the given handler instance, so it also
     * serves inline wherever else it is used. A custom handler should call
     * {@link com.vaadin.flow.server.streams.DownloadEvent#inline(String)
     * DownloadEvent.inline} itself, since content served as an attachment is
     * saved instead of shown.
     * <p>
     * The handler is registered for the clicked component and follows its
     * lifecycle: it is only served while the component is attached, visible and
     * enabled. A button that disables itself on click can be disabled before
     * the browser requests the content; pass
     * {@link DownloadHandler#allowDisabled() handler.allowDisabled()} to serve
     * the content in that case. The handler that {@code allowDisabled()}
     * returns can no longer be switched to inline, so for
     * {@link DownloadTarget#NEW_TAB} call
     * {@link AbstractDownloadHandler#inline() inline()} on a built-in handler
     * first, for example
     * {@code DownloadHandler.forFile(file).inline().allowDisabled()}.
     *
     * @param component
     *            the component whose clicks deliver the content, not
     *            {@code null}
     * @param handler
     *            produces the content when the browser requests it, not
     *            {@code null}
     * @param target
     *            where the browser puts the content, not {@code null}
     * @return a registration for removing the behavior from the component;
     *         after removal the handler is no longer served
     */
    public static Registration onClick(Component component,
            DownloadHandler handler, DownloadTarget target) {
        Objects.requireNonNull(handler, "handler must not be null");
        Objects.requireNonNull(target, "target must not be null");
        return bind(component, switch (target) {
        case FILE -> new DownloadAction(handler);
        case NEW_TAB -> {
            if (handler instanceof AbstractDownloadHandler<?> builtIn) {
                // the built-in handlers serve an attachment by default,
                // which the browser would save instead of showing in the tab
                builtIn.inline();
            }
            yield new OpenInNewTabAction(handler);
        }
        });
    }

    /**
     * Saves the content of a URL as a file when the given component is clicked.
     * Same as {@link #onClick(Component, String, DownloadTarget)
     * onClick(component, url, DownloadTarget.FILE)}. The versions that take a
     * URL are for content the browser can already address, such as a static
     * file or a REST endpoint; the versions that take a handler are for content
     * produced on the server.
     *
     * @param component
     *            the component whose clicks start the download, not
     *            {@code null}
     * @param url
     *            the URL to download from, not {@code null}
     * @return a registration for removing the download from the component
     */
    public static Registration onClick(Component component, String url) {
        return onClick(component, url, DownloadTarget.FILE);
    }

    /**
     * Delivers the content of a URL to the given target when the given
     * component is clicked. With {@link DownloadTarget#NEW_TAB} the URL can
     * also be a route of the application or an external page. The version
     * without a target saves the content as a file.
     *
     * @param component
     *            the component whose clicks deliver the content, not
     *            {@code null}
     * @param url
     *            the URL of the content, not {@code null}
     * @param target
     *            where the browser puts the content, not {@code null}
     * @return a registration for removing the behavior from the component
     * @throws IllegalArgumentException
     *             if {@code target} is {@link DownloadTarget#NEW_TAB} and
     *             {@code url} is a {@code javascript:} URL
     */
    public static Registration onClick(Component component, String url,
            DownloadTarget target) {
        Objects.requireNonNull(target, "target must not be null");
        return bind(component, switch (target) {
        case FILE -> new DownloadAction(url);
        case NEW_TAB -> new OpenInNewTabAction(url);
        });
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
     * @return a registration for removing the download from the component
     */
    public static Registration onClick(Component component, String url,
            String fileName) {
        return bind(component, new DownloadAction(url, fileName));
    }

    private static Registration bind(Component component, Action action) {
        Objects.requireNonNull(component, "component must not be null");
        Trigger trigger = new ClickTrigger(component);
        trigger.triggers(action);
        return trigger::remove;
    }
}
