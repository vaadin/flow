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
package com.vaadin.flow.test.cspstrict.chunk;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * A view with a frontend module of its own. The production build puts the
 * module in a chunk that the browser loads when the view is first navigated to,
 * and the module defines the custom element this view shows.
 */
@Route("lazy-chunk")
@JsModule("./csp-lazy-element.js")
public class LazyChunkView extends Div {

    public LazyChunkView() {
        Element lazy = new Element("csp-lazy-element");
        lazy.setAttribute("id", "lazy");
        getElement().appendChild(lazy);
    }
}
