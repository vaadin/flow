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

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.WildcardParameter;

@Route("com.vaadin.flow.ServerView")
public class ServerView extends Div implements HasUrlParameter<String> {

    private final Span setParameterSpan = new Span();
    private int setParameterCount = 0;
    private final Span queryLogSpan = new Span();

    public ServerView() {
        NativeButton serverNavigation = new NativeButton(
                "Navigate through Server", event -> {
                    event.getSource().getUI().get()
                            .navigate(NavigationView.class);
                });
        serverNavigation.setId(NavigationView.SERVER_ID);
        setParameterSpan.setId(NavigationView.SET_PARAMETER_COUNTER_ID);
        queryLogSpan.setId(NavigationView.QUERY_LOG_ID);

        add(new Span("ServerView"), new Div(), serverNavigation, new Div(),
                setParameterSpan, new Div(), queryLogSpan);
    }

    @Override
    public void setParameter(BeforeEvent event,
            @WildcardParameter String parameter) {
        setParameterSpan.setText("" + ++setParameterCount);
        if (!parameter.isEmpty()) {
            logQuery(event);
        }
    }

    private void logQuery(BeforeEvent event) {
        // Slow navigation so that a navigation started meanwhile gets queued
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String query = event.getLocation().getQueryParameters()
                .getSingleParameter("qp").orElse("");
        queryLogSpan.setText(queryLogSpan.getText().isEmpty() ? query
                : queryLogSpan.getText() + "," + query);
    }
}
