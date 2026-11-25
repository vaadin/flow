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
package com.vaadin.flow.test.cspstrict.csp;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;

/**
 * A view whose element fetches a {@code data:} URL in the browser, as
 * {@code vaadin-icon} does for an {@code SvgIcon} given as a
 * {@code data:image/svg+xml} URL. The element shows the outcome as its text.
 */
@Route("data-url-fetch")
@JsModule("./csp-data-url-element.js")
public class DataUrlFetchView extends Div {

    public DataUrlFetchView() {
        Element fetcher = new Element("csp-data-url-element");
        fetcher.setAttribute("id", "fetcher");
        getElement().appendChild(fetcher);
    }
}
