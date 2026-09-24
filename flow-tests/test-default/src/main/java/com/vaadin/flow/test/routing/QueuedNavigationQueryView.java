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
package com.vaadin.flow.test.routing;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

@Route("queued-navigation-query")
public class QueuedNavigationQueryView extends Div
        implements BeforeEnterObserver {

    public static final String ANCHOR_ID = "query-link";
    public static final String QUERY_ID = "query";
    public static final String QUERY_VALUE = "value";

    private final Span query = new Span();

    public QueuedNavigationQueryView() {
        Anchor anchor = new Anchor("queued-navigation-query?qp=" + QUERY_VALUE,
                "Navigate with query parameter");
        anchor.setId(ANCHOR_ID);
        query.setId(QUERY_ID);
        add(anchor, query);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Slow navigation so that a second click is queued while the first
        // navigation is still in progress
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        query.setText(event.getLocation().getQueryParameters()
                .getSingleParameter("qp").orElse(""));
    }
}
