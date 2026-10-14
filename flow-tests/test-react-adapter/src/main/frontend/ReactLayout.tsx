/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
import {ReactAdapterElement, RenderHooks} from "Frontend/generated/flow/ReactAdapter.js";
import React from "react";

class ReactLayoutElement extends ReactAdapterElement {
    protected render(hooks: RenderHooks): React.ReactElement | null {
        const content = hooks.useContent('content');
        const second = hooks.useContent('second');
        return <>
            <span>Before Flow components content</span>
            {content}
            <div></div>
            <span>After Flow components content</span>
            <div></div>
            {second}
        </>;
    }
}

customElements.define('react-layout', ReactLayoutElement);
