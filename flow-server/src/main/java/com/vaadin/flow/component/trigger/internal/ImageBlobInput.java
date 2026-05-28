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

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Input that produces the source {@code <img>} element of a component, for use
 * as the image slot of {@link WriteToClipboardAction}. The TS helper
 * ({@code window.Vaadin.Flow.clipboard.writePayload}) re-encodes it to
 * {@code image/png} via a canvas round-trip — the only image MIME type every
 * browser's asynchronous Clipboard API accepts on write.
 * <p>
 * The Java type parameter is purely a marker: the value never crosses the
 * network — the action calls the TS helper with the live {@link Element}
 * reference and the canvas conversion happens entirely on the client.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ImageBlobInput extends Action.Input<Object> {

    private final Element source;

    /**
     * Creates an image input that yields the given component's root element as
     * the source {@code <img>}.
     *
     * @param source
     *            the component carrying the {@code <img>} root element, not
     *            {@code null}; its root tag must be {@code img}
     * @throws IllegalArgumentException
     *             if the source's root element is not an {@code <img>}
     */
    public ImageBlobInput(Component source) {
        Element element = Objects.requireNonNull(source).getElement();
        if (!Tag.IMG.equals(element.getTag())) {
            throw new IllegalArgumentException(
                    "source root element must be <img>, was <"
                            + element.getTag() + ">");
        }
        this.source = element;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction.of("return $0", source);
    }
}
