/*
 * Copyright 2000-2024 Vaadin Ltd.
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

import {ReactAdapterElement, RenderHooks} from "Frontend/generated/flow/ReactAdapter.js";
import React from "react";

class ReactLayoutElement extends ReactAdapterElement {
    protected render(hooks: RenderHooks): React.ReactElement | null {
        const content = this.useContent('content');
        const second = this.useContent('second');
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
