/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.PreserveOnRefreshForwardToView")
@PreserveOnRefresh
public class PreserveOnRefreshForwardToView extends Div
        implements BeforeEnterObserver {

    public PreserveOnRefreshForwardToView() {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getPathWithQueryParameters()
                .contains("initial")) {
            QueryParameters queryParameters = QueryParameters.of("afterforward",
                    "true");
            Location location = new Location(event.getLocation().getPath(),
                    queryParameters);
            event.getUI().getPage().getHistory().replaceState(null, location);
        }
    }
}
