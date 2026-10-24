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
package com.vaadin.flow.component.trigger;

import java.net.URI;
import java.util.IdentityHashMap;
import java.util.Map;

import com.vaadin.flow.component.trigger.internal.ActionInput;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.server.StreamResourceRegistry;
import com.vaadin.flow.server.streams.DownloadHandler;

/**
 * Input that yields the URL of a {@link DownloadHandler}, for actions that load
 * server-generated content such as {@link DownloadAction} and
 * {@link OpenInNewTabAction}.
 * <p>
 * Registers one {@link StreamResourceRegistry.ElementStreamResource} per
 * trigger, scoped to the trigger's host. Each trigger owns its resource, so
 * removing one trigger leaves the others sharing this input working, even on
 * the same host. Lifecycle (register on attach, unregister on detach,
 * re-register on re-attach, unregister when the trigger is removed) is
 * delegated to {@link Element#setAttribute(String, AbstractStreamResource)} —
 * the same machinery {@code Image.setSrc(DownloadHandler)} and
 * {@code Anchor.setHref(DownloadHandler)} use. The attribute name is derived
 * from the resource's UUID id, so it is unique per binding and harmless on any
 * host element. The URI baked into the JS is the same one the framework
 * re-publishes on the attribute, kept stable by the resource's id.
 */
final class DownloadHandlerInput extends ActionInput<String> {

    private static final String ATTR_PREFIX = "data-flow-download-";

    private final DownloadHandler handler;
    private final Map<Trigger, URI> uriByTrigger = new IdentityHashMap<>();

    DownloadHandlerInput(DownloadHandler handler) {
        this.handler = handler;
    }

    @Override
    public JsFunction toJs(Trigger trigger) {
        // Register-and-resolve in one step: the URI is stable per
        // (handler, trigger) pair, so multiple toJs() calls for the same
        // trigger reuse the same resource.
        URI uri = uriByTrigger.get(trigger);
        if (uri == null) {
            uri = register(trigger);
            uriByTrigger.put(trigger, uri);
        }
        return JsFunction.of("return $0", uri.toASCIIString());
    }

    private URI register(Trigger trigger) {
        Element host = trigger.getHost();
        StreamResourceRegistry.ElementStreamResource resource = new StreamResourceRegistry.ElementStreamResource(
                handler, host);
        String attribute = ATTR_PREFIX + resource.getId();
        host.setAttribute(attribute, resource);
        // Removing the attribute also unregisters the resource, so the
        // URL stops serving the file once the trigger is removed.
        trigger.addCleanup(() -> {
            host.removeAttribute(attribute);
            uriByTrigger.remove(trigger);
        });
        return StreamResourceRegistry.getURI(resource);
    }
}
