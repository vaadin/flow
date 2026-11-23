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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;

/**
 * Where the navigation to {@link LazyChunkView} starts, so that its chunk is
 * loaded by a navigation and not with the page.
 */
@Route("lazy-chunk-start")
public class LazyChunkStartView extends Div {

    public LazyChunkStartView() {
        RouterLink link = new RouterLink("Go to the view with a lazy chunk",
                LazyChunkView.class);
        link.setId("link");
        add(link);
    }
}
