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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;
import java.net.URI;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Starts a file download in the browser when the bound trigger fires. The URL
 * is loaded into a synthesised {@code <a href download>} that is clicked
 * synchronously inside the trigger's handler — so the call happens inside the
 * user gesture and is not subject to popup blockers.
 * <p>
 * Three ways to specify what to download:
 * <ul>
 * <li><b>A URL the application already knows</b> — a server endpoint, an
 * external CDN, a {@code blob:} URL minted on the client, anything addressable.
 * Use {@link #DownloadAction(String)} or
 * {@link #DownloadAction(String, String)} to also suggest a filename.</li>
 * <li><b>A {@link DownloadHandler} for server-generated content</b> — see
 * {@link DownloadHandler#forFile}, {@link DownloadHandler#forClassResource},
 * {@link DownloadHandler#fromInputStream}, or any lambda. Use
 * {@link #DownloadAction(DownloadHandler)}. The action lazily registers a
 * stream resource scoped to the trigger host element; the resource is
 * unregistered when the host detaches and re-registered on re-attach, keeping
 * the URL stable for the lifetime of the action. Filename comes from the
 * {@link DownloadHandler} itself (its URL postfix and/or
 * {@code Content-Disposition} header).</li>
 * <li><b>A value resolved from client state at fire time</b> — use
 * {@link #DownloadAction(Action.Input)} or
 * {@link #DownloadAction(Action.Input, Action.Input)} when the URL (or
 * filename) is only known on the client, e.g. the current value of an input
 * field, or a {@code blob:} URL the client just produced.</li>
 * </ul>
 * <p>
 * The {@code download} attribute that suggests a filename is honoured only for
 * same-origin URLs. For cross-origin downloads the server must send the
 * filename in a {@code Content-Disposition} header itself.
 * <p>
 * No outcome callback: the browser does not notify when (or whether) the file
 * is actually saved.
 *
 * <pre>{@code
 * // Static URL
 * new ClickTrigger(button).triggers(
 *         new DownloadAction("/api/reports/2026-Q1.pdf", "report.pdf"));
 *
 * // Server-generated content
 * new ClickTrigger(button)
 *         .triggers(new DownloadAction(DownloadHandler.forFile(reportFile)));
 *
 * // URL taken from a text field at fire time
 * Action.Input<String> currentUrl = new PropertyInput<>(urlField, "value",
 *         String.class);
 * new ClickTrigger(button).triggers(new DownloadAction(currentUrl));
 * }</pre>
 *
 * For internal use only. May be renamed or removed in a future release.
 */
public class DownloadAction extends Action {

    private final Action.Input<String> urlInput;
    private final Action.@Nullable Input<String> filenameInput;

    /**
     * Downloads from a known URL with no filename hint.
     *
     * @param url
     *            the URL to download from, not {@code null}
     */
    public DownloadAction(String url) {
        this.urlInput = literal(url, "url");
        this.filenameInput = null;
    }

    /**
     * Downloads from a known URL and suggests {@code filename} as the saved
     * name. The hint is ignored by the browser for cross-origin URLs; provide a
     * {@code Content-Disposition} response header in that case.
     *
     * @param url
     *            the URL to download from, not {@code null}
     * @param filename
     *            the suggested filename, not {@code null}
     */
    public DownloadAction(String url, String filename) {
        this.urlInput = literal(url, "url");
        this.filenameInput = literal(filename, "filename");
    }

    /**
     * Downloads from a {@link DownloadHandler}. The handler is registered as a
     * stream resource scoped to the trigger host element the first time this
     * action is wired to a trigger; re-attaching the host re-registers the
     * resource with the same URL.
     *
     * @param handler
     *            produces the response when the URL is fetched, not
     *            {@code null}
     */
    public DownloadAction(DownloadHandler handler) {
        this.urlInput = new HandlerBinding(
                Objects.requireNonNull(handler, "handler must not be null"));
        this.filenameInput = null;
    }

    /**
     * Downloads from a URL resolved on the client at fire time. Use this when
     * the URL is only known in the browser — for example, the current contents
     * of an input field, or a {@code blob:} URL the client just minted.
     *
     * @param url
     *            input supplying the URL when the trigger fires, not
     *            {@code null}
     */
    public DownloadAction(Action.Input<String> url) {
        this.urlInput = Objects.requireNonNull(url, "url must not be null");
        this.filenameInput = null;
    }

    /**
     * Like {@link #DownloadAction(Action.Input)} but also resolves the
     * suggested filename on the client.
     *
     * @param url
     *            input supplying the URL when the trigger fires, not
     *            {@code null}
     * @param filename
     *            input supplying the suggested filename when the trigger fires,
     *            not {@code null}
     */
    public DownloadAction(Action.Input<String> url,
            Action.Input<String> filename) {
        this.urlInput = Objects.requireNonNull(url, "url must not be null");
        this.filenameInput = Objects.requireNonNull(filename,
                "filename must not be null");
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // Two body shapes — with or without a filename argument — keep the
        // emitted JS minimal. Both URL and filename are JsFunctions invoked
        // with event so any handler-scoped inputs read live state.
        if (filenameInput == null) {
            return JsFunction.of("window.Vaadin.Flow.download.start($0(event))",
                    urlInput.toJs(trigger)).withArguments("event");
        }
        return JsFunction
                .of("window.Vaadin.Flow.download.start($0(event), $1(event))",
                        urlInput.toJs(trigger), filenameInput.toJs(trigger))
                .withArguments("event");
    }

    private static LiteralInput<String> literal(String value, String name) {
        return new LiteralInput<>(
                Objects.requireNonNull(value, name + " must not be null"));
    }

    /**
     * Manages the {@link DownloadHandler}-backed flavour: one
     * {@link StreamResourceRegistry.ElementStreamResource} per trigger host.
     * Lifecycle (register on attach, unregister on detach, re-register on
     * re-attach) is delegated to
     * {@link Element#setAttribute(String, AbstractStreamResource)} — the same
     * machinery {@code Image.setSrc(DownloadHandler)} and
     * {@code Anchor.setHref(DownloadHandler)} use. The attribute name is
     * derived from the resource's UUID id, so it is unique per binding and
     * harmless on any host element. The URI baked into the JS is the same one
     * the framework re-publishes on the attribute, kept stable by the
     * resource's id.
     */
    private static final class HandlerBinding extends Action.Input<String>
            implements Serializable {

        private static final String ATTR_PREFIX = "data-flow-download-";

        private final DownloadHandler handler;
        private final Map<Element, URI> uriByHost = new IdentityHashMap<>();

        HandlerBinding(DownloadHandler handler) {
            this.handler = handler;
        }

        @Override
        public JsFunction toJs(Trigger trigger) {
            // Register-and-resolve in one step: the URI is stable per
            // (handler, host) pair, so multiple toJs() calls for the same
            // host reuse the same resource.
            URI uri = uriByHost.computeIfAbsent(trigger.getHost(),
                    this::registerForHost);
            return JsFunction.of("return $0", uri.toASCIIString());
        }

        private URI registerForHost(Element host) {
            StreamResourceRegistry.ElementStreamResource resource = new StreamResourceRegistry.ElementStreamResource(
                    handler, host);
            host.setAttribute(ATTR_PREFIX + resource.getId(), resource);
            return StreamResourceRegistry.getURI(resource);
        }
    }
}
