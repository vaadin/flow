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
package com.vaadin.flow.component.newtab;

import java.io.Serializable;
import java.util.Objects;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.OpenInNewTabAction;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.server.streams.AbstractDownloadHandler;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.shared.Registration;

/**
 * Entry point for opening content in a new browser tab from a click on a button
 * or any other clickable component:
 *
 * <pre>{@code
 * Button view = new Button("View invoice");
 * OpenInNewTab.onClick(view, DownloadHandler.fromInputStream(event -> {
 *     byte[] pdf = invoiceService.createPdf();
 *     return new DownloadResponse(new ByteArrayInputStream(pdf), "invoice.pdf",
 *             "application/pdf", pdf.length);
 * }));
 * }</pre>
 *
 * The tab opens in the browser's own click handler, without waiting for a
 * server round trip, so popup blockers let it through. Opening a tab from a
 * server-side click listener, for example with
 * {@link com.vaadin.flow.component.page.Page#open(String) Page.open}, happens
 * only after the round trip and may be blocked.
 * <p>
 * The new tab is opened without access to the opening page ({@code noopener})
 * and without a {@code Referer} header ({@code noreferrer}).
 * <p>
 * To save content as a file instead of showing it, use
 * {@link com.vaadin.flow.component.download.Download Download}.
 * 
 * @since 25.4
 */
public final class OpenInNewTab implements Serializable {

    private OpenInNewTab() {
        // utility class
    }

    /**
     * Opens the content of a handler in a new tab when the given component is
     * clicked. The version that takes a handler is for content produced on the
     * server; the version that takes a URL is for content the browser can
     * already address.
     * <p>
     * The handler runs only when the new tab requests the content, once per
     * click. The browser shows the content when it can, for example a PDF, an
     * image or plain text. The built-in handlers, such as
     * {@link DownloadHandler#forFile(java.io.File)}, are switched to serve the
     * content inline. This changes the given handler instance, so it also
     * serves inline wherever else it is used. A custom handler should call
     * {@link com.vaadin.flow.server.streams.DownloadEvent#inline(String)
     * DownloadEvent.inline} itself, since content served as an attachment is
     * downloaded instead of shown.
     * <p>
     * The handler is registered for the clicked component and follows its
     * lifecycle: it is only served while the component is attached, visible and
     * enabled. A button that disables itself on click can be disabled before
     * the new tab requests the content; pass
     * {@link DownloadHandler#allowDisabled() handler.allowDisabled()} to serve
     * the content in that case. The handler that {@code allowDisabled()}
     * returns can no longer be switched to inline, so call
     * {@link AbstractDownloadHandler#inline() inline()} on a built-in handler
     * first, for example
     * {@code DownloadHandler.forFile(file).inline().allowDisabled()}.
     *
     * @param component
     *            the component whose clicks open the new tab, not {@code null}
     * @param handler
     *            produces the content when the new tab requests it, not
     *            {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a registration for removing the behavior from the component;
     *         after removal the handler is no longer served
     */
    public static <T extends Component & ClickNotifier<?>> Registration onClick(
            T component, DownloadHandler handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        if (handler instanceof AbstractDownloadHandler<?> builtIn) {
            // the built-in handlers serve an attachment by default, which
            // the browser would download instead of showing in the tab
            builtIn.inline();
        }
        return bind(component, new OpenInNewTabAction(handler));
    }

    /**
     * Opens a URL in a new tab when the given component is clicked. The version
     * that takes a URL is for content the browser can already address, such as
     * a static file, a route of the application or an external page; the
     * version that takes a handler is for content produced on the server.
     *
     * @param component
     *            the component whose clicks open the new tab, not {@code null}
     * @param url
     *            the URL to open, not {@code null}
     * @param <T>
     *            the component type, must implement {@link ClickNotifier}
     * @return a registration for removing the behavior from the component
     * @throws IllegalArgumentException
     *             if {@code url} is a {@code javascript:} URL
     */
    public static <T extends Component & ClickNotifier<?>> Registration onClick(
            T component, String url) {
        return bind(component, new OpenInNewTabAction(url));
    }

    private static Registration bind(Component component,
            OpenInNewTabAction action) {
        Objects.requireNonNull(component, "component must not be null");
        Trigger trigger = new ClickTrigger(component);
        trigger.triggers(action);
        return trigger::remove;
    }
}
