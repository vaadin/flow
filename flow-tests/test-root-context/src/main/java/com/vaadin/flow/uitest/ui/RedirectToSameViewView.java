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
package com.vaadin.flow.uitest.ui;

import java.util.Map;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Test view for issue #23232: StackOverflowError when forwarding to the same
 * view with different route parameters.
 */
@Route(value = "com.vaadin.flow.uitest.ui.RedirectToSameViewView/:redirectType((forward|reroute))/:id", layout = ViewTestLayout.class)
public class RedirectToSameViewView extends Div implements BeforeEnterObserver {

    enum RedirectType {
        forward, reroute
    }

    static final String VALID_ID = "valid-id";
    static final Set<String> VALID_IDS = Set.of(VALID_ID, "other-valid-id");

    public static final String ID_LABEL = "id-label";
    public static final String ENTER_COUNT_LABEL = "enter-count-label";
    public static final String INSTANCE_ID_LABEL = "instance-id-label";

    private static int instanceCounter = 0;
    private int enterCount = 0;
    private final int instanceId;

    private final Span idLabel;
    private final Span enterCountLabel;
    private final Span instanceIdLabel;

    public static void resetStatic() {
        instanceCounter = 0;
    }

    public RedirectToSameViewView() {
        instanceId = ++instanceCounter;

        idLabel = new Span();
        idLabel.setId(ID_LABEL);

        enterCountLabel = new Span();
        enterCountLabel.setId(ENTER_COUNT_LABEL);

        instanceIdLabel = new Span();
        instanceIdLabel.setId(INSTANCE_ID_LABEL);
        instanceIdLabel.setText(String.valueOf(instanceId));

        add(new Div(new Span("ID: "), idLabel));
        add(new Div(new Span("Enter count: "), enterCountLabel));
        add(new Div(new Span("Instance ID: "), instanceIdLabel));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        enterCount++;
        RedirectType redirectType = RedirectType.valueOf(event
                .getRouteParameters().get("redirectType").orElse("forward"));
        String id = event.getRouteParameters().get("id").orElse("");
        idLabel.setText(id);
        enterCountLabel.setText(String.valueOf(enterCount));

        // If the ID is not valid, forward to the same view with a valid ID
        // This simulates the scenario from issue #23232
        if (!VALID_IDS.contains(id)) {
            Class<? extends Component> targetViewClass = (Class<? extends Component>) event
                    .getNavigationTarget();
            RouteParameters parameters = new RouteParameters(Map
                    .of("redirectType", redirectType.name(), "id", VALID_ID));

            switch (redirectType) {
            case forward -> event.forwardTo(targetViewClass, parameters);
            case reroute -> event.rerouteTo(targetViewClass, parameters);
            }
        }
    }
}
