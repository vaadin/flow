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
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

@Route("queued-navigation-query")
public class QueuedNavigationQueryView extends Div
        implements HasUrlParameter<String> {

    public static final String FIRST_ANCHOR_ID = "first-link";
    public static final String SECOND_ANCHOR_ID = "second-link";
    public static final String QUERY_LOG_ID = "query-log";

    private final Span queryLog = new Span();

    public QueuedNavigationQueryView() {
        Anchor first = new Anchor("queued-navigation-query/first?qp=first",
                "First");
        first.setId(FIRST_ANCHOR_ID);
        Anchor second = new Anchor(
                "queued-navigation-query/second?qp=second#fragment", "Second");
        second.setId(SECOND_ANCHOR_ID);
        queryLog.setId(QUERY_LOG_ID);
        add(first, second, queryLog);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        if (parameter.isEmpty()) {
            return;
        }
        // Slow navigation so that a navigation started meanwhile gets queued
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String query = event.getLocation().getQueryParameters()
                .getSingleParameter("qp").orElse("");
        queryLog.setText(queryLog.getText().isEmpty() ? query
                : queryLog.getText() + "," + query);
    }
}
